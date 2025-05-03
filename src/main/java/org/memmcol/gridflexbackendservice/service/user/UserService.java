package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.model.UserModel;

import java.util.Map;

public interface UserService {
    Map<String, Object> createUser(UserModel user);

    Map<String, Object> updateUser(UserModel user);

    Map<String, Object> getUsers(int page, int size);

    Map<String, Object> getUser(int userId);

    Map<String, Object> changeState(String status);
}
