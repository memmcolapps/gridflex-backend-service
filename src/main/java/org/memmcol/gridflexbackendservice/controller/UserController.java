package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.CreateGroupRequest;
import org.memmcol.gridflexbackendservice.model.CreateUserRequest;
import org.memmcol.gridflexbackendservice.model.UserModel;
import org.memmcol.gridflexbackendservice.service.user.UserService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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


    @GetMapping("/all-users")
    public ResponseEntity<?> getUsers(
//            @RequestParam(value = "page", required = false) int page,
//            @RequestParam(value = "size", required = false) int size
            @RequestParam(value = "email", required = false, defaultValue = "") String email,
            @RequestParam(value = "permission", required = false, defaultValue = "") String permission,
            @RequestParam(value = "dateAdded", required = false, defaultValue = "") String dateAdded,
            @RequestParam(value = "lastActive", required = false, defaultValue = "") Boolean lastActive
    ) {
        try {
            Map<String, Object> result = service.getUsers(email, permission, dateAdded, lastActive);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-user")
    public ResponseEntity<?> getUser(@RequestParam Long userId) {
        try {
            Map<String, Object> result = service.getUser(userId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(@RequestParam Long userId, @RequestParam Boolean status) {
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

//    @PostMapping("/")
//    public ResponseEntity<?> createPermission(@RequestBody UserModel user) {
//        try {
//            Map<String, Object> result = service.createUser(user);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
