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
public class ExamSubmissionService {
    private final ExamAttemptMapper attemptMapper;
    private final ExamCandidateMapper candidateMapper;
    private final ExamAnswerMapper answerMapper;
    private final ExamRuntimeStateService runtimeStateService;
    private final ExamService examService;
    private final ExamGradingService gradingService;

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

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
