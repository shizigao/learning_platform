/* 文件职责：提供考生考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 提供考生考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class CandidateExamController {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 委托session执行对应领域规则。 */
    private final CandidateExamSessionService sessionService;
    /** 委托答案执行对应领域规则。 */
    private final ExamAnswerService answerService;
    /** 委托交卷执行对应领域规则。 */
    private final ExamSubmissionService submissionService;
    /** 委托成绩执行对应领域规则。 */
    private final ExamResultService resultService;
    /** 委托AI分析执行对应领域规则。 */
    private final ExamAiAnalysisService aiAnalysisService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ExamSummaryResponse>> list(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(examService.listAssigned(userId));
    }

    @GetMapping("/{examId}")
    /** 处理 GET /{examId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<CandidateExamResponse> detail(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(examService.candidateDetail(examId, userId));
    }

    @GetMapping("/{examId}/overview")
    /** 处理 GET /{examId}/overview 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<CandidateExamOverviewResponse> overview(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.overview(examId, userId));
    }

    @GetMapping("/{examId}/eligibility")
    /** 处理 GET /{examId}/eligibility 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamEligibilityResponse> eligibility(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.eligibility(examId, userId));
    }

    @PostMapping("/{examId}/start")
    /** 处理 POST /{examId}/start 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamStartResponse> start(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.start(examId, userId));
    }

    @GetMapping("/{examId}/session")
    /** 处理 GET /{examId}/session 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamStartResponse> session(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(sessionService.resume(examId, userId));
    }

    @PutMapping("/{examId}/answers/{questionId}")
    /** 处理 PUT /{examId}/answers/{questionId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 PUT /{examId}/answers 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ExamAnswerResponse>> saveAnswers(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamAnswerBatchSaveRequest request
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(answerService.saveBatch(examId, userId, request.answers()));
    }

    @PostMapping("/{examId}/submit")
    /** 处理 POST /{examId}/submit 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamSubmissionResponse> submit(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(submissionService.submitManual(examId, userId));
    }

    @GetMapping("/{examId}/result")
    /** 处理 GET /{examId}/result 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamResultDetailResponse> result(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(resultService.candidateResult(examId, userId));
    }

    @GetMapping("/{examId}/result/ai-analysis")
    /** 处理 GET /{examId}/result/ai-analysis 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamAiAnalysisPageResponse> resultAiAnalysis(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(aiAnalysisService.personalPage(examId, userId));
    }

    @PostMapping("/{examId}/result/ai-analysis")
    /** 处理 POST /{examId}/result/ai-analysis 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
