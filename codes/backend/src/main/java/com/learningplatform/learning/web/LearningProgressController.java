/* 文件职责：提供学习进度相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.learning.dto.LearningProgressResponse;
import com.learningplatform.learning.dto.UpdateLearningProgressRequest;
import com.learningplatform.learning.service.LearningProgressService;
import com.learningplatform.learning.service.ContentInteractionService;
import com.learningplatform.content.dto.ContentSummaryResponse;
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
@RequestMapping("/api/learning")
/**
 * 提供学习进度相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class LearningProgressController {
    /** 委托进度执行对应领域规则。 */
    private final LearningProgressService progressService;
    /** 委托interaction执行对应领域规则。 */
    private final ContentInteractionService interactionService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public LearningProgressController(
            LearningProgressService progressService,
            ContentInteractionService interactionService
    ) {
        this.progressService = progressService;
        this.interactionService = interactionService;
    }

    @PostMapping("/contents/{contentId}/start")
    /** 处理 POST /contents/{contentId}/start 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<LearningProgressResponse> start(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(progressService.start(
                principal.userId(),
                isAdmin(principal),
                contentId
        ));
    }

    @PutMapping("/contents/{contentId}/progress")
    /** 处理 PUT /contents/{contentId}/progress 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<LearningProgressResponse> update(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateLearningProgressRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(progressService.update(
                principal.userId(),
                isAdmin(principal),
                contentId,
                request
        ));
    }

    @GetMapping("/contents/{contentId}/progress")
    /** 处理 GET /contents/{contentId}/progress 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<LearningProgressResponse> get(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        return ApiResponse.success(progressService.get(principal(authentication).userId(), contentId));
    }

    @GetMapping("/progress")
    /** 处理 GET /progress 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<LearningProgressResponse>> list(Authentication authentication) {
        return ApiResponse.success(progressService.list(principal(authentication).userId()));
    }

    @GetMapping("/favorites")
    /** 处理 GET /favorites 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ContentSummaryResponse>> favorites(Authentication authentication) {
        return ApiResponse.success(interactionService.favorites(principal(authentication).userId()));
    }

    /** 处理 GET /favorites 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 处理 GET /favorites 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
