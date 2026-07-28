/* 文件职责：提供发布者考试试卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.dto.ExamPaperDetailResponse;
import com.learningplatform.exam.dto.ExamPaperListQuery;
import com.learningplatform.exam.dto.ExamPaperSummaryResponse;
import com.learningplatform.exam.dto.ExamPaperWriteRequest;
import com.learningplatform.exam.dto.ReplacePaperQuestionsRequest;
import com.learningplatform.exam.service.ExamPaperService;
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
@RequestMapping("/api/publisher/papers")
/**
 * 提供发布者考试试卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherExamPaperController {
    /** 委托试卷执行对应领域规则。 */
    private final ExamPaperService paperService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherExamPaperController(ExamPaperService paperService) {
        this.paperService = paperService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ExamPaperSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute ExamPaperListQuery query
    ) {
        return ApiResponse.success(paperService.list(principal(authentication).userId(), query));
    }

    @PostMapping
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamPaperDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody ExamPaperWriteRequest request
    ) {
        return ApiResponse.success(paperService.create(principal(authentication).userId(), request));
    }

    @GetMapping("/{paperId}")
    /** 处理 GET /{paperId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamPaperDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long paperId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.detail(
                paperId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/{paperId}")
    /** 处理 PUT /{paperId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamPaperDetailResponse> update(
            Authentication authentication,
            @PathVariable Long paperId,
            @Valid @RequestBody ExamPaperWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.update(
                paperId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @PutMapping("/{paperId}/questions")
    /** 处理 PUT /{paperId}/questions 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ExamPaperDetailResponse> replaceQuestions(
            Authentication authentication,
            @PathVariable Long paperId,
            @Valid @RequestBody ReplacePaperQuestionsRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.replaceQuestions(
                paperId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/{paperId}")
    /** 处理 DELETE /{paperId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long paperId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        paperService.delete(paperId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    /** 处理 DELETE /{paperId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 判断是否满足管理条件，不修改持久化状态。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
