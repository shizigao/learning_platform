/* 文件职责：以不可变记录表示Authenticated用户认证主体数据，并作为模块内部或接口层的数据契约。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import com.learningplatform.user.domain.RoleCode;

import java.security.Principal;
import java.util.Set;

/**
 * 已认证请求在应用层使用的不可变身份快照。
 *
 * @param userId 数据库用户主键，资源权限判断应优先使用它
 * @param username 登录名，同时作为 {@link Principal#getName()} 返回值
 * @param roles 登录时从数据库加载的角色集合
 */
public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        Set<RoleCode> roles
) implements Principal {

    /** 适配 Java/Spring Security 的标准 Principal 协议。 */
    @Override
    public String getName() {
        return username;
    }
}
