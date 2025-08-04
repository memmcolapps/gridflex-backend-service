package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.user.CreateGroupRequest;
//import org.memmcol.gridflexbackendservice.model.user.CreateUserRequest;
import org.memmcol.gridflexbackendservice.model.user.CreateUserRequest;
import org.memmcol.gridflexbackendservice.service.user.UserService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
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

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody CreateUserRequest user) {
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
            @RequestParam(required = false, defaultValue = "") String lastActive
    ) {
        try {
            Map<String, Object> result = service.getUsers(firstname, lastname, email, permission, createdAt, lastActive, page, size);
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
    public ResponseEntity<?> changeState(@RequestParam UUID userId, @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.changeState(userId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/create/group-permission")
    public ResponseEntity<?> createGroupPermission(@RequestBody CreateGroupRequest request) {
        try {
            Map<String, Object> result = service.createGroupPermission(request);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getGroup() {
        try {
            Map<String, Object> result = service.getGroups();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
