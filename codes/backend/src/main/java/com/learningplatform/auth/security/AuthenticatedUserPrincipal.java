package com.learningplatform.auth.security;

import com.learningplatform.user.domain.RoleCode;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        Set<RoleCode> roles
) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
