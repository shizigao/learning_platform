/* 文件职责：提供发布者考试考生相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.dto.ExamCandidateOptionResponse;
import com.learningplatform.exam.dto.ExamCandidateSearchQuery;
import com.learningplatform.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/publisher/exam-candidates")
/**
 * 提供发布者考试考生相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherExamCandidateController {
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherExamCandidateController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ExamCandidateOptionResponse>> search(
            @RequestParam(required = false)
            @Size(max = 64, message = "考生搜索关键字不能超过64个字符")
            String keyword
    ) {
        return ApiResponse.success(userService.searchActive(keyword).stream()
                .map(ExamCandidateOptionResponse::from)
                .toList());
    }

    @GetMapping("/search")
    /** 处理 GET /search 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ExamCandidateOptionResponse>> searchPage(
            @Valid @ModelAttribute ExamCandidateSearchQuery query
    ) {
        var users = userService.searchActive(query.getKeyword(), query);
        return ApiResponse.success(PageResult.of(
                users.items().stream().map(ExamCandidateOptionResponse::from).toList(),
                users.total(),
                users.pageNumber(),
                users.pageSize()
        ));
    }
}
