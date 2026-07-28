/* 文件职责：集中解析认证认证主体解析器，避免各调用方重复实现协议或身份转换逻辑。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

/**
 * 集中解析认证认证主体解析器，避免各调用方重复实现协议或身份转换逻辑。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public final class AuthenticationPrincipalResolver {
    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    private AuthenticationPrincipalResolver() {
    }

    /** 校验及相关业务前置条件，不满足时抛出明确业务异常。 */
    public static AuthenticatedUserPrincipal require(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
