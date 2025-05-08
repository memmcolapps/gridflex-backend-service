package org.memmcol.gridflexbackendservice.service.perm_evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.model.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private static final Map<String, List<String>> SUBMODULE_URI_MAP = Map.of(

            "Full Access", List.of("/band/service/create", "band/service/update", "band/service/change-state",
                    "band/service/all-band",  "band/service/single-band", "tariff/service/single-tariff", "tariff/service/all-tariff",
                    "tariff/service/create", "tariff/service/change-state", "tariff/service/bulk-approve","/user/service/single-user",
                    "/user/service/all-users", "/user/service/change-state", "/user/service/update",  "/user/service/create",
                    "/user/service/groups",  "/user/service/create/group-permission"),

            "Data Management", List.of("/band/service/create", "band/service/update", "band/service/change-state",
                    "band/service/all-band",  "band/service/single-band", "tariff/service/single-tariff", "tariff/service/all-tariff",
                    "tariff/service/create", "tariff/service/change-state", "tariff/service/bulk-approve"),

            "Band Management", List.of("/band/service/create", "band/service/update", "band/service/change-state",
                    "band/service/all-band",  "band/service/single-band"),

            "Tariff", List.of("tariff/service/single-tariff", "tariff/service/all-tariff",
                    "tariff/service/create", "tariff/service/change-state", "tariff/service/bulk-approve"),

            "User Management", List.of("/user/service/single-user", "/user/service/all-users",
                    "/user/service/change-state", "/user/service/update",  "/user/service/create",
                    "/user/service/groups",  "/user/service/create/group-permission"),

            "Users", List.of("/user/service/single-user", "/user/service/all-users",
                    "/user/service/change-state", "/user/service/update",  "/user/service/create",
                    "/user/service/groups",  "/user/service/create/group-permission")
    );

