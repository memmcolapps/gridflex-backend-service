package org.memmcol.gridflexbackendservice.model.user;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomUserPrincipal {
    private final String username;
    private final String permissionTreeJson; // Raw JSON string from the JWT
    private Set<String> allowedPaths; // optional: parsed list of allowed API paths or actions

    public CustomUserPrincipal(String username, String permissionTreeJson) {
        this.username = username;
        this.permissionTreeJson = permissionTreeJson;
        this.allowedPaths = parsePermissions(permissionTreeJson);
    }

    public String getUsername() {
        return username;
    }

    public String getPermissionTreeJson() {
        return permissionTreeJson;
    }

    public Set<String> getAllowedPaths() {
        return allowedPaths;
    }

    public boolean hasPermissionFor(String path) {
        // Simple match; improve with pattern matching if needed
        return allowedPaths.contains(path);
    }

    private Set<String> parsePermissions(String json) {
        // Example: parse from JSON to Set<String> using Jackson
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Assuming JSON looks like: { "allowedPaths": ["/service/user", "/service/device"] }
            Map<String, List<String>> parsed = mapper.readValue(json, new TypeReference<>() {});
            return new HashSet<>(parsed.getOrDefault("allowedPaths", List.of()));
        } catch (Exception e) {
            return Set.of();
        }
    }
}






//public class CustomUserPrincipal {
//    private final String username;
//    private final String permissionTreeJson;
//
//    public CustomUserPrincipal(String username, String permissionTreeJson) {
//        this.username = username;
//        this.permissionTreeJson = permissionTreeJson;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public String getPermissionTreeJson() {
//        return permissionTreeJson;
//    }
//
//    public boolean hasPermissionFor(String requestUri) {
//    }
//}

