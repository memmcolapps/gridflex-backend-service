package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.model.CreateGroupRequest;
import org.memmcol.gridflexbackendservice.model.CreateUserRequest;
import org.memmcol.gridflexbackendservice.model.UserModel;

import java.util.Map;

public interface UserService {
    Map<String, Object> createUser(CreateUserRequest request);

    Map<String, Object> updateUser(UserModel user);

    Map<String, Object> getUsers(int page, int size);

    Map<String, Object> getUser(Long userId);

    Map<String, Object> changeState(String status);

    Map<String, Object> createGroupPermission(CreateGroupRequest request);

    Map<String, Object> getGroups();
}
