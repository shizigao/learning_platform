package com.learningplatform.admin.dto;

import com.learningplatform.exam.dto.ExamManagementResponse;

public record AdminExamDetailResponse(
        ExamManagementResponse management,
        String publisherUsername,
        String publisherNickname
) {
}
