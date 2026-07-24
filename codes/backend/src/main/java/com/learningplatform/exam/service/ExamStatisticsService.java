package com.learningplatform.exam.service;

import com.learningplatform.exam.domain.ExamQuestionStatistics;
import com.learningplatform.exam.domain.ExamStatisticsSummary;
import com.learningplatform.exam.dto.ExamQuestionStatisticsResponse;
import com.learningplatform.exam.dto.ExamStatisticsResponse;
import com.learningplatform.exam.mapper.ExamStatisticsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExamStatisticsService {
    private final ExamService examService;
    private final ExamStatisticsMapper statisticsMapper;

    public ExamStatisticsService(
            ExamService examService,
            ExamStatisticsMapper statisticsMapper
    ) {
        this.examService = examService;
        this.statisticsMapper = statisticsMapper;
    }

    public ExamStatisticsResponse statistics(
            Long examId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        examService.detail(examId, requesterId, requesterAdmin);
        ExamStatisticsSummary summary = statisticsMapper.summary(examId);
        int gradedCount = value(summary.getGradedCount());
        return new ExamStatisticsResponse(
                examId,
                value(summary.getTotalCandidates()),
                value(summary.getParticipatedCount()),
                value(summary.getSubmittedCount()),
                value(summary.getNotParticipatedCount()),
                gradedCount,
                score(summary.getAverageScore()),
                score(summary.getHighestScore()),
                score(summary.getLowestScore()),
                value(summary.getPassedCount()),
                rate(value(summary.getPassedCount()), gradedCount),
                statisticsMapper.questionStatistics(examId).stream()
                        .map(this::questionResponse)
                        .toList()
        );
    }

    private ExamQuestionStatisticsResponse questionResponse(ExamQuestionStatistics row) {
        return new ExamQuestionStatisticsResponse(
                row.getQuestionId(),
                value(row.getSortOrder()),
                row.getQuestionType(),
                row.getStem(),
                row.getMaxScore(),
                value(row.getGradedCount()),
                value(row.getAnsweredCount()),
                value(row.getCorrectCount()),
                rate(value(row.getCorrectCount()), value(row.getGradedCount()))
        );
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal score(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
