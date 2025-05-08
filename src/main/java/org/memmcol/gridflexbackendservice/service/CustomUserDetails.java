package org.memmcol.gridflexbackendservice.service;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class CustomUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String permissionTreeJson;
    private final Set<String> allowedPaths;

    public CustomUserDetails(String username, String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String permissionTreeJson) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.permissionTreeJson = permissionTreeJson;
        this.allowedPaths = parsePermissions(permissionTreeJson);
    }

    public String getPermissionTreeJson() {
        return permissionTreeJson;
    }

    public Set<String> getAllowedPaths() {
        return allowedPaths;
    }

    public boolean hasPermissionFor(String path) {
        return allowedPaths.contains(path);
    }

    private Set<String> parsePermissions(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Adjust this structure if needed
            List<Map<String, Object>> parsed = mapper.readValue(json, new TypeReference<>() {});
            Set<String> paths = new HashSet<>();
            for (Map<String, Object> group : parsed) {
                List<Map<String, Object>> modules = (List<Map<String, Object>>) group.get("modules");
                if (modules == null) continue;
                for (Map<String, Object> module : modules) {
                    List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("submodules");
                    if (submodules == null) continue;
                    for (Map<String, Object> submodule : submodules) {
                        String submoduleName = (String) submodule.get("submodule");
                        paths.add("/" + submoduleName); // Adjust as needed
                    }
                }
            }
            return paths;
        } catch (Exception e) {
            return Set.of();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

}



//public class CustomUserDetails extends org.springframework.security.core.userdetails.User {
//
//    private final String email;
//    private final String permissionsJson;
//
//
//    public CustomUserDetails(String email, String password,
//                             Collection<? extends GrantedAuthority> authorities,
//                             String permissionsJson) {
//        super(email, password, authorities);
//        this.email = email;
//        this.permissionsJson = permissionsJson;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public String getPermissionsJson() {
//        return permissionsJson;
//    }
//}
