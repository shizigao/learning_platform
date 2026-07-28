/* 文件职责：提供发布者题目相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：题库、题目、选项与标准答案；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.question.dto.QuestionBankResponse;
import com.learningplatform.question.dto.QuestionBankWriteRequest;
import com.learningplatform.question.dto.QuestionListQuery;
import com.learningplatform.question.dto.QuestionManagementResponse;
import com.learningplatform.question.dto.QuestionWriteRequest;
import com.learningplatform.question.service.QuestionBankService;
import com.learningplatform.question.service.QuestionService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/publisher")
/**
 * 提供发布者题目相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherQuestionController {
    /** 委托题库执行对应领域规则。 */
    private final QuestionBankService bankService;
    /** 委托题目执行对应领域规则。 */
    private final QuestionService questionService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherQuestionController(
            QuestionBankService bankService,
            QuestionService questionService
    ) {
        this.bankService = bankService;
        this.questionService = questionService;
    }

    @GetMapping("/question-banks")
    /** 处理 GET /question-banks 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<QuestionBankResponse>> listBanks(Authentication authentication) {
        return ApiResponse.success(bankService.list(principal(authentication).userId()));
    }

    @PostMapping("/question-banks")
    /** 处理 POST /question-banks 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<QuestionBankResponse> createBank(
            Authentication authentication,
            @Valid @RequestBody QuestionBankWriteRequest request
    ) {
        return ApiResponse.success(bankService.create(principal(authentication).userId(), request));
    }

    @PutMapping("/question-banks/{bankId}")
    /** 处理 PUT /question-banks/{bankId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<QuestionBankResponse> updateBank(
            Authentication authentication,
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionBankWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(bankService.update(
                bankId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/question-banks/{bankId}")
    /** 处理 DELETE /question-banks/{bankId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> deleteBank(
            Authentication authentication,
            @PathVariable Long bankId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        bankService.delete(bankId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @GetMapping("/questions")
    /** 处理 GET /questions 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<QuestionManagementResponse>> listQuestions(
            Authentication authentication,
            @Valid @ModelAttribute QuestionListQuery query
    ) {
        return ApiResponse.success(questionService.list(principal(authentication).userId(), query));
    }

    @PostMapping("/questions")
    /** 处理 POST /questions 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<QuestionManagementResponse> createQuestion(
            Authentication authentication,
            @Valid @RequestBody QuestionWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(questionService.create(
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @GetMapping("/questions/{questionId}")
    /** 处理 GET /questions/{questionId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<QuestionManagementResponse> questionDetail(
            Authentication authentication,
            @PathVariable Long questionId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(questionService.detail(
                questionId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/questions/{questionId}")
    /** 处理 PUT /questions/{questionId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<QuestionManagementResponse> updateQuestion(
            Authentication authentication,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(questionService.update(
                questionId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/questions/{questionId}")
    /** 处理 DELETE /questions/{questionId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> deleteQuestion(
            Authentication authentication,
            @PathVariable Long questionId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        questionService.delete(questionId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    /** 处理 DELETE /questions/{questionId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 判断是否满足管理条件，不修改持久化状态。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