//            "User List", List.of("/user/service/single-user", "/user/service/all-users"),
//            "Role Assignment", List.of("/roles/assign", "/roles/view"),

    @Override
    public boolean checkAccess(HttpServletRequest request, Authentication authentication) {
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            // Fetch the principal and check its type
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof CustomUserPrincipal)) {
                throw new IllegalStateException("Expected CustomUserPrincipal but got " + principal.getClass().getName());
            }

            CustomUserPrincipal customUserPrincipal = (CustomUserPrincipal) principal;

            // Get the permission tree (this is a JSON string from the JWT)
            String permissionTreeJson = customUserPrincipal.getPermissionTreeJson();
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> permissionTree;

            try {
                // Parse the permission tree JSON
                permissionTree = objectMapper.readValue(permissionTreeJson, List.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return false; // Could not parse permission tree
            }
            String requestUri = request.getRequestURI();
            for (Map<String, Object> group : permissionTree) {
                List<Map<String, Object>> modules = (List<Map<String, Object>>) group.get("modules");
                if (modules == null) continue;

                for (Map<String, Object> module : modules) {
                    List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("submodules");
                    String moduleName = (String) module.get("module");
                    if (submodules == null) continue;

                    for (Map<String, Object> submodule : submodules) {
                        String submoduleName = (String) submodule.get("submodule");
                        List<String> permissions = (List<String>) submodule.get("permissions");

                        if (submoduleName == null || permissions == null) continue;
                        System.out.println("module: " + moduleName);
                        System.out.println("submoduleName: " + submoduleName);
                        System.out.println("permissions: " + permissions);
                        System.out.println("requestUri: " + requestUri);

                        List<String> mappedUris = SUBMODULE_URI_MAP.get(moduleName);
                        if (mappedUris != null && mappedUris.stream().anyMatch(requestUri::contains)) {
                            for (String perm : permissions) {
                                if (List.of("view", "edit", "approve", "disable").contains(perm)) {
                                    System.out.println("Matched permission: " + perm);
                                    // Do something specific based on matched permission
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false; // No matching permission found
    }
}




//                        if (requestUri != null && requestUri.contains(submoduleName) && permissions.contains("view")) {
//                            System.out.println("permissions>>>>>> : " + submodule.get("permissions"));
//                            return true;
//                        }

//@Component
//public class PermissionEvaluatorImpl implements PermissionEvaluator {
//
//    @Override
//    public boolean checkAccess(HttpServletRequest request, Authentication authentication) {
//        try {
//            System.out.println(">>>>>>>>>>>> checkAccess1");
//            if (authentication == null || !authentication.isAuthenticated()) {
//                return false;
//            }
//            System.out.println(">>>>>>>>>>>> checkAccess2:: "+authentication.getPrincipal());
//            String requestUri = request.getRequestURI();
////            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
//            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
//                System.out.println(">>>>>>>>>>>> Principal is not CustomUserDetails: " + authentication.getPrincipal().getClass());
//                return false;
//            }
//            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
//            System.out.println(">>>>>>>>>>>> checkAccess3");
//            // Get the user's permission tree (from JWT)
//            String permissionTreeJson = principal.getPermissionTreeJson();  // This should be the JSON string from JWT
//
//            // Convert the permission tree JSON to a Map or a List for easier processing
//            ObjectMapper objectMapper = new ObjectMapper();
//            List<Map<String, Object>> permissionTree;
//            try {
//                permissionTree = objectMapper.readValue(permissionTreeJson, List.class);
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//                return false; // Could not parse permission tree
//            }
//
//            // Check the permissions against the request URI
//            for (Map<String, Object> module : permissionTree) {
//                List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("modules");
//                for (Map<String, Object> submodule : submodules) {
//                    List<String> permissions = (List<String>) submodule.get("permissions");
//                    String submoduleName = (String) submodule.get("submodule");
//
//                    // Here, you will need a way to match the request URI to a submodule
//                    // For example, check if the request URI corresponds to a submodule, and if the user has permission
//                    System.out.println("submoduleName: " + submoduleName);
//                    if (requestUri.contains(submoduleName) && permissions.contains("axe")) {
//                        return true; // User has permission to access this submodule
//                    }
//                }
//            }
//
//            // If no matching permission found
//            return false;
//        } catch (Exception e) {
//            System.out.println(">>>>>>>>>>>> Exception" + e.getMessage());
//        }
//        return false;
//    }
//}


///

//@Component
//public class PermissionEvaluatorImpl implements PermissionEvaluator {
//
//    @Override
//    public boolean checkAccess(HttpServletRequest request, Authentication authentication) {
//            System.out.println(">>>>>>>>>>>> checkAccess1");
//
//            if (authentication == null || !authentication.isAuthenticated()) {
//                return false;
//            }
//
//            System.out.println(">>>>>>>>>>>> checkAccess2");
//
//            Object principalObj = authentication.getPrincipal();
//            if (!(principalObj instanceof CustomUserPrincipal)) {
//                System.out.println(">>>>>>>>>>>> Principal is not CustomUserPrincipal: " + principalObj.getClass());
//                return false;
//            }
//
//            CustomUserPrincipal principal = (CustomUserPrincipal) principalObj;
//            System.out.println(">>>>>>>>>>>> checkAccess3");
//
//            String requestUri = request.getRequestURI();
//
//            String permissionTreeJson = principal.getPermissionTreeJson(); // JSON from JWT or DB
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            List<Map<String, Object>> permissionTree;
//            try {
//                permissionTree = objectMapper.readValue(permissionTreeJson, List.class);
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//                return false;
//            }
//
//            for (Map<String, Object> module : permissionTree) {
//                List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("modules");
//
//                for (Map<String, Object> submodule : submodules) {
//                    List<String> permissions = (List<String>) submodule.get("permissions");
//                    String submoduleName = (String) submodule.get("submodule");
//
//                    System.out.println("submoduleName: " + submoduleName);
//
//                    if (requestUri.contains(submoduleName) && permissions.contains("axe")) {
//                        return true;
//                    }
//                }
//            }
//        return false;
//    }
//}





/// ----------------------------


//@Component
//public class PermissionEvaluatorImpl implements PermissionEvaluator {
//
//    @Override
//    public boolean checkAccess(HttpServletRequest request, Authentication authentication) {
//        // Example: check if user has a permission that matches request path
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return false;
//        }
//
//        String requestUri = request.getRequestURI();
//        // Example: Get custom permissions from your user principal
//        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
//        return principal.hasPermissionFor(requestUri);
//    }
//}

//    @Override
//    public boolean hasPermissionForPath(Map<String, Object> permissionTree, String path) {
//        // Logic to traverse permissionTree and check if path is allowed based on permissions
//        // Example: Check if the path exists in the permission tree and the user has the required permission
//        return permissionTree.containsKey(path);  // Simplified example
//    }