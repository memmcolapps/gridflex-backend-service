package org.memmcol.gridflexbackendservice.service.perm_evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.EndpointRegistry;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.service.CustomUserDetails;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ThirdPartyPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    @Autowired
    private EndpointRegistry endpointRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AntPathMatcher matcher = new AntPathMatcher();

    // These endpoint do not required permission to be accessible
//    private static final List<String> PUBLIC_ENDPOINTS = List.of(
//            "/odyssey/**"
////            "/audit-log/service/**",
////            "/dashboard/service/data-management/**"
////            "/hes/service/meter-status/stream/**"
//    );

    // These endpoints required permission at least to be true for access
    private static final List<String> GLOBAL_PERMISSION_ENDPOINTS = List.of(
            "/dashboard/service/data-management/**",
            "/audit-log/service/**",
            "/uploads/**",
//            "/hes/service/stream",
//            "/hes/service/meter-status/stream",
            "/api/realtime/**"
    );

    @Override
    public boolean checkAccess(HttpServletRequest request, Authentication authentication) {

        if (matcher.match("/hes/service/meter-status/stream", request.getRequestURI())) {
            return true;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof ThirdPartyPrincipal) {

            ThirdPartyPrincipal tp =
                    (ThirdPartyPrincipal) principal;

            String path = request.getServletPath();

            if (path.contains("/electricity/payments")
                    && tp.getScopes().contains("PAYMENT_READ")) {
                return true;
            }

            if (path.contains("/meter/readings")
                    && tp.getScopes().contains("METER_READ")) {
                return true;
            }

            return false;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

//        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserPrincipal user)) {
            return false;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();


        try {

//            boolean isPublic = PUBLIC_ENDPOINTS.stream()
//                    .anyMatch(pattern -> matcher.match(pattern, uri));
//
//            if (isPublic) {
//                System.out.println("PUBLIC ENDPOINT - ACCESS GRANTED");
//                return true;
//            }
////
            boolean isGlobalEndpoint = GLOBAL_PERMISSION_ENDPOINTS.stream()
                    .anyMatch(pattern -> matcher.match(pattern, uri));

            if (isGlobalEndpoint) {
                System.out.println("GLOBAL ENDPOINT MATCHED");

                // Only check permission (NOT module)
                return hasActionPermission(uri, method, extractPermissions(authentication));
            }

            List<Map<String, Object>> permissionTree =
                    objectMapper.readValue(user.getPermissionTreeJson(), List.class);

            for (Map<String, Object> group : permissionTree) {

                Map<String, Object> groupPermissions =
                        (Map<String, Object>) group.get("permissions");

                List<Map<String, Object>> modules =
                        (List<Map<String, Object>>) group.get("modules");

                if (modules == null) continue;

                for (Map<String, Object> module : modules) {

                    Map<String, Object> moduleInfo =
                            (Map<String, Object>) module.get("module");

                    if (moduleInfo == null) continue;

                    Boolean moduleAccess = (Boolean) moduleInfo.get("access");
                    if (!Boolean.TRUE.equals(moduleAccess)) continue;

                    List<Map<String, Object>> submodules =
                            (List<Map<String, Object>>) module.get("submodules");

                    if (submodules == null) continue;

                    for (Map<String, Object> sub : submodules) {
                        Map<String, Object> subInfo =
                                (Map<String, Object>) sub.get("submodule");

                        if (subInfo == null) continue;
//                        System.out.println("access--71: "+subInfo);
                        Boolean subAccess = (Boolean) subInfo.get("access");
                        if (!Boolean.TRUE.equals(subAccess)) continue;
                        String submoduleName = (String) subInfo.get("name");
                        String code = (String) subInfo.get("code");
                        if (submoduleName == null) continue;
                        // STEP 1: match endpoint
                        List<String> endpoints = endpointRegistry.getEndpoints(code);
//                        List<String> endpoints = endpointRegistry.getEndpoints(submoduleName);
                        System.out.println("endpoints: "+endpoints);
                        if (endpoints == null || endpoints.isEmpty()) continue;

//                        boolean match = endpoints.stream()
//                                .anyMatch(uri::startsWith);
                        boolean match = endpoints.stream()
                                .anyMatch(pattern -> matcher.match(pattern, uri));
                        if (!match) continue;
//                        if (!uri.toLowerCase().contains(
//                                submoduleName.toLowerCase().replace(" ", "-"))) {
//                            continue;
//                        }
                        // STEP 2: enforce permissions
                        if (!hasActionPermission(uri, method, groupPermissions)) {
                            return false;
                        }

                        // BOTH PASSED
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("exception: "+e);
            return false;
        }
        System.out.println("access--12");
        return false;
    }

    private Map<String, Object> extractPermissions(Authentication authentication) {
        try {
            CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();

            List<Map<String, Object>> permissionTree =
                    objectMapper.readValue(user.getPermissionTreeJson(), List.class);

            if (!permissionTree.isEmpty()) {
                return (Map<String, Object>) permissionTree.get(0).get("permissions");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private boolean hasActionPermission(String uri, String method, Map<String, Object> permissions) {

        if (permissions == null) return false;

        boolean isView = "GET".equalsIgnoreCase(method);
        boolean isWrite = List.of("POST", "PUT", "PATCH").contains(method.toUpperCase());
        boolean isDelete = "DELETE".equalsIgnoreCase(method);

        // APPROVE override (URI-based)
        boolean isApproveEndpoint = uri.toLowerCase().contains("approve");

        if (isApproveEndpoint) {
            return Boolean.TRUE.equals(permissions.get("approve"));
        }

        if (isView) {
            return Boolean.TRUE.equals(permissions.get("view"));
        }

        if (isWrite) {
            return Boolean.TRUE.equals(permissions.get("edit"));
        }

        if (isDelete) {
            return Boolean.TRUE.equals(permissions.get("disable"));
        }

        return false;
    }
}
