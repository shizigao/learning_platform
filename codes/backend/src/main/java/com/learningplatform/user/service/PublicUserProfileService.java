/* 文件职责：实现Public用户资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现Public用户资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class PublicUserProfileService {
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 委托角色执行对应领域规则。 */
    private final RoleService roleService;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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

    /** 执行 profile 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public PublicUserProfileResponse profile(Long userId) {
        User user = userService.getRequiredActiveById(userId);
        return PublicUserProfileResponse.from(
                user,
                roleService.findRoleCodesByUserId(userId),
                avatarService.avatarUrl(user),
                UserPublicationStatsResponse.from(contentService.publicationStats(userId))
        );
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 执行 contents 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public PageResult<ContentSummaryResponse> contents(
            Long userId,
            com.learningplatform.common.page.PageQuery query
    ) {
        userService.getRequiredActiveById(userId);
        return contentService.listPublicByPublisher(userId, query);
    }
}
