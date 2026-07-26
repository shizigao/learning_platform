package com.learningplatform.exam.web;

import com.learningplatform.ai.dto.WrongQuestionAnalysisGenerateRequest;
import com.learningplatform.ai.dto.WrongQuestionAnalysisResponse;
import com.learningplatform.ai.dto.WrongQuestionReviewPageResponse;
import com.learningplatform.ai.service.WrongQuestionReviewService;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams/wrong-review")
public class WrongQuestionReviewController {
    private final WrongQuestionReviewService reviewService;

    public WrongQuestionReviewController(
            WrongQuestionReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<WrongQuestionReviewPageResponse> page(
            Authentication authentication
    ) {
        return ApiResponse.success(reviewService.page(
                AuthenticationPrincipalResolver.require(authentication).userId()
        ));
    }

    @PostMapping("/analysis")
    public ApiResponse<WrongQuestionAnalysisResponse> analyze(
            Authentication authentication,
            @Valid @RequestBody WrongQuestionAnalysisGenerateRequest request
    ) {
        return ApiResponse.success(reviewService.generate(
                AuthenticationPrincipalResolver.require(authentication).userId(),
                request.requestId()
        ));
    }
}
