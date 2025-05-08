package org.memmcol.gridflexbackendservice.service.perm_evaluator;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface PermissionEvaluator {
//    boolean hasPermissionForPath(Map<String, Object> permissionTree, String path);

    boolean checkAccess(HttpServletRequest servletRequest, Authentication authentication);
}
