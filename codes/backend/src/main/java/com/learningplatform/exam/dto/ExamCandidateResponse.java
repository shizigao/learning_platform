package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamCandidate;
import com.learningplatform.exam.domain.ExamCandidateStatus;

import java.time.LocalDateTime;

public record ExamCandidateResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        ExamCandidateStatus status,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
    public static ExamCandidateResponse from(ExamCandidate candidate) {
        return new ExamCandidateResponse(
                candidate.getId(),
                candidate.getUserId(),
                candidate.getUsername(),
                candidate.getNickname(),
                candidate.getStatus(),
                candidate.getAssignedAt(),
                candidate.getStartedAt(),
                candidate.getSubmittedAt()
        );
    }
}
