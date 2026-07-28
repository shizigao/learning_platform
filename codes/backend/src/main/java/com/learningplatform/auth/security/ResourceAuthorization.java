/* 文件职责：表示Resource授权领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import com.learningplatform.user.domain.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("resourceAuthorization")
/**
 * 表示Resource授权领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class ResourceAuthorization {

    /** 判断是否满足OwnerOr管理条件，不修改持久化状态。 */
    public boolean isOwnerOrAdmin(Long ownerId, Authentication authentication) {
        if (ownerId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return false;
        }
        return ownerId.equals(principal.userId()) || principal.roles().contains(RoleCode.ADMIN);
    }
}
