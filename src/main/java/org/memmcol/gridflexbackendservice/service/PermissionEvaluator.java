//package org.memmcol.gridflexbackendservice.service;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.memmcol.gridflexbackendservice.model.CustomUserPrincipal;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
//@Component("permissionEvaluator")
//public class PermissionEvaluator {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public boolean hasPermission(Authentication authentication, String submodule, String requiredPermission) {
//        Object principal = authentication.getPrincipal();
//        if (!(principal instanceof CustomUserPrincipal userPrincipal)) return false;
//
//        try {
//            String json = userPrincipal.getPermissionTreeJson();
//            List<Map<String, Object>> permissionTree = objectMapper.readValue(json, new TypeReference<>() {});
//
//            for (Map<String, Object> group : permissionTree) {
//                List<Map<String, Object>> modules = (List<Map<String, Object>>) group.get("modules");
//                for (Map<String, Object> module : modules) {
//                    List<Map<String, Object>> submodules = (List<Map<String, Object>>) module.get("submodules");
//                    for (Map<String, Object> sm : submodules) {
//                        if (submodule.equalsIgnoreCase((String) sm.get("submodule"))) {
//                            List<String> permissions = (List<String>) sm.get("permissions");
//                            return permissions.contains(requiredPermission.toLowerCase());
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }
//}
//
