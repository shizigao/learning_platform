package com.learningplatform.user.service;

import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.dto.PublicUserProfileResponse;
import com.learningplatform.user.dto.PublicUserSummaryResponse;
import com.learningplatform.user.dto.UserPublicationStatsResponse;
import com.learningplatform.user.dto.UserSearchQuery;
import org.springframework.stereotype.Service;

@Service
public class PublicUserProfileService {
    private final UserService userService;
    private final RoleService roleService;
    private final UserAvatarService avatarService;
    private final LearningContentService contentService;

    public PublicUserProfileService(
            UserService userService,
            RoleService roleService,
            UserAvatarService avatarService,
            LearningContentService contentService
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.avatarService = avatarService;
        this.contentService = contentService;
    }

    public PublicUserProfileResponse profile(Long userId) {
        User user = userService.getRequiredActiveById(userId);
        return PublicUserProfileResponse.from(
                user,
                roleService.findRoleCodesByUserId(userId),
                avatarService.avatarUrl(user),
                UserPublicationStatsResponse.from(contentService.publicationStats(userId))
        );
    }

    public PageResult<PublicUserSummaryResponse> search(UserSearchQuery query) {
        PageResult<User> users = userService.searchActive(query.getKeyword(), query);
        return PageResult.of(
                users.items().stream()
                        .map(user -> PublicUserSummaryResponse.from(
                                user,
                                avatarService.avatarUrl(user)
                        ))
                        .toList(),
                users.total(),
                users.pageNumber(),
                users.pageSize()
        );
    }

    public PageResult<ContentSummaryResponse> contents(
            Long userId,
            com.learningplatform.common.page.PageQuery query
    ) {
        userService.getRequiredActiveById(userId);
        return contentService.listPublicByPublisher(userId, query);
    }
}
