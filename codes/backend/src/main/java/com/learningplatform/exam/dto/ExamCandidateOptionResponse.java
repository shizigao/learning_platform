package com.learningplatform.exam.dto;

import com.learningplatform.user.domain.User;

public record ExamCandidateOptionResponse(
        Long id,
        String username,
        String nickname
) {
    public static ExamCandidateOptionResponse from(User user) {
        return new ExamCandidateOptionResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
