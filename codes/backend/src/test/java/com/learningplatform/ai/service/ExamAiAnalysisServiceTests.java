package com.learningplatform.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.mapper.ExamAiAnalysisMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.dto.ExamStatisticsResponse;
import com.learningplatform.exam.mapper.ExamAnswerMapper;
import com.learningplatform.exam.mapper.ExamPaperMapper;
import com.learningplatform.exam.mapper.ExamResultMapper;
import com.learningplatform.exam.service.ExamResultService;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.exam.service.ExamStatisticsService;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.service.EntitlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamAiAnalysisServiceTests {
    @Mock private AiClient aiClient;
    @Mock private AiRequestGuard requestGuard;
    @Mock private AiTaskLifecycleService taskService;
    @Mock private AiQuotaService quotaService;
    @Mock private ExamAiAnalysisPersistenceService persistenceService;
    @Mock private ExamAiAnalysisMapper analysisMapper;
    @Mock private ExamService examService;
    @Mock private ExamStatisticsService statisticsService;
    @Mock private ExamResultService resultService;
    @Mock private ExamResultMapper resultMapper;
    @Mock private ExamPaperMapper paperMapper;
    @Mock private ExamAnswerMapper answerMapper;
    @Mock private EntitlementService entitlementService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ExamAiAnalysisService service;

    @Test
    void overallPageIsDisabledBeforeExamEnds() {
        Exam exam = exam(11L, 7L, LocalDateTime.now().plusHours(1), true);
        when(examService.getRequired(11L)).thenReturn(exam);
        when(analysisMapper.findHistory(
                11L, null, 7L,
                com.learningplatform.ai.domain.ExamAiAnalysisScope.OVERALL
        )).thenReturn(List.of());
        when(entitlementService.availableQuota(
                7L, EntitlementType.EXAM_OVERALL_AI_QUOTA
        )).thenReturn(3);

        var page = service.overallPage(11L, 7L);

        assertThat(page.eligible()).isFalse();
        assertThat(page.ineligibleReason()).contains("考试结束后");
        assertThat(page.quotaRemaining()).isEqualTo(3);
        verify(statisticsService, never()).statistics(11L, 7L, false);
    }

    @Test
    void personalPageDoesNotBypassHiddenAnswerPolicy() {
        Exam exam = exam(12L, 7L, LocalDateTime.now().minusHours(1), false);
        ExamResult result = new ExamResult();
        result.setAttemptId(21L);
        result.setGradingCompleted(true);
        result.setVisibleToCandidate(true);
        when(examService.getRequired(12L)).thenReturn(exam);
        when(resultMapper.findCandidateResult(12L, 9L))
                .thenReturn(Optional.of(result));
        when(analysisMapper.findHistory(
                12L, 21L, 9L,
                com.learningplatform.ai.domain.ExamAiAnalysisScope.PERSONAL
        )).thenReturn(List.of());
        when(entitlementService.availableQuota(
                9L, EntitlementType.EXAM_PERSONAL_AI_QUOTA
        )).thenReturn(2);

        var page = service.personalPage(12L, 9L);

        assertThat(page.eligible()).isFalse();
        assertThat(page.ineligibleReason()).contains("未开放答案");
    }

    @Test
    void overallGenerationRequiresEverySubmissionToBeGraded() {
        Exam exam = exam(13L, 7L, LocalDateTime.now().minusHours(1), true);
        when(examService.getRequired(13L)).thenReturn(exam);
        when(statisticsService.statistics(13L, 7L, false))
                .thenReturn(statistics(2, 1));

        assertThatThrownBy(() ->
                service.generateOverall(13L, 7L, "request-1")
        )
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception.getMessage()).contains("全部交卷完成阅卷");
                });
        verify(taskService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    private Exam exam(
            Long id,
            Long publisherId,
            LocalDateTime endAt,
            boolean showAnswers
    ) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setPublisherId(publisherId);
        exam.setPaperId(31L);
        exam.setName("测试考试");
        exam.setEndAt(endAt);
        exam.setShowAnswerAfterFinish(showAnswers);
        return exam;
    }

    private ExamStatisticsResponse statistics(int submitted, int graded) {
        return new ExamStatisticsResponse(
                13L,
                2,
                2,
                submitted,
                0,
                graded,
                new BigDecimal("70.00"),
                new BigDecimal("80.00"),
                new BigDecimal("60.00"),
                1,
                new BigDecimal("50.00"),
                List.of()
        );
    }
}
