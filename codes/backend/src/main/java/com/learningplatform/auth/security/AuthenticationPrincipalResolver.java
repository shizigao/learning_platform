package com.learningplatform.auth.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

public final class AuthenticationPrincipalResolver {
    private AuthenticationPrincipalResolver() {
    }

    public static AuthenticatedUserPrincipal require(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
