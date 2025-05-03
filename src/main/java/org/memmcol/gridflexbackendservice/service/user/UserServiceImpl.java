package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.model.UserModel;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements  UserService {
    @Override
    public Map<String, Object> createUser(UserModel user) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateUser(UserModel user) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getUsers(UserModel user) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getUser(UserModel user) {
        return Map.of();
    }

    @Override
    public Map<String, Object> changeState(String status) {
        return Map.of();
    }
}
