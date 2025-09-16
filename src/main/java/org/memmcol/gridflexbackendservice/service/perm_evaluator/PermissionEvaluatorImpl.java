package org.memmcol.gridflexbackendservice.service.perm_evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private static final Map<String, List<String>> SUBMODULE_URI_MAP = Map.of(

            "full access", List.of("/band/service/create", "/band/service/update", "/band/service/change-state",
                    "/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/all",
                    "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve","/user/service/single",
                    "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create", "/user/service/group/change-state",
                    "/user/service/groups",  "/user/service/create/group-permission", "/customer/service/create", "/customer/service/update",
                    "/customer/service/change-state", "/customer/service/all",  "/customer/service/single", "/customer/service/bulk-upload",
                    "/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
                    "/node/service/single", "/node/service/all", "/manufacturer/service/create", "/manufacturer/service/update",
                    "/manufacturer/service/single", "/manufacturer/service/all", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report",
                    "/debit-credit-adjustment/service/create", "/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept",
                    "/debit-credit-adjustment/service/all", "/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create",
                    "/debt-setting/service/liability-cause/update", "/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single",
                    "/debt-setting/service/liability-cause/approve", "/debt-setting/service/percentage-range/create", "/debt-setting/service/percentage-range/update",
                    "/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve"),

            "data management", List.of("/band/service/create", "/band/service/update", "/band/service/change-state",
                    "/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/all",
                    "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve", "/customer/service/create",
                    "/customer/service/update", "/customer/service/change-state", "/customer/service/all",  "/customer/service/single-customer",
                    "/customer/service/bulk-upload", "/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
                    "/node/service/single", "/node/service/all", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "customer management", List.of("/customer/service/create", "/customer/service/update", "/customer/service/change-state", "/customer/service/all",
                    "/customer/service/single", "/customer/service/bulk-upload", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "debt management", List.of("/debit-credit-adjustment/service/create",
                    "/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept", "/debit-credit-adjustment/service/all",
                    "/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
                    "/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve",
                    "/debt-setting/service/percentage-range/create", "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/all",
                    "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve", "/audit-log/service/all", "/audit-log/service/single-log",
                    "/audit-log/service/incident/report"),

            "organization", List.of("/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
                    "/node/service/single", "/node/service/all", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "band management", List.of("/band/service/create", "/band/service/update", "/band/service/change-state", "/band/service/all",  "/band/service/single",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "tariff", List.of("/tariff/service/single", "/tariff/service/all", "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "user management", List.of("/user/service/single", "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create",
                    "/user/service/groups", "/user/service/create/group-permission", "/node/service/create/node/region-bhub-service-center", "/user/service/group/change-state",
                    "/node/service/update/node/region-bhub-service-center", "/node/service/create/node/substation-transformer-feeder-line",
                    "/node/service/update/node/substation-transformer-feeder-line", "/node/service/single", "/node/service/all", "/audit-log/service/all",
                    "/audit-log/service/single-log", "/audit-log/service/incident/report"),

            "users", List.of("/user/service/single-user", "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create",
                    "/user/service/groups", "/user/service/create/group-permission", "/audit-log/service/all", "/audit-log/service/single-log",
                    "/user/service/group/change-state", "/audit-log/service/incident/report")
    );

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
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 11");
            // Get the permission tree (this is a JSON string from the JWT)
            String permissionTreeJson = customUserPrincipal.getPermissionTreeJson();
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> permissionTree;
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 11");
            try {
                // Parse the permission tree JSON
                permissionTree = objectMapper.readValue(permissionTreeJson, List.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return false; // Could not parse permission tree
            }
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12");
            String requestUri = request.getRequestURI();
            for (Map<String, Object> group : permissionTree) {
                List<Map<String, Object>> modules = (List<Map<String, Object>>) group.get("modules");
                Map<String, Object> groupPermissions = (Map<String, Object>) group.get("permissions");

                if (modules == null || groupPermissions == null) continue;
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12"+permi);
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12:::: "+modules);
                for (Map<String, Object> module : modules) {
                    Map<String, Object> moduleInfo = (Map<String, Object>) module.get("module");
                    if (moduleInfo == null || !Boolean.TRUE.equals(moduleInfo.get("access"))) continue;

                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13"+module);

                    List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("submodules");
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13::"+submodules);
                    String moduleName = (String) moduleInfo.get("name");

                    if (submodules == null) continue;
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13");

                    for (Map<String, Object> submodule : submodules) {
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13+++::"+submodule);
                        Map<String, Object> submoduleInfo = (Map<String, Object>) submodule.get("submodule");
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 14::"+submoduleInfo);
                        if (submoduleInfo == null || !Boolean.TRUE.equals(submoduleInfo.get("access"))) continue;
                        String submoduleName = (String) submoduleInfo.get("name");

//                        String submoduleName = (String) submodule.get("submodule");
//                        List<String> permissions = (List<String>) submodule.get("permissions");

                        if (submoduleName == null) continue;
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> 15");
                        System.out.println("module: " + moduleName);
                        System.out.println("submoduleName: " + submoduleName);
                        System.out.println("permissions: " + groupPermissions.keySet());
                        System.out.println("requestUri: " + requestUri);
//                        String normalizedSubmoduleName = submoduleName.replaceAll("\\s+", "").toLowerCase();
                        List<String> mappedUris = SUBMODULE_URI_MAP.get(submoduleName.toLowerCase());
                        if (mappedUris != null && mappedUris.stream().anyMatch(requestUri::contains)) {
                            for (String perm : groupPermissions.keySet()) {
                                if (List.of("view", "edit", "approve", "disable")
                                        .stream().anyMatch(p -> p.equalsIgnoreCase(perm))) {

                                    System.out.println("Matched permission: " + perm);
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