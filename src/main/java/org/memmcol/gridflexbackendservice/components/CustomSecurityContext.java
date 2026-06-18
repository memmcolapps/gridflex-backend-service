package org.memmcol.gridflexbackendservice.components;

import org.memmcol.gridflexbackendservice.model.user.UserPermissionResponse;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ThirdPartyPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CustomSecurityContext {
    public UserPermissionResponse getPrincipal() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof ThirdPartyPrincipal)) {
            throw new AccessDeniedException("Invalid authentication context");
        }

        return (UserPermissionResponse) authentication.getPrincipal();
    }
}
