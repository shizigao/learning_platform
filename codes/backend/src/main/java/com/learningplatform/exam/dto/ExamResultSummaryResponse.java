package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.domain.ExamAnswerGradingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamResultSummaryResponse(
        Long id,
        Long examId,
        Long attemptId,
        Long userId,
        BigDecimal totalScore,
        BigDecimal passingScore,
        boolean passed,
        int correctCount,
        int partialCreditCount,
        int incorrectCount,
        int unansweredCount,
        int pendingReviewCount,
        boolean gradingCompleted,
        LocalDateTime generatedAt
) {
    public static ExamResultSummaryResponse from(ExamResult result) {
        return new ExamResultSummaryResponse(
                result.getId(),
                result.getExamId(),
                result.getAttemptId(),
                result.getUserId(),
                result.getTotalScore(),
                result.getPassingScore(),
                Boolean.TRUE.equals(result.getPassed()),
                result.getCorrectCount(),
                0,
                result.getIncorrectCount(),
                result.getUnansweredCount(),
                0,
                Boolean.TRUE.equals(result.getGradingCompleted()),
                result.getGeneratedAt()
        );
    }

    public static ExamResultSummaryResponse from(
            ExamResult result,
            List<ExamResultQuestionResponse> questions
    ) {
        int correctCount = (int) questions.stream()
                .filter(question -> Boolean.TRUE.equals(question.correct()))
                .count();
        int partialCreditCount = (int) questions.stream()
                .filter(question -> question.score() != null)
                .filter(question -> question.score().compareTo(BigDecimal.ZERO) > 0)
                .filter(question -> question.score().compareTo(question.maxScore()) < 0)
                .count();
        int incorrectCount = (int) questions.stream()
                .filter(question -> question.score() != null)
                .filter(question -> question.score().compareTo(BigDecimal.ZERO) == 0)
                .filter(question -> question.gradingStatus() != ExamAnswerGradingStatus.UNANSWERED)
                .filter(question -> question.gradingStatus() != ExamAnswerGradingStatus.PENDING_REVIEW)
                .count();
        int unansweredCount = (int) questions.stream()
                .filter(question -> question.gradingStatus() == ExamAnswerGradingStatus.UNANSWERED)
                .count();
        int pendingReviewCount = (int) questions.stream()
                .filter(question -> question.gradingStatus() == ExamAnswerGradingStatus.PENDING_REVIEW)
                .count();
        return new ExamResultSummaryResponse(
                result.getId(),
                result.getExamId(),
                result.getAttemptId(),
                result.getUserId(),
                result.getTotalScore(),
                result.getPassingScore(),
                Boolean.TRUE.equals(result.getPassed()),
                correctCount,
                partialCreditCount,
                incorrectCount,
                unansweredCount,
                pendingReviewCount,
                Boolean.TRUE.equals(result.getGradingCompleted()),
                result.getGeneratedAt()
        );
    }
}
