package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.model.CreateGroupRequest;
import org.memmcol.gridflexbackendservice.model.CreateUserRequest;

import java.time.LocalDateTime;
import java.util.Map;

public interface UserService {
    Map<String, Object> createUser(CreateUserRequest request);

    Map<String, Object> updateUser(CreateUserRequest user);

    Map<String, Object> getUser(Long userId);

    Map<String, Object> changeState(Long userId, Boolean status);

    Map<String, Object> createGroupPermission(CreateGroupRequest request);

    Map<String, Object> getGroups();

    Map<String, Object> getUsers(String firstname, String lastname, String email, String permission, String dateAdded, String lastActive, int page, int size);
}
