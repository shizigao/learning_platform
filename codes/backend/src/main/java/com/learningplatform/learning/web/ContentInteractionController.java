/* 文件职责：提供学习资料Interaction相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.learning.dto.ContentCommentResponse;
import com.learningplatform.learning.dto.ContentReactionResponse;
import com.learningplatform.learning.dto.CreateCommentRequest;
import com.learningplatform.learning.service.ContentInteractionService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents/{contentId}")
/**
 * 提供学习资料Interaction相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class ContentInteractionController {
    /** 委托interaction执行对应领域规则。 */
    private final ContentInteractionService interactionService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ContentInteractionController(ContentInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/reactions")
    /** 处理 GET /reactions 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentReactionResponse> state(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.state(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/like")
    /** 处理 POST /like 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentReactionResponse> like(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.like(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/like")
    /** 处理 DELETE /like 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentReactionResponse> unlike(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.unlike(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/favorite")
    /** 处理 POST /favorite 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentReactionResponse> favorite(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.favorite(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/favorite")
    /** 处理 DELETE /favorite 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentReactionResponse> unfavorite(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.unfavorite(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/comments")
    /** 处理 POST /comments 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentCommentResponse> comment(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.comment(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @GetMapping("/comments")
    /** 处理 GET /comments 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentCommentResponse>> comments(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @ModelAttribute PageQuery query
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.comments(
                contentId,
                principal.userId(),
                isAdmin(principal),
                query
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
