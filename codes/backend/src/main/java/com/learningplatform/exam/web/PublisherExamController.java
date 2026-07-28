/* 文件职责：提供发布者考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.dto.ExamListQuery;
import com.learningplatform.exam.dto.ExamManagementResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.dto.ExamWriteRequest;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/publisher/exams")
/**
 * 提供发布者考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherExamController {
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ExamSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute ExamListQuery query
    ) {
        return ApiResponse.success(examService.list(principal(authentication).userId(), query));
    }

    @PostMapping
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamManagementResponse> create(
            Authentication authentication,
            @Valid @RequestBody ExamWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.create(
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @GetMapping("/{examId}")
    /** 处理 GET /{examId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamManagementResponse> detail(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.detail(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/{examId}")
    /** 处理 PUT /{examId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamManagementResponse> update(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.update(
                examId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @PostMapping("/{examId}/publish")
    /** 处理 POST /{examId}/publish 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamManagementResponse> publish(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.publish(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/{examId}/cancel")
    /** 处理 POST /{examId}/cancel 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamManagementResponse> cancel(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.cancel(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/{examId}")
    /** 处理 DELETE /{examId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        examService.delete(examId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @GetMapping("/quota")
    /** 处理 GET /quota 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Integer> quota(Authentication authentication) {
        return ApiResponse.success(examService.availableQuota(principal(authentication).userId()));
    }

    /** 处理 GET /quota 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 处理 GET /quota 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
