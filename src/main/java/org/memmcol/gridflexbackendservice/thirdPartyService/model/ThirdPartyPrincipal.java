package org.memmcol.gridflexbackendservice.thirdPartyService.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ThirdPartyPrincipal {

    private final String clientId;
    private final UUID id;
    private final UUID orgId;
    private final Boolean status;
    private final List<String> scopes;

    /**
     * Check if client has a required scope
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    /**
     * Optional helper: check multiple scopes (ANY match)
     */
    public boolean hasAnyScope(String... requiredScopes) {
        if (scopes == null) return false;

        for (String scope : requiredScopes) {
            if (scopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Optional helper: check ALL required scopes
     */
    public boolean hasAllScopes(String... requiredScopes) {
        if (scopes == null) return false;

        for (String scope : requiredScopes) {
            if (!scopes.contains(scope)) {
                return false;
            }
        }
        return true;
    }
}
