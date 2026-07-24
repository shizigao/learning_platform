package com.learningplatform.admin.service;

import com.learningplatform.admin.dto.AdminExamDetailResponse;
import com.learningplatform.admin.dto.AdminExamListQuery;
import com.learningplatform.admin.dto.AdminExamSummaryResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.dto.ExamManagementResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.mapper.ExamMapper;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminExamService {
    private final ExamMapper examMapper;
    private final ExamService examService;
    private final UserService userService;

    public AdminExamService(
            ExamMapper examMapper,
            ExamService examService,
            UserService userService
    ) {
        this.examMapper = examMapper;
        this.examService = examService;
        this.userService = userService;
    }

    public PageResult<AdminExamSummaryResponse> list(AdminExamListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = examMapper.countForAdmin(
                query.getPublisherId(),
                query.getStatus(),
                keyword
        );
        List<AdminExamSummaryResponse> items = examMapper.findForAdmin(
                        query.getPublisherId(),
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::summary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public AdminExamDetailResponse detail(Long operatorId, Long examId) {
        ExamManagementResponse management =
                examService.detail(examId, operatorId, true);
        User publisher = userService.getRequiredById(
                management.exam().publisherId()
        );
        return new AdminExamDetailResponse(
                management,
                publisher.getUsername(),
                publisher.getNickname()
        );
    }

    private AdminExamSummaryResponse summary(Exam exam) {
        User publisher = userService.getRequiredById(exam.getPublisherId());
        return AdminExamSummaryResponse.from(
                ExamSummaryResponse.from(exam),
                publisher
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
