/* 文件职责：提供发布者考试阅卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.ai.dto.ExamAiAnalysisGenerateRequest;
import com.learningplatform.ai.dto.ExamAiAnalysisPageResponse;
import com.learningplatform.ai.dto.ExamAiAnalysisResponse;
import com.learningplatform.ai.service.ExamAiAnalysisService;
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
/**
 * 提供发布者考试阅卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherExamGradingController {
    /** 委托阅卷执行对应领域规则。 */
    private final ExamGradingService gradingService;
    /** 委托statistics执行对应领域规则。 */
    private final ExamStatisticsService statisticsService;
    /** 委托AI分析执行对应领域规则。 */
    private final ExamAiAnalysisService aiAnalysisService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherExamGradingController(
            ExamGradingService gradingService,
            ExamStatisticsService statisticsService,
            ExamAiAnalysisService aiAnalysisService
    ) {
        this.gradingService = gradingService;
        this.statisticsService = statisticsService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping("/attempts")
    /** 处理 GET /attempts 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 GET /attempts/{attemptId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 PUT /attempts/{attemptId}/answers/{answerId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 POST /attempts/{attemptId}/complete 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 GET /statistics 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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

    @GetMapping("/ai-analysis")
    /** 处理 GET /ai-analysis 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamAiAnalysisPageResponse> aiAnalysis(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        Long userId = principal(authentication).userId();
        return ApiResponse.success(aiAnalysisService.overallPage(examId, userId));
    }

    @PostMapping("/ai-analysis")
    /** 处理 POST /ai-analysis 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamAiAnalysisResponse> generateAiAnalysis(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamAiAnalysisGenerateRequest request
    ) {
        Long userId = principal(authentication).userId();
        return ApiResponse.success(aiAnalysisService.generateOverall(
                examId,
                userId,
                request.requestId()
        ));
    }

    /** 执行 principal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 判断是否满足管理条件，不修改持久化状态。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
