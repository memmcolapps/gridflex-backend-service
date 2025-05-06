package org.memmcol.gridflexbackendservice.service.user;

import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.*;
import org.memmcol.gridflexbackendservice.model.Module;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
public class UserServiceImpl implements  UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userName = "User";

    @Override
    public Map<String, Object> createUser(CreateUserRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel operator = request.getUser();
            operator.setPassword(passwordEncoder.encode(request.getUser().getPassword()));

            // check if operator exist
            UserModel isOperator = userMapper.findById(request.getUser().getId());
            if (isOperator != null){
                return ResponseMap.response(status.getExistCode(), userName + " " + status.getExistDesc(), "");
            }

            // check if groupId exist
            List<Long> isGroupIds = userMapper.checkGroupId(request.getGroupIds());

            if (isGroupIds.size() != request.getGroupIds().size()) {
                return ResponseMap.response(status.getNotFoundCode(), "One or more group IDs " + status.getNotFoundDesc(), "");
                //throw new IllegalArgumentException("One or more group IDs are invalid.");
            }

            // Insert into operators
            userMapper.insertOperator(operator);

            // Insert group assignments
            if (request.getGroupIds() != null) {
                for (Long groupId : request.getGroupIds()) {
                    userMapper.assignUserToGroup(operator.getId(), groupId);
                }
            }
//            handleAddCache(operator);
//            auditNotificationDTO.setCreator(isOperatorExist);
//            auditNotificationDTO.setDescription("Created Tariff [" + tariff.getName() + "]");
//            auditNotificationDTO.setType("tariff");
//            auditNotificationDTO.setCreatedTariff(tariffByName);
//            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> updateUser(UserModel user) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getUsers(int page, int size) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getUser(Long userId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String username = (authentication != null) ? authentication.getName() : "Unknown";
//            UserModel user = userMapper.findByUsername(username);
//            if (!user.getStatus()) {
//                throw new LockedException("User is blocked");
//            }

            UserModel user = userMapper.findById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            List<Group> groups = userMapper.findGroupsByUserId(userId);

            List<GroupWithPermissionsDTO> groupDTOs = groups.stream().map(group -> {
                GroupWithPermissionsDTO groupDTO = new GroupWithPermissionsDTO();
                groupDTO.setGroup(group);

                List<Module> modules = userMapper.findModulesByGroupId(group.getId());

                List<ModuleWithSubModules> moduleDTOs = modules.stream().map(module -> {
                    ModuleWithSubModules moduleDTO = new ModuleWithSubModules();
                    moduleDTO.setModule(module);

                    List<SubModule> subModules = userMapper.findSubModulesByModuleId(module.getId());

                    List<SubModuleWithPermissions> subDTOs = subModules.stream().map(sub -> {
                        SubModuleWithPermissions subDTO = new SubModuleWithPermissions();
                        subDTO.setSubModule(sub);

                        List<Permission> permissions = userMapper.findPermissionsByUserAndSubModule(userId, sub.getId());
                        subDTO.setPermissions(permissions);

                        return subDTO;
                    }).collect(Collectors.toList());

                    moduleDTO.setSubModules(subDTOs);
                    return moduleDTO;
                }).collect(Collectors.toList());

                groupDTO.setModules(moduleDTOs);
                return groupDTO;
            }).collect(Collectors.toList());
            user.setPassword("");
            UserDTO response = new UserDTO();
            response.setUser(user);
            response.setGroups(groupDTOs);
            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getDesc(), response);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> changeState(String status) {
        return Map.of();
    }


    public Map<String, Object> createGroupPermission(CreateGroupRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Group group = request.getGroup();
            userMapper.insertGroup(group);

            for (ModuleWithSubModules moduleWithSubs : request.getModules()) {
                Module module = moduleWithSubs.getModule();
                userMapper.insertModule(module);

                for (SubModuleWithPermissions smwp : moduleWithSubs.getSubModules()) {
                    SubModule sm = smwp.getSubModule();
                    sm.setModuleId(module.getId());
                    userMapper.insertSubModule(sm);

                    for (Permission p : smwp.getPermissions()) {
                        p.setSubModuleId(sm.getId());
                        userMapper.insertPermission(p);
                        userMapper.assignPermissionToGroup(group.getId(), p.getId());
                    }
                }
            }

//            for (Long userId : request.getUserIds()) {
//                userMapper.assignUserToGroup(userId, group.getId());
//            }
            return ResponseMap.response(status.getSuccessCode(),  request.getGroup().getTitle() + " Group " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> getGroups() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            List<Group> groups = userMapper.getGroups();
            if (groups == null) {
                return ResponseMap.response(status.getNotFoundCode(), "group " + status.getNotFoundDesc(), "");
            }
            return ResponseMap.response(status.getSuccessCode(),  "groups " + status.getDesc(), groups);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }


//    private void handleAddCache(UserModel operator) {
//        tariffCache.remove(operator.getEmail());
//        for (String key : auditCache.keySet()) {
//            if (key.startsWith("grid_flex_audit_log_page_")) {
//                auditCache.remove(key);
//            }
//        }
//        for (String key : tariffCache.keySet()) {
//            if (key.startsWith("tariffs_")) {
//                tariffCache.remove(key);
//            }
//        }
//        tariffCache.put(tariff.getId().toString(), tariff);  // Cache updated or deleted entity
//    }
}
