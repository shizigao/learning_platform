/* 文件职责：提供错题题目复习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.web;

import com.learningplatform.ai.dto.WrongQuestionAnalysisGenerateRequest;
import com.learningplatform.ai.dto.WrongQuestionAnalysisResponse;
import com.learningplatform.ai.dto.WrongQuestionReviewPageResponse;
import com.learningplatform.ai.service.WrongQuestionReviewService;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams/wrong-review")
/**
 * 提供错题题目复习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class WrongQuestionReviewController {
    /** 委托复习执行对应领域规则。 */
    private final WrongQuestionReviewService reviewService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public WrongQuestionReviewController(
            WrongQuestionReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<WrongQuestionReviewPageResponse> page(
            Authentication authentication
    ) {
        return ApiResponse.success(reviewService.page(
                AuthenticationPrincipalResolver.require(authentication).userId()
        ));
    }

    @PostMapping("/analysis")
    /** 处理 POST /analysis 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<WrongQuestionAnalysisResponse> analyze(
            Authentication authentication,
            @Valid @RequestBody WrongQuestionAnalysisGenerateRequest request
    ) {
        return ApiResponse.success(reviewService.generate(
                AuthenticationPrincipalResolver.require(authentication).userId(),
                request.requestId()
        ));
    }
}
