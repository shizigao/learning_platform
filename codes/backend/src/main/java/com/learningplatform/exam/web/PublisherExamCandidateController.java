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
public class PublisherExamCandidateController {
    private final UserService userService;

    public PublisherExamCandidateController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
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
