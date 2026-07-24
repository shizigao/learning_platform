package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.exam.dto.ExamGradingAttemptResponse;
import com.learningplatform.exam.dto.ExamGradingDetailResponse;
import com.learningplatform.exam.dto.ExamResultQuestionResponse;
import com.learningplatform.exam.dto.ExamResultSummaryResponse;
import com.learningplatform.exam.dto.ExamStatisticsResponse;
import com.learningplatform.exam.dto.ManualGradeRequest;
import com.learningplatform.exam.service.ExamGradingService;
import com.learningplatform.exam.service.ExamStatisticsService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publisher/exams/{examId}/grading")
public class PublisherExamGradingController {
    private final ExamGradingService gradingService;
    private final ExamStatisticsService statisticsService;

    public PublisherExamGradingController(
            ExamGradingService gradingService,
            ExamStatisticsService statisticsService
    ) {
        this.gradingService = gradingService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/attempts")
    public ApiResponse<List<ExamGradingAttemptResponse>> attempts(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(gradingService.listAttempts(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<ExamGradingDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long attemptId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(gradingService.detail(
                examId,
                attemptId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/attempts/{attemptId}/answers/{answerId}")
    public ApiResponse<ExamResultQuestionResponse> grade(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long attemptId,
            @PathVariable Long answerId,
            @Valid @RequestBody ManualGradeRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(gradingService.gradeAnswer(
                examId,
                attemptId,
                answerId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public ApiResponse<ExamResultSummaryResponse> complete(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long attemptId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(gradingService.completeReview(
                examId,
                attemptId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/statistics")
    public ApiResponse<ExamStatisticsResponse> statistics(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(statisticsService.statistics(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
