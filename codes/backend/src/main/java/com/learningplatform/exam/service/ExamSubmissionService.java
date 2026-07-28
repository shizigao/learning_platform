/* 文件职责：实现考试交卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamSubmissionType;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.dto.ExamSubmissionResponse;
import com.learningplatform.exam.mapper.ExamAnswerMapper;
import com.learningplatform.exam.mapper.ExamAttemptMapper;
import com.learningplatform.exam.mapper.ExamCandidateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
/**
 * 实现考试交卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamSubmissionService {
    /** 访问作答持久化数据。 */
    private final ExamAttemptMapper attemptMapper;
    /** 访问考生持久化数据。 */
    private final ExamCandidateMapper candidateMapper;
    /** 访问答案持久化数据。 */
    private final ExamAnswerMapper answerMapper;
    /** 委托运行态State执行对应领域规则。 */
    private final ExamRuntimeStateService runtimeStateService;
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 委托阅卷执行对应领域规则。 */
    private final ExamGradingService gradingService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamSubmissionService(
            ExamAttemptMapper attemptMapper,
            ExamCandidateMapper candidateMapper,
            ExamAnswerMapper answerMapper,
            ExamRuntimeStateService runtimeStateService,
            ExamService examService,
            ExamGradingService gradingService
    ) {
        this.attemptMapper = attemptMapper;
        this.candidateMapper = candidateMapper;
        this.answerMapper = answerMapper;
        this.runtimeStateService = runtimeStateService;
        this.examService = examService;
        this.gradingService = gradingService;
    }

    @Transactional
    /** 执行提交状态流转，仅允许从合法前置状态进入目标状态。 */
    public ExamSubmissionResponse submitManual(Long examId, Long userId) {
        ExamAttempt attempt = attemptMapper.findFirstForUpdate(examId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "请先开始考试"));
        if (attempt.getStatus() == ExamAttemptStatus.SUBMITTED
                || attempt.getStatus() == ExamAttemptStatus.GRADING
                || attempt.getStatus() == ExamAttemptStatus.COMPLETED) {
            return response(attempt);
        }
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前作答记录不能提交");
        }
        LocalDateTime now = now();
        ExamSubmissionType type = now.isBefore(attempt.getDeadlineAt())
                ? ExamSubmissionType.MANUAL
                : ExamSubmissionType.TIMEOUT;
        return complete(attempt, type, now);
    }

    @Transactional
    /** 执行提交状态流转，仅允许从合法前置状态进入目标状态。 */
    public boolean submitExpired(Long attemptId) {
        ExamAttempt attempt = attemptMapper.findByIdForUpdate(attemptId).orElse(null);
        if (attempt == null || attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            return false;
        }
        LocalDateTime now = now();
        if (now.isBefore(attempt.getDeadlineAt())) {
            return false;
        }
        complete(attempt, ExamSubmissionType.TIMEOUT, now);
        return true;
    }

    /** 执行完成状态流转，仅允许从合法前置状态进入目标状态。 */
    private ExamSubmissionResponse complete(
            ExamAttempt attempt,
            ExamSubmissionType type,
            LocalDateTime submittedAt
    ) {
        if (attemptMapper.markSubmitted(attempt.getId(), submittedAt, type) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "作答状态已发生变化，请重试");
        }
        candidateMapper.markSubmitted(attempt.getCandidateId(), submittedAt);
        attempt.setStatus(ExamAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);
        attempt.setSubmissionType(type);
        Exam exam = examService.getRequired(attempt.getExamId());
        attempt = gradingService.gradeSubmittedAttempt(attempt, exam);
        runtimeStateService.clear(attempt.getId());
        return response(attempt);
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ExamSubmissionResponse response(ExamAttempt attempt) {
        return new ExamSubmissionResponse(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getSubmittedAt(),
                attempt.getSubmissionType(),
                answerMapper.countAnswered(attempt.getId()),
                answerMapper.countTotal(attempt.getId())
        );
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
