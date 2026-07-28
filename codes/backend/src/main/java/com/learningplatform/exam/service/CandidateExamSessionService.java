/* 文件职责：实现考生考试Session业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamCandidate;
import com.learningplatform.exam.domain.ExamCandidateStatus;
import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamStatus;
import com.learningplatform.exam.dto.CandidateExamOverviewResponse;
import com.learningplatform.exam.dto.ExamEligibilityResponse;
import com.learningplatform.exam.dto.ExamPaperSummaryResponse;
import com.learningplatform.exam.dto.ExamStartResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.mapper.ExamAttemptMapper;
import com.learningplatform.exam.mapper.ExamCandidateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 考生进入、开始和恢复考试的会话边界。
 *
 * <p>服务同时校验考试发布状态、参与范围、考试时间窗、考生状态和个人截止时间。
 * 首次开始会锁定考生记录并创建唯一作答，重复开始则安全恢复已有会话。</p>
 */
@Service
public class CandidateExamSessionService {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 委托试卷执行对应领域规则。 */
    private final ExamPaperService paperService;
    /** 访问考生持久化数据。 */
    private final ExamCandidateMapper candidateMapper;
    /** 访问作答持久化数据。 */
    private final ExamAttemptMapper attemptMapper;
    /** 委托答案执行对应领域规则。 */
    private final ExamAnswerService answerService;
    /** 委托运行态State执行对应领域规则。 */
    private final ExamRuntimeStateService runtimeStateService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public CandidateExamSessionService(
            ExamService examService,
            ExamPaperService paperService,
            ExamCandidateMapper candidateMapper,
            ExamAttemptMapper attemptMapper,
            ExamAnswerService answerService,
            ExamRuntimeStateService runtimeStateService
    ) {
        this.examService = examService;
        this.paperService = paperService;
        this.candidateMapper = candidateMapper;
        this.attemptMapper = attemptMapper;
        this.answerService = answerService;
        this.runtimeStateService = runtimeStateService;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public CandidateExamOverviewResponse overview(Long examId, Long userId) {
        LocalDateTime now = now();
        Exam exam = examService.getRequired(examId);
        examService.ensureCandidateAccess(exam, userId);
        ExamCandidate candidate = getCandidate(examId, userId);
        ExamPaper paper = paperService.getRequired(exam.getPaperId());
        ExamAttempt attempt = attemptMapper.findFirst(examId, userId).orElse(null);
        return new CandidateExamOverviewResponse(
                ExamSummaryResponse.from(exam),
                exam.getInstructions(),
                ExamPaperSummaryResponse.from(paper),
                eligibility(exam, candidate, attempt, now)
        );
    }

    /** 仅计算进入资格，供列表或入口页轻量刷新。 */
    public ExamEligibilityResponse eligibility(Long examId, Long userId) {
        LocalDateTime now = now();
        Exam exam = examService.getRequired(examId);
        examService.ensureCandidateAccess(exam, userId);
        ExamCandidate candidate = getCandidate(examId, userId);
        ExamAttempt attempt = attemptMapper.findFirst(examId, userId).orElse(null);
        return eligibility(exam, candidate, attempt, now);
    }

    /**
     * 首次开始或恢复考试。
     * 个人截止时间取“开始时间 + 时长”和考试统一结束时间中的较早值。
     */
    @Transactional
    public ExamStartResponse start(Long examId, Long userId) {
        Exam exam = examService.getRequired(examId);
        examService.ensureCandidateAccess(exam, userId);
        ExamCandidate candidate = candidateMapper.findOneForUpdate(examId, userId)
                .orElseThrow(() -> forbidden("你不是本场考试的指定考生"));
        LocalDateTime now = now();
        assertStartAllowed(exam, candidate, now);

        ExamAttempt attempt = attemptMapper.findFirst(examId, userId).orElse(null);
        if (attempt == null) {
            LocalDateTime personalDeadline = now.plusMinutes(exam.getDurationMinutes());
            LocalDateTime deadline = personalDeadline.isBefore(exam.getEndAt())
                    ? personalDeadline
                    : exam.getEndAt();
            if (!deadline.isAfter(now)) {
                throw conflict("本场考试的作答时间已结束");
            }
            attempt = new ExamAttempt();
            attempt.setExamId(examId);
            attempt.setCandidateId(candidate.getId());
            attempt.setUserId(userId);
            attempt.setAttemptNo(1);
            attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(now);
            attempt.setDeadlineAt(deadline);
            attempt.setVersion(0);
            if (attemptMapper.insert(attempt) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建考试作答记录失败");
            }
            if (candidate.getStatus() == ExamCandidateStatus.ASSIGNED
                    && candidateMapper.markStarted(candidate.getId(), now) != 1) {
                throw conflict("考生状态已发生变化，请重试");
            }
        } else {
            assertAttemptCanContinue(attempt, now);
        }
        return startResponse(exam, attempt, now);
    }

    /** 恢复已存在且仍在有效时间内的作答，不创建第二次作答。 */
    @Transactional
    public ExamStartResponse resume(Long examId, Long userId) {
        Exam exam = examService.getRequired(examId);
        examService.ensureCandidateAccess(exam, userId);
        getCandidate(examId, userId);
        LocalDateTime now = now();
        ExamAttempt attempt = attemptMapper.findFirst(examId, userId)
                .orElseThrow(() -> conflict("请先开始考试"));
        assertAttemptCanContinue(attempt, now);
        return startResponse(exam, attempt, now);
    }

    /** 创建或初始化响应，并维护唯一性、初始状态和必要关联。 */
    private ExamStartResponse startResponse(Exam exam, ExamAttempt attempt, LocalDateTime now) {
        ExamPaper paper = paperService.getRequired(exam.getPaperId());
        runtimeStateService.rememberStarted(attempt.getId(), attempt.getDeadlineAt(), now);
        return new ExamStartResponse(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getDeadlineAt(),
                now,
                remainingSeconds(attempt.getDeadlineAt(), now),
                ExamSummaryResponse.from(exam),
                exam.getInstructions(),
                ExamPaperSummaryResponse.from(paper),
                paperService.candidateQuestions(paper.getId()),
                answerService.initializeAndList(attempt, paper.getId(), now)
        );
    }

    /** 执行 eligibility 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ExamEligibilityResponse eligibility(
            Exam exam,
            ExamCandidate candidate,
            ExamAttempt attempt,
            LocalDateTime now
    ) {
        String reason = null;
        boolean canStart = true;
        if (!isPublished(exam)) {
            canStart = false;
            reason = "考试尚未发布或已取消";
        } else if (now.isBefore(exam.getStartAt())) {
            canStart = false;
            reason = "考试尚未开始";
        } else if (!now.isBefore(exam.getEndAt())) {
            canStart = false;
            reason = "考试已结束";
        } else if (candidate.getStatus() == ExamCandidateStatus.SUBMITTED) {
            canStart = false;
            reason = "试卷已提交";
        } else if (attempt != null && attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            canStart = false;
            reason = "当前作答记录不能继续";
        } else if (attempt != null && !now.isBefore(attempt.getDeadlineAt())) {
            canStart = false;
            reason = "个人作答时间已结束";
        } else if (attempt != null) {
            reason = "可以继续考试";
        } else {
            reason = "可以开始考试";
        }
        return new ExamEligibilityResponse(
                exam.getId(),
                true,
                canStart,
                reason,
                candidate.getStatus(),
                now,
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes(),
                attempt == null ? null : attempt.getId(),
                attempt == null ? null : attempt.getStatus(),
                attempt == null ? null : attempt.getStartedAt(),
                attempt == null ? null : attempt.getDeadlineAt(),
                attempt == null ? 0 : remainingSeconds(attempt.getDeadlineAt(), now)
        );
    }

    /** 校验开始Allowed及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertStartAllowed(Exam exam, ExamCandidate candidate, LocalDateTime now) {
        if (!isPublished(exam)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试尚未发布或已取消");
        }
        if (now.isBefore(exam.getStartAt())) {
            throw conflict("考试尚未开始");
        }
        if (!now.isBefore(exam.getEndAt())) {
            throw conflict("考试已结束");
        }
        if (candidate.getStatus() == ExamCandidateStatus.SUBMITTED) {
            throw conflict("试卷已提交，不能再次开始");
        }
        if (candidate.getStatus() == ExamCandidateStatus.ABSENT) {
            throw conflict("已被标记为缺考，不能开始考试");
        }
    }

    /** 校验作答CanContinue及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertAttemptCanContinue(ExamAttempt attempt, LocalDateTime now) {
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw conflict("当前作答记录不能继续");
        }
        if (!now.isBefore(attempt.getDeadlineAt())) {
            throw conflict("个人作答时间已结束");
        }
    }

    /** 判断是否满足Published条件，不修改持久化状态。 */
    private boolean isPublished(Exam exam) {
        return exam.getStatus() == ExamStatus.PUBLISHED
                || exam.getStatus() == ExamStatus.ONGOING;
    }

    /** 返回考生。 */
    private ExamCandidate getCandidate(Long examId, Long userId) {
        return candidateMapper.findOne(examId, userId)
                .orElseThrow(() -> forbidden("你不是本场考试的指定考生"));
    }

    /** 执行 remainingSeconds 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private long remainingSeconds(LocalDateTime deadline, LocalDateTime now) {
        return Math.max(0, Duration.between(now, deadline).getSeconds());
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 执行 forbidden 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }

    /** 执行 conflict 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
