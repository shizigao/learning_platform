/* 文件职责：实现考试Statistics业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现考试Statistics业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamStatisticsService {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 访问statistics持久化数据。 */
    private final ExamStatisticsMapper statisticsMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamStatisticsService(
            ExamService examService,
            ExamStatisticsMapper statisticsMapper
    ) {
        this.examService = examService;
        this.statisticsMapper = statisticsMapper;
    }

    /** 执行 statistics 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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

    /** 执行 questionResponse 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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

    /** 执行 rate 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /** 执行分数核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    private BigDecimal score(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** 执行 value 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
