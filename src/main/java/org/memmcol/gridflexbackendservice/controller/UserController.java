package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.user.CreateGroupRequest;
//import org.memmcol.gridflexbackendservice.model.user.CreateUserRequest;
import org.memmcol.gridflexbackendservice.model.user.CreateUserRequest;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.service.user.UserService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user/service")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            Map<String, Object> result = service.createUser(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    // Update user group by assigning the user to another group
    @PutMapping("/group/update")
    public ResponseEntity<?> updateGroupUser(@RequestBody CreateUserRequest request) {
        try {
            Map<String, Object> result = service.updateUserGroup(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    // Update User profile
    @PutMapping("/update")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserModel user) {
        try {
            Map<String, Object> result = service.updateUser(user);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

//    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'User List', 'view')")
    @GetMapping("/all")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "firstname", required = false, defaultValue = "") String firstname,
            @RequestParam(value = "lastname", required = false, defaultValue = "") String lastname,
            @RequestParam(value = "email", required = false, defaultValue = "") String email,
            @RequestParam(value = "permission", required = false, defaultValue = "") String permission,
            @RequestParam(value = "createdAt", required = false, defaultValue = "") String createdAt,
            @RequestParam(required = false, defaultValue = "") String lastActive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        try {
            Map<String, Object> result = service.getUsers(
                    firstname, lastname, email, permission, createdAt, lastActive,
                    search, status, sortDirection, page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getUser(@RequestParam UUID userId) {
        try {
            Map<String, Object> result = service.getUser(userId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(@RequestBody UserModel request) {
        try {
            Map<String, Object> result = service.changeState(request.getId(), request.getStatus());
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    //Create group permission, adding modules and submodules
    @PostMapping("/create/group-permission")
    public ResponseEntity<?> createGroupPermission(@RequestBody CreateGroupRequest request) {
        try {
            Map<String, Object> result = service.createGroupPermission(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    //Update group permission, adding or removing module and submodule
    @PutMapping("/update/group-permission")
    public ResponseEntity<?> updateGroupPermission(@RequestBody CreateGroupRequest createGroupRequest) {
        try {
            Map<String, Object> result = service.updateGroupPermission(createGroupRequest);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getGroup(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection) {
        try {
            Map<String, Object> result = service.getGroups(search, status, sortDirection);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/group/change-state")
    public ResponseEntity<?> changeGroupPermissionState(@RequestParam UUID groupId, @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.changeGroupPermissionStatus(groupId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/module-access")
    public ResponseEntity<?> getModule() {
        try {
            Map<String, Object> result = service.getOrgModule();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
