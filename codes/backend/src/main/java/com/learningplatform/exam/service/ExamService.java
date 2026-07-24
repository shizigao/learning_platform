package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.domain.Exam;
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
public class ExamService {
    private final ExamMapper examMapper;
    private final ExamCandidateMapper candidateMapper;
    private final ExamPaperService paperService;
    private final ExamPublishQuotaService quotaService;
    private final UserService userService;

    public ExamService(
            ExamMapper examMapper,
            ExamCandidateMapper candidateMapper,
            ExamPaperService paperService,
            ExamPublishQuotaService quotaService,
            UserService userService
    ) {
        this.examMapper = examMapper;
        this.candidateMapper = candidateMapper;
        this.paperService = paperService;
        this.quotaService = quotaService;
        this.userService = userService;
    }

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
    public ExamManagementResponse create(
            Long publisherId,
            boolean publisherAdmin,
            ExamWriteRequest request
    ) {
        ExamPaper paper = paperService.getReadyOwned(request.paperId(), publisherId, publisherAdmin);
        validateSchedule(request, paper);
        List<Long> candidateIds = validateCandidates(request.candidateUserIds());

        Exam exam = new Exam();
        exam.setPublisherId(publisherId);
        apply(exam, request);
        if (examMapper.insert(exam) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建考试失败");
        }
        replaceCandidates(exam.getId(), candidateIds);
        return detail(exam.getId(), publisherId, publisherAdmin);
    }

    @Transactional
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
        List<Long> candidateIds = validateCandidates(request.candidateUserIds());

        apply(exam, request);
        if (examMapper.updateDraft(exam) != 1) {
            throw invalidState("只有草稿考试可以修改");
        }
        replaceCandidates(examId, candidateIds);
        return detail(examId, requesterId, requesterAdmin);
    }

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
                candidates
        );
    }

    @Transactional
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
    public void delete(Long examId, Long requesterId, boolean requesterAdmin) {
        Exam exam = getRequired(examId);
        assertOwnerOrAdmin(exam, requesterId, requesterAdmin);
        assertDraft(exam);
        if (examMapper.softDelete(examId, exam.getPublisherId()) != 1) {
            throw invalidState("只有草稿考试可以删除");
        }
    }

    public List<ExamSummaryResponse> listAssigned(Long userId) {
        return examMapper.findAssignedToCandidate(userId).stream()
                .map(ExamSummaryResponse::from)
                .toList();
    }

    public CandidateExamResponse candidateDetail(Long examId, Long userId) {
        Exam exam = getRequired(examId);
        if (!candidateMapper.exists(examId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是本场考试的指定考生");
        }
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

    public int availableQuota(Long publisherId) {
        return quotaService.availableQuota(publisherId);
    }

    public Exam getRequired(Long examId) {
        return examMapper.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考试不存在"));
    }

    private void apply(Exam exam, ExamWriteRequest request) {
        exam.setPaperId(request.paperId());
        exam.setName(request.name().trim());
        exam.setInstructions(normalize(request.instructions()));
        exam.setStartAt(request.startAt());
        exam.setEndAt(request.endAt());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setPassingScore(request.passingScore());
        exam.setShowResultImmediately(Boolean.TRUE.equals(request.showResultImmediately()));
        exam.setShowAnswerAfterFinish(
                request.showAnswerAfterFinish() == null || request.showAnswerAfterFinish()
        );
    }

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

    private void validatePublishTime(Exam exam) {
        if (!exam.getEndAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试已超过结束时间，不能发布");
        }
    }

    private void assertOwnerOrAdmin(Exam exam, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !exam.getPublisherId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的考试");
        }
    }

    private void assertDraft(Exam exam) {
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw invalidState("只有草稿考试可以执行此操作");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
