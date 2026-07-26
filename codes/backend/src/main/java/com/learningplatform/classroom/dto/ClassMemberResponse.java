package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassMemberStatus;
import com.learningplatform.classroom.domain.ClassMemberView;
import com.learningplatform.classroom.domain.ClassRole;

import java.time.LocalDateTime;

public record ClassMemberResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        String avatarUrl,
        ClassRole role,
        ClassMemberStatus status,
        LocalDateTime joinedAt
) {
    public static ClassMemberResponse from(ClassMemberView member, String avatarUrl) {
        return new ClassMemberResponse(
                member.getId(),
                member.getUserId(),
                member.getUsername(),
                member.getNickname(),
                avatarUrl,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }
}
