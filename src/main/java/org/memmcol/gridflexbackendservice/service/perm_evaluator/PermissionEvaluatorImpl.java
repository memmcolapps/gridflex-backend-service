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

    private static final Map<String, List<String>> SUBMODULE_URI_MAP = Map.ofEntries(

            Map.entry("full access", List.of("/band/service/create", "/band/service/update", "/band/service/change-state",
                    "/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/all", "/tariff/service/export",
                    "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve","/user/service/single", "/user/service/update/group-permission",
                    "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create", "/user/service/group/change-state", "/user/service/group/update",
                    "/user/service/groups",  "/user/service/create/group-permission", "/customer/service/create", "/customer/service/update", "/customer/service/download/template/csv",
                    "/customer/service/change-state", "/customer/service/all",  "/customer/service/single", "/customer/service/bulk-upload", "/customer/service/download/template/excel",
                    "/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center", "/node/service/businessHub",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
                    "/node/service/single", "/node/service/all", "/manufacturer/service/create", "/manufacturer/service/update", "/manufacturer/service/single",
                    "/manufacturer/service/all", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report","/audit-log/service/incident/report/get",
                    "/debit-credit-adjustment/service/create", "/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept",
                    "/debit-credit-adjustment/service/all", "/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
                    "/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve", "/debt-setting/service/percentage-range/create",
                    "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve",
                    "/debt-setting/service/percentage-range/bulk-approve", "/debt-setting/service/liability-cause/bulk-approve",
                    "/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "/meter/service/change-state", "/meter/service/approve", "/meter/service/download/assign/template/csv",
                    "/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
                    "/meter/service/download/template/excel", "/meter/service/download/allocate/template/excel", "/meter/service/download/allocate/template/csv", "/meter/service/bulk-approve",
                    "/meter/service/download/template/csv", "meter/service/bulk-upload", "/meter/service/virtual/export", "/meter/service/export", "/meter/service/download/assign/template/excel",
                    "/meter/service/migrate", "/meter/service/manufacturers", "/meter/service/assign", "/meter/service/cin/assign", "meter/service/customer", "/meter/reading/service/download/template/csv",
                    "/meter/reading/service/download/template/excel","/meter/reading/service/bulk-upload", "meter/service/allocate", "meter/service/detach", "/meter/reading/service/create",
                    "/meter/reading/service/generate", "/meter/reading/service/update", "/meter/reading/service/all", "/vending/service/generate/token/credit", "/vending/service/generate/token/credit/calculate",
                    "/vending/service/generate/kct", "/vending/service/generate/token/kct-clear-tamper", "/vending/service/generate/token/clear-credit", "/vending/service/generate/token/clear-tamper",
                    "/vending/service/generate/token/compensation", "/vending/service/generate/token/all", "/dashboard/service/data-management", "/vending/service/generate/meter-kct",
                    "/billing/service/meter/reading/create", "/billing/service/meter/reading/generate","/billing/service/meter/reading/update", "/billing/service/meter/reading/all", "/dashboard/service/billing",
                    "/billing/service/meter/reading/download/template/csv", "/billing/service/meter/reading/download/template/excel", "/billing/service/meter/reading/bulk-upload",
                    "/vending/service/generate/token/print", "/dashboard/service/vending", "/dashboard/service/hes", "/hes/service/communication/report", "/hes/service/event", "/hes/service/profile",
                    "/hes/service/model", "/hes/service/communication/range/report", "/hes/service/meter-status/stream", "/hes/service/stream", "/hes/service/data/schedule")),

            Map.entry("data management", List.of("/band/service/create", "/band/service/update", "/band/service/change-state", "/user/service/update",
                    "/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/all", "/tariff/service/export",
                    "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve", "/customer/service/create",
                    "/customer/service/update", "/customer/service/change-state", "/customer/service/all",  "/customer/service/single-customer",
                    "/customer/service/bulk-upload", "/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line", "/node/service/businessHub",
                    "/node/service/single", "/node/service/all", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report",
                    "/audit-log/service/incident/report/get", "/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single",
                    "/meter/service/change-state", "/meter/service/approve", "/meter/service/migrate", "/meter/service/assign", "meter/service/bulk-upload", "/meter/service/bulk-approve",
                    "/meter/service/cin/assign", "meter/service/customer", "meter/service/allocate", "meter/service/detach", "/meter/service/download/assign/template/excel",
                    "/meter/service/download/assign/template/csv", "/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
                    "/meter/service/download/allocate/template/excel", "/meter/service/download/allocate/template/csv", "/meter/service/virtual/export", "/meter/service/export",
                    "/manufacturer/service/create", "/manufacturer/service/update", "/manufacturer/service/single", "/manufacturer/service/all",
                    "/meter/service/download/template/excel", "/meter/service/download/template/csv", "/dashboard/service/data-management",
                    "/debit-credit-adjustment/service/create", "/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept",
                    "/debit-credit-adjustment/service/all", "/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
                    "/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve", "/debt-setting/service/percentage-range/create",
                    "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve",
                    "/debt-setting/service/percentage-range/bulk-approve", "/debt-setting/service/liability-cause/bulk-approve")),

            Map.entry("organization", List.of("/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
                    "/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line", "/user/service/update",
                    "/node/service/single", "/node/service/all", "/audit-log/service/all", "/node/service/businessHub", "/audit-log/service/single-log",
                    "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("meter manufacturers", List.of(
                    "/manufacturer/service/create", "/manufacturer/service/update", "/manufacturer/service/single", "/manufacturer/service/all", "/dashboard/service/data-management",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("meter management", List.of("/tariff/service/all", "/node/service/single", "/node/service/all", "/dashboard/service/data-management",
                    "/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "/user/service/update",
                    "/meter/service/change-state", "/meter/service/approve", "/meter/service/migrate", "/meter/service/assign", "/manufacturer/service/create",
                    "/manufacturer/service/update", "/manufacturer/service/single", "/manufacturer/service/all", "meter/service/bulk-upload",
                    "/meter/service/cin/assign", "meter/service/customer", "meter/service/allocate", "meter/service/detach", "/meter/service/download/allocate/template/excel",
                    "/meter/service/download/allocate/template/csv", "/meter/service/virtual/export", "/meter/service/export", "/meter/service/download/assign/template/excel",
                    "/meter/service/download/assign/template/csv", "/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
                    "/meter/service/download/template/excel", "/meter/service/download/template/csv", "/dashboard/service/data-management", "/audit-log/service/all",
                    "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("meter inventory", List.of("/node/service/single", "/node/service/all", "/dashboard/service/data-management",
                    "/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "meter/service/allocate",
                    "/meter/service/download/template/excel", "/meter/service/download/template/csv", "meter/service/bulk-upload", "/meter/service/virtual/export", "/meter/service/export",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("assigned meter", List.of("/node/service/single", "/node/service/all", "/meter/service/migrate", "/meter/service/detach", "meter/service/bulk-upload",
                    "/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "/dashboard/service/data-management", "/meter/service/download/assign/template/excel",
                    "/meter/service/download/assign/template/csv", "/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
                    "meter/service/allocate", "/audit-log/service/single-log", "/audit-log/service/all", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("meter", List.of("/tariff/service/all", "/node/service/single", "/node/service/all", "/meter/service/create", "/dashboard/service/data-management",
                    "/meter/service/download/allocate/template/excel", "/meter/service/download/allocate/template/csv", "/meter/service/virtual/export", "/meter/service/export",
                    "/meter/service/download/assign/template/excel", "/meter/service/download/assign/template/csv", "/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
                    "/meter/service/update", "/meter/service/all", "/meter/service/single", "/meter/service/assign", "/meter/service/cin/assign", "/meter/service/cin/change-state",
                    "meter/service/allocate", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("review and approval", List.of("/band/service/all", "/band/service/single", "/band/service/approve", "/meter/service/all",
                    "/meter/service/single", "/meter/service/approve", "/tariff/service/single", "/tariff/service/all", "/tariff/service/approve", "/meter/service/bulk-approve",
                    "/debt-setting/service/liability-cause/all", "/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/approve",
                    "/debt-setting/service/liability-cause/approve", "/dashboard/service/data-management", "/user/service/update",  "/debt-setting/service/percentage-range/bulk-approve",
                    "/debt-setting/service/liability-cause/bulk-approve",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("customer management", List.of("/customer/service/create", "/customer/service/update", "/customer/service/change-state", "/customer/service/all",
                    "/customer/service/download/template/excel", "/customer/service/download/template/csv", "/dashboard/service/data-management", "/user/service/update",
                    "/customer/service/single", "/customer/service/bulk-upload", "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report",
                    "/audit-log/service/incident/report/get")),

            Map.entry("debt management", List.of("/debit-credit-adjustment/service/create", "/dashboard/service/data-management", "/user/service/update",
                    "/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept", "/debit-credit-adjustment/service/all",
                    "/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
                    "/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve",
                    "/debt-setting/service/percentage-range/create", "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/all",
                    "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve", "/audit-log/service/all", "/audit-log/service/single-log",
                    "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("band management", List.of("/band/service/create", "/band/service/update", "/band/service/change-state", "/band/service/all",  "/band/service/single",
                    "/dashboard/service/data-management","/user/service/update",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("tariff", List.of("/tariff/service/single", "/tariff/service/all", "/tariff/service/create", "/tariff/service/change-state", "/dashboard/service/data-management",
                    "/audit-log/service/all", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get", "/user/service/update",
                    "/tariff/service/export")),

            Map.entry("dashboard", List.of("/dashboard/service/data-management")),

            Map.entry("user management", List.of(
                    "/user/service/single", "/user/service/all", "/user/service/change-state", "/user/service/group/update",  "/user/service/create", "/user/service/update",
                    "/user/service/groups", "/user/service/create/group-permission", "/user/service/update/group-permission", "/node/service/create/node/region-bhub-service-center",
                    "/user/service/group/change-state", "/node/service/update/node/region-bhub-service-center", "/node/service/create/node/substation-transformer-feeder-line", "/dashboard/service/data-management",
                    "/node/service/businessHub","/node/service/update/node/substation-transformer-feeder-line", "/node/service/single", "/node/service/all", "/audit-log/service/all",
                    "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("users", List.of(
                    "/user/service/single-user", "/user/service/all", "/user/service/change-state", "/user/service/group/update", "/user/service/create", "/user/service/update",
                    "/user/service/groups", "/user/service/create/group-permission", "/audit-log/service/all", "/audit-log/service/single-log", "/user/service/group/update",
                    "/user/service/group/change-state", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get", "/dashboard/service/data-management")),

            Map.entry("group permission", List.of("/user/service/create/group-permission", "/user/service/update/group-permission",
                    "/user/service/group/change-state", "/user/service/groups", "/dashboard/service/data-management",
                    "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("vending", List.of("/dashboard/service/vending", "/vending/service/generate/token/credit", "/vending/service/generate/token/credit/calculate", "/vending/service/generate/kct",
                    "/vending/service/generate/token/kct-clear-tamper", "/vending/service/generate/token/clear-credit", "/vending/service/generate/token/clear-tamper",
                    "/vending/service/generate/token/compensation",  "/vending/service/generate/meter-kct", "/vending/service/generate/token/all", "/vending/service/generate/token/print",
                    "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("billing", List.of( "/billing/service/meter/reading/create", "/billing/service/meter/reading/generate","/billing/service/meter/reading/update",
                    "/billing/service/meter/reading/all", "/dashboard/service/billing", "/billing/service/meter/reading/service/download/template/csv", "/billing/service/meter/reading/download/template/excel",
                    "/billing/service/meter/reading/bulk-upload", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get")),

            Map.entry("hes", List.of("/dashboard/service/hes-management", "/audit-log/service/single-log", "/audit-log/service/incident/report", "/audit-log/service/incident/report/get",
                    "/dashboard/service/hes", "/hes/service/communication/report", "/hes/service/event", "/hes/service/profile", "/hes/service/model", "/hes/service/communication/range/report",
                    "/hes/service/meter-status/stream", "/hes/service/stream", "/hes/service/data/schedule"))

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