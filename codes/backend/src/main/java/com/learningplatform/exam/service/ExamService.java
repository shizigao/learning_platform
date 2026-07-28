/* 文件职责：实现考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.classroom.mapper.ClassScopeMapper;
import com.learningplatform.classroom.service.ClassroomService;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamAssignmentMode;
import com.learningplatform.exam.domain.ExamCandidate;
import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamStatus;
import com.learningplatform.exam.dto.CandidateExamResponse;
import com.learningplatform.exam.dto.ExamCandidateResponse;
import com.learningplatform.exam.dto.ExamListQuery;
import com.learningplatform.exam.dto.ExamManagementResponse;
import com.learningplatform.exam.dto.ExamPaperSummaryResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.dto.ExamWriteRequest;
import com.learningplatform.exam.mapper.ExamCandidateMapper;
import com.learningplatform.exam.mapper.ExamMapper;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
/**
 * 实现考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamService {
    /** 访问考试持久化数据。 */
    private final ExamMapper examMapper;
    /** 访问考生持久化数据。 */
    private final ExamCandidateMapper candidateMapper;
    /** 委托试卷执行对应领域规则。 */
    private final ExamPaperService paperService;
    /** 委托额度执行对应领域规则。 */
    private final ExamPublishQuotaService quotaService;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 访问班级范围持久化数据。 */
    private final ClassScopeMapper classScopeMapper;
    /** 委托班级执行对应领域规则。 */
    private final ClassroomService classroomService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamService(
            ExamMapper examMapper,
            ExamCandidateMapper candidateMapper,
            ExamPaperService paperService,
            ExamPublishQuotaService quotaService,
            UserService userService,
            ClassScopeMapper classScopeMapper,
            ClassroomService classroomService
    ) {
        this.examMapper = examMapper;
        this.candidateMapper = candidateMapper;
        this.paperService = paperService;
        this.quotaService = quotaService;
        this.userService = userService;
        this.classScopeMapper = classScopeMapper;
        this.classroomService = classroomService;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<ExamSummaryResponse> list(Long publisherId, ExamListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = examMapper.countByPublisher(publisherId, query.getStatus(), keyword);
        List<ExamSummaryResponse> items = examMapper.findByPublisher(
                        publisherId,
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ExamSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public ExamManagementResponse create(
            Long publisherId,
            boolean publisherAdmin,
            ExamWriteRequest request
    ) {
        ExamPaper paper = paperService.getReadyOwned(request.paperId(), publisherId, publisherAdmin);
        validateSchedule(request, paper);
        ExamAssignmentMode assignmentMode = assignmentMode(request);
        validateAssignment(request, publisherId, assignmentMode);
        List<Long> candidateIds = assignmentMode == ExamAssignmentMode.INDIVIDUAL
                ? validateCandidates(request.candidateUserIds())
                : List.of();

        Exam exam = new Exam();
        exam.setPublisherId(publisherId);
        apply(exam, request);
        if (examMapper.insert(exam) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建考试失败");
        }
        replaceClassScopes(exam.getId(), request.classIds(), assignmentMode);
        replaceCandidates(exam.getId(), candidateIds);
        return detail(exam.getId(), publisherId, publisherAdmin);
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public ExamManagementResponse update(
            Long examId,
            Long requesterId,
            boolean requesterAdmin,
            ExamWriteRequest request
    ) {
        Exam exam = getRequired(examId);
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        assertDraft(exam);
        ExamPaper paper = paperService.getReadyOwned(request.paperId(), requesterId, requesterAdmin);
        validateSchedule(request, paper);
        ExamAssignmentMode assignmentMode = assignmentMode(request);
        validateAssignment(request, requesterId, assignmentMode);
        List<Long> candidateIds = assignmentMode == ExamAssignmentMode.INDIVIDUAL
                ? validateCandidates(request.candidateUserIds())
                : List.of();

        apply(exam, request);
        if (examMapper.updateDraft(exam) != 1) {
            throw invalidState("只有草稿考试可以修改");
        }
        replaceClassScopes(examId, request.classIds(), assignmentMode);
        replaceCandidates(examId, candidateIds);
        return detail(examId, requesterId, requesterAdmin);
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public ExamManagementResponse detail(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        Exam exam = getRequired(examId);
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        ExamPaper paper = paperService.getRequired(exam.getPaperId());
        List<ExamCandidateResponse> candidates = candidateMapper.findByExamId(examId).stream()
                .map(ExamCandidateResponse::from)
                .toList();
        return new ExamManagementResponse(
                ExamSummaryResponse.from(exam),
                exam.getInstructions(),
                ExamPaperSummaryResponse.from(paper),
                candidates,
                classScopeMapper.findExamClassIds(examId)
        );
    }

    @Transactional
    /** 执行发布状态流转，仅允许从合法前置状态进入目标状态。 */
    public ExamManagementResponse publish(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        Exam exam = examMapper.findByIdForUpdate(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在"));
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            return detail(examId, requesterId, requesterAdmin);
        }
        assertDraft(exam);
        ExamPaper paper = paperService.getReadyOwned(
                exam.getPaperId(),
                exam.getPublisherId(),
                requesterAdmin
        );
        validatePublishTime(exam);
        if (exam.getAssignmentMode() == ExamAssignmentMode.CLASS) {
            syncClassCandidates(examId);
        }
        if (candidateMapper.findByExamId(examId).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试至少需要指定一名考生");
        }

        quotaService.consume(exam.getPublisherId(), examId);
        if (examMapper.publish(examId, LocalDateTime.now()) != 1) {
            throw invalidState("考试状态已发生变化，请重试");
        }
        return detail(examId, requesterId, requesterAdmin);
    }

    @Transactional
    /** 判断是否满足cel条件，不修改持久化状态。 */
    public ExamManagementResponse cancel(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        Exam exam = getRequired(examId);
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        if (exam.getStatus() == ExamStatus.CANCELLED) {
            return detail(examId, requesterId, requesterAdmin);
        }
        if (exam.getStatus() != ExamStatus.PUBLISHED
                || !LocalDateTime.now().isBefore(exam.getStartAt())
                || examMapper.cancel(examId) != 1) {
            throw invalidState("只有已发布且尚未开始作答的考试可以取消");
        }
        return detail(examId, requesterId, requesterAdmin);
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long examId, Long requesterId, boolean requesterAdmin) {
        Exam exam = getRequired(examId);
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        assertDraft(exam);
        if (examMapper.softDelete(examId, exam.getPublisherId()) != 1) {
            throw invalidState("只有草稿考试可以删除");
        }
    }

    /** 查询Assigned相关数据；只返回当前调用方有权查看的结果。 */
    public List<ExamSummaryResponse> listAssigned(Long userId) {
        return examMapper.findAssignedToCandidate(userId).stream()
                .map(ExamSummaryResponse::from)
                .toList();
    }

    /** 查询For班级相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<ExamSummaryResponse> listForClass(
            Long classId,
            Long requesterId,
            PageQuery query
    ) {
        classroomService.requireActiveMember(classId, requesterId);
        long total = classScopeMapper.countClassExams(classId);
        List<ExamSummaryResponse> items = classScopeMapper.findClassExams(
                        classId,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ExamSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 判断是否满足didateDetail条件，不修改持久化状态。 */
    public CandidateExamResponse candidateDetail(Long examId, Long userId) {
        Exam exam = getRequired(examId);
        ensureCandidateAccess(exam, userId);
        if (exam.getStatus() != ExamStatus.PUBLISHED
                && exam.getStatus() != ExamStatus.ONGOING
                && exam.getStatus() != ExamStatus.FINISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试尚未发布或已取消");
        }
        if (LocalDateTime.now().isBefore(exam.getStartAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开始");
        }
        ExamPaper paper = paperService.getRequired(exam.getPaperId());
        return new CandidateExamResponse(
                ExamSummaryResponse.from(exam),
                exam.getInstructions(),
                ExamPaperSummaryResponse.from(paper),
                paperService.candidateQuestions(paper.getId())
        );
    }

    /** 执行 availableQuota 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public int availableQuota(Long publisherId) {
        return quotaService.availableQuota(publisherId);
    }

    /** 返回Required。 */
    public Exam getRequired(Long examId) {
        return examMapper.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在"));
    }

    /** 执行 apply 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void apply(Exam exam, ExamWriteRequest request) {
        exam.setPaperId(request.paperId());
        exam.setName(request.name().trim());
        exam.setInstructions(normalize(request.instructions()));
        exam.setAssignmentMode(assignmentMode(request));
        exam.setStartAt(request.startAt());
        exam.setEndAt(request.endAt());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setPassingScore(request.passingScore());
        exam.setShowResultImmediately(Boolean.TRUE.equals(request.showResultImmediately()));
        exam.setShowAnswerAfterFinish(
                request.showAnswerAfterFinish() == null || request.showAnswerAfterFinish()
        );
    }

    /** 校验Schedule及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateSchedule(ExamWriteRequest request, ExamPaper paper) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试结束时间必须晚于开始时间");
        }
        long windowMinutes = Duration.between(request.startAt(), request.endAt()).toMinutes();
        if (request.durationMinutes() > windowMinutes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "答题时长不能超过考试开放时长");
        }
        if (!request.endAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试结束时间必须晚于当前时间");
        }
        if (request.passingScore().compareTo(paper.getTotalScore()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "及格分不能超过试卷总分");
        }
    }

    /** 校验Candidates及相关业务前置条件，不满足时抛出明确业务异常。 */
    private List<Long> validateCandidates(List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new HashSet<>();
        for (Long userId : requestedIds) {
            if (!unique.add(userId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "指定考生不能重复");
            }
            User user = userService.getRequiredById(userId);
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "只能指定状态正常的考生");
            }
        }
        return List.copyOf(requestedIds);
    }

    /** 更新Candidates，通过返回值或版本条件识别并发状态变化。 */
    private void replaceCandidates(Long examId, List<Long> candidateIds) {
        candidateMapper.deleteByExamId(examId);
        for (Long userId : candidateIds) {
            ExamCandidate candidate = new ExamCandidate();
            candidate.setExamId(examId);
            candidate.setUserId(userId);
            if (candidateMapper.insert(candidate) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存指定考生失败");
            }
        }
    }

    @Transactional
    /** 校验考生访问权及相关业务前置条件，不满足时抛出明确业务异常。 */
    public ExamCandidate ensureCandidateAccess(Exam exam, Long userId) {
        if (exam.getAssignmentMode() == ExamAssignmentMode.CLASS) {
            if (!classScopeMapper.hasExamAccess(exam.getId(), userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅考试指定班级的有效成员可以进入");
            }
            if (!candidateMapper.exists(exam.getId(), userId)) {
                insertCandidate(exam.getId(), userId);
            }
        } else if (!candidateMapper.exists(exam.getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是本场考试的指定考生");
        }
        return candidateMapper.findOne(exam.getId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是本场考试的指定考生"));
    }

    /** 校验Assignment及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateAssignment(
            ExamWriteRequest request,
            Long publisherId,
            ExamAssignmentMode mode
    ) {
        if (mode == ExamAssignmentMode.CLASS) {
            classroomService.requireManageableClasses(request.classIds(), publisherId);
        }
    }

    /** 更新班级Scopes，通过返回值或版本条件识别并发状态变化。 */
    private void replaceClassScopes(
            Long examId,
            List<Long> classIds,
            ExamAssignmentMode mode
    ) {
        classScopeMapper.deleteExamScopes(examId);
        if (mode != ExamAssignmentMode.CLASS) return;
        classIds.forEach(classId -> {
            if (classScopeMapper.insertExamScope(examId, classId) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存考试班级范围失败");
            }
        });
    }

    /** 执行 syncClassCandidates 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void syncClassCandidates(Long examId) {
        List<Long> userIds = classScopeMapper.findActiveMemberIdsForExam(examId);
        if (userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指定班级中暂无有效成员");
        }
        userIds.forEach(userId -> {
            if (!candidateMapper.exists(examId, userId)) insertCandidate(examId, userId);
        });
    }

    /** 创建或初始化考生，并维护唯一性、初始状态和必要关联。 */
    private void insertCandidate(Long examId, Long userId) {
        ExamCandidate candidate = new ExamCandidate();
        candidate.setExamId(examId);
        candidate.setUserId(userId);
        if (candidateMapper.insert(candidate) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存考试考生失败");
        }
    }

    /** 执行 assignmentMode 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ExamAssignmentMode assignmentMode(ExamWriteRequest request) {
        return request.assignmentMode() == null
                ? ExamAssignmentMode.INDIVIDUAL
                : request.assignmentMode();
    }

    /** 校验发布Time及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validatePublishTime(Exam exam) {
        if (!exam.getEndAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试已超过结束时间，不能发布");
        }
    }

    /** 校验OwnerOr管理及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertOwnerOrAdmin(Exam exam, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !exam.getPublisherId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的考试");
        }
    }

    /** 校验Draft及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertDraft(Exam exam) {
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw invalidState("只有草稿考试可以执行此操作");
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 invalidState 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
