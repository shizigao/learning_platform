package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.ai.dto.ExamAiAnalysisGenerateRequest;
import com.learningplatform.ai.dto.ExamAiAnalysisPageResponse;
import com.learningplatform.ai.dto.ExamAiAnalysisResponse;
import com.learningplatform.ai.service.ExamAiAnalysisService;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.exam.dto.CandidateExamResponse;
import com.learningplatform.exam.dto.CandidateExamOverviewResponse;
import com.learningplatform.exam.dto.ExamEligibilityResponse;
import com.learningplatform.exam.dto.ExamAnswerBatchSaveRequest;
import com.learningplatform.exam.dto.ExamAnswerResponse;
import com.learningplatform.exam.dto.ExamAnswerWriteRequest;
import com.learningplatform.exam.dto.ExamStartResponse;
import com.learningplatform.exam.dto.ExamSubmissionResponse;
import com.learningplatform.exam.dto.ExamResultDetailResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.service.CandidateExamSessionService;
import com.learningplatform.exam.service.ExamAnswerService;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.exam.service.ExamSubmissionService;
import com.learningplatform.exam.service.ExamResultService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class CandidateExamController {
    private final ExamService examService;
    private final CandidateExamSessionService sessionService;
    private final ExamAnswerService answerService;
    private final ExamSubmissionService submissionService;
    private final ExamResultService resultService;
    private final ExamAiAnalysisService aiAnalysisService;

    public CandidateExamController(
            ExamService examService,
            CandidateExamSessionService sessionService,
            ExamAnswerService answerService,
            ExamSubmissionService submissionService,
            ExamResultService resultService,
            ExamAiAnalysisService aiAnalysisService
    ) {
        this.examService = examService;
        this.sessionService = sessionService;
        this.answerService = answerService;
        this.submissionService = submissionService;
        this.resultService = resultService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping
    public ApiResponse<List<ExamSummaryResponse>> list(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(examService.listAssigned(userId));
    }

    @GetMapping("/{examId}")
    public ApiResponse<CandidateExamResponse> detail(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(examService.candidateDetail(examId, userId));
    }

    @GetMapping("/{examId}/overview")
    public ApiResponse<CandidateExamOverviewResponse> overview(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.overview(examId, userId));
    }

    @GetMapping("/{examId}/eligibility")
    public ApiResponse<ExamEligibilityResponse> eligibility(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.eligibility(examId, userId));
    }

    @PostMapping("/{examId}/start")
    public ApiResponse<ExamStartResponse> start(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.start(examId, userId));
    }

    @GetMapping("/{examId}/session")
    public ApiResponse<ExamStartResponse> session(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.resume(examId, userId));
    }

    @PutMapping("/{examId}/answers/{questionId}")
    public ApiResponse<ExamAnswerResponse> saveAnswer(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody ExamAnswerWriteRequest request
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(answerService.saveOne(examId, userId, questionId, request));
    }

    @PutMapping("/{examId}/answers")
    public ApiResponse<List<ExamAnswerResponse>> saveAnswers(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamAnswerBatchSaveRequest request
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(answerService.saveBatch(examId, userId, request.answers()));
    }

    @PostMapping("/{examId}/submit")
    public ApiResponse<ExamSubmissionResponse> submit(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(submissionService.submitManual(examId, userId));
    }

    @GetMapping("/{examId}/result")
    public ApiResponse<ExamResultDetailResponse> result(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(resultService.candidateResult(examId, userId));
    }

    @GetMapping("/{examId}/result/ai-analysis")
    public ApiResponse<ExamAiAnalysisPageResponse> resultAiAnalysis(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(aiAnalysisService.personalPage(examId, userId));
    }

    @PostMapping("/{examId}/result/ai-analysis")
    public ApiResponse<ExamAiAnalysisResponse> generateResultAiAnalysis(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamAiAnalysisGenerateRequest request
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(aiAnalysisService.generatePersonal(
                examId,
                userId,
                request.requestId()
        ));
    }
}
