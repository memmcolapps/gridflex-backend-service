package org.memmcol.gridflexbackendservice.controller;

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
    public ResponseEntity<?> createUser(@RequestBody UserModel user) {
        try {
            Map<String, Object> result = service.createUser(user);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserModel user) {
        try {
            Map<String, Object> result = service.updateUser(user);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/all-users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "page", required = false) int page,
            @RequestParam(value = "size", required = false) int size
    ) {
        try {
            Map<String, Object> result = service.getUsers(page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-user")
    public ResponseEntity<?> getUser(@RequestParam int userId) {
        try {
            Map<String, Object> result = service.getUser(userId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(@RequestParam String status) {
        try {
            Map<String, Object> result = service.changeState(status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
