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
public class PublisherQuestionController {
    private final QuestionBankService bankService;
    private final QuestionService questionService;

    public PublisherQuestionController(
            QuestionBankService bankService,
            QuestionService questionService
    ) {
        this.bankService = bankService;
        this.questionService = questionService;
    }

    @GetMapping("/question-banks")
    public ApiResponse<List<QuestionBankResponse>> listBanks(Authentication authentication) {
        return ApiResponse.success(bankService.list(principal(authentication).userId()));
    }

    @PostMapping("/question-banks")
    public ApiResponse<QuestionBankResponse> createBank(
            Authentication authentication,
            @Valid @RequestBody QuestionBankWriteRequest request
    ) {
        return ApiResponse.success(bankService.create(principal(authentication).userId(), request));
    }

    @PutMapping("/question-banks/{bankId}")
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
    public ApiResponse<Void> deleteBank(
            Authentication authentication,
            @PathVariable Long bankId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        bankService.delete(bankId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @GetMapping("/questions")
    public ApiResponse<PageResult<QuestionManagementResponse>> listQuestions(
            Authentication authentication,
            @Valid @ModelAttribute QuestionListQuery query
    ) {
        return ApiResponse.success(questionService.list(principal(authentication).userId(), query));
    }

    @PostMapping("/questions")
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
    public ApiResponse<Void> deleteQuestion(
            Authentication authentication,
            @PathVariable Long questionId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        questionService.delete(questionId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
