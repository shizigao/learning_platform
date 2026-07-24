package com.learningplatform.auth.security;

import com.learningplatform.user.domain.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("resourceAuthorization")
public class ResourceAuthorization {

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
