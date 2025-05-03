package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.model.UserModel;

import java.util.Map;

public interface UserService {
    Map<String, Object> createUser(UserModel user);

    Map<String, Object> updateUser(UserModel user);

    Map<String, Object> getUsers(UserModel user);

    Map<String, Object> getUser(UserModel user);

    Map<String, Object> changeState(String status);
}
