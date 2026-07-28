/* 文件职责：提供AI学习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.web;

import com.learningplatform.ai.dto.AiConversationCreateRequest;
import com.learningplatform.ai.dto.AiConversationResponse;
import com.learningplatform.ai.dto.AiExplanationRequest;
import com.learningplatform.ai.dto.AiExplanationResponse;
import com.learningplatform.ai.dto.AiSummaryGenerateRequest;
import com.learningplatform.ai.dto.AiSummaryResponse;
import com.learningplatform.ai.dto.AiTemplateRequest;
import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.ai.dto.AiUsageRecordResponse;
import com.learningplatform.ai.service.AiConversationService;
import com.learningplatform.ai.service.AiQuotaService;
import com.learningplatform.ai.service.AiSummaryService;
import com.learningplatform.ai.service.AiTaskLifecycleService;
import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
/**
 * 提供AI学习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AiLearningController {
    /** 委托总结执行对应领域规则。 */
    private final AiSummaryService summaryService;
    /** 委托会话执行对应领域规则。 */
    private final AiConversationService conversationService;
    /** 委托任务执行对应领域规则。 */
    private final AiTaskLifecycleService taskService;
    /** 委托额度执行对应领域规则。 */
    private final AiQuotaService quotaService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiLearningController(
            AiSummaryService summaryService,
            AiConversationService conversationService,
            AiTaskLifecycleService taskService,
            AiQuotaService quotaService
    ) {
        this.summaryService = summaryService;
        this.conversationService = conversationService;
        this.taskService = taskService;
        this.quotaService = quotaService;
    }

    @PostMapping("/contents/{contentId}/summaries")
    /** 处理 POST /contents/{contentId}/summaries 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiSummaryResponse> generateSummary(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody(required = false) AiSummaryGenerateRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        // 生成总结，调用summaryService.generate，点击generate
        return ApiResponse.success(summaryService.generate(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request == null ? null : request.requestId()
        ));
    }

    @GetMapping("/contents/{contentId}/summaries/latest")
    /** 处理 GET /contents/{contentId}/summaries/latest 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiSummaryResponse> latestSummary(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(summaryService.latest(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/tasks/{taskId}")
    /** 处理 GET /tasks/{taskId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiTaskResponse> task(
            Authentication authentication,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(taskService.detail(
                taskId,
                principal(authentication).userId()
        ));
    }

    @GetMapping("/tasks")
    /** 处理 GET /tasks 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<AiTaskResponse>> tasks(
            Authentication authentication
    ) {
        return ApiResponse.success(taskService.list(
                principal(authentication).userId()
        ));
    }

    @GetMapping("/usage-records")
    /** 处理 GET /usage-records 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<AiUsageRecordResponse>> usageRecords(
            Authentication authentication
    ) {
        return ApiResponse.success(quotaService.records(
                principal(authentication).userId()
        ));
    }

    @PostMapping("/contents/{contentId}/conversations")
    /** 处理 POST /contents/{contentId}/conversations 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiConversationResponse> createConversation(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody(required = false) AiConversationCreateRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.create(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request == null ? null : request.title()
        ));
    }

    @GetMapping("/contents/{contentId}/conversations")
    /** 处理 GET /contents/{contentId}/conversations 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<AiConversationResponse>> conversations(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.list(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/conversations/{conversationId}")
    /** 处理 GET /conversations/{conversationId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiConversationResponse> conversation(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.detail(
                conversationId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    /** 处理 POST /conversations/{conversationId}/messages 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiExplanationResponse> explain(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiExplanationRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.explain(
                conversationId,
                principal.userId(),
                isAdmin(principal),
                request.requestId(),
                request.question()
        ));
    }

    @PostMapping("/conversations/{conversationId}/templates")
    /** 处理 POST /conversations/{conversationId}/templates 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AiExplanationResponse> explainWithTemplate(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiTemplateRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.explainTemplate(
                conversationId,
                principal.userId(),
                isAdmin(principal),
                request.requestId(),
                request.template()
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
