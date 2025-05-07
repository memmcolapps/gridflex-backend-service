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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }
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
            userMapper.insertUser(operator);

            // Insert group assignments
            if (request.getGroupIds() != null) {
                for (Long groupId : request.getGroupIds()) {
                    userMapper.assignUserToGroup(operator.getId(), groupId);
                }
            }
            UserModel user = userMapper.findById(request.getUser().getId());
//            handleAddCache(operator);
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Created User [" + user.getEmail() + "]");
            auditNotificationDTO.setType("user");
            auditNotificationDTO.setCreatedOperator(user);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> updateUser(CreateUserRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }
            UserModel operator = request.getUser();
            operator.setPassword(passwordEncoder.encode(request.getUser().getPassword()));
            // check if operator exist
            UserModel isOperator = userMapper.findById(operator.getId());
            if (isOperator == null){
                return ResponseMap.response(status.getNotFoundCode(), userName + " " + status.getNotFoundDesc(), "");
            }

            // check if groupId exist
            List<Long> isGroupIds = userMapper.checkGroupId(request.getGroupIds());

            if (isGroupIds.size() != request.getGroupIds().size()) {
                return ResponseMap.response(status.getNotFoundCode(), "One or more group IDs " + status.getNotFoundDesc(), "");
                //throw new IllegalArgumentException("One or more group IDs are invalid.");
            }

            // Insert into operators
            userMapper.updateUser(operator);

            System.out.println("user id: " + operator.getId());

            // Insert group assignments
            if (request.getGroupIds() != null) {
                for (Long groupId : request.getGroupIds()) {
                    userMapper.updateUserToGroup(operator.getId(), groupId);
                }
            }
          UserModel user = userMapper.findById(request.getUser().getId());
//            handleAddCache(operator);
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Updated User [" + user.getEmail() + "]");
            auditNotificationDTO.setType("user");
            auditNotificationDTO.setCreatedOperator(user);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updating user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getUsers(String firstname, String lastname, String email, String permission, String dateAdded, String lastActive, int page, int size) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            List<UserModel> users = userMapper.findAllUsers(); // Fetch all users

            // Apply filtering
            Stream<UserModel> userStream = users.stream();

            if (firstname != null && !firstname.isEmpty()) {
                userStream = userStream.filter(u -> u.getFirstname() != null && u.getFirstname().equalsIgnoreCase(firstname));
            }

            if (lastname != null && !lastname.isEmpty()) {
                userStream = userStream.filter(u -> u.getLastname() != null && u.getLastname().equalsIgnoreCase(lastname));
            }

            if (email != null && !email.isEmpty()) {
                userStream = userStream.filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email));
            }

            if (dateAdded != null && !dateAdded.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate date = LocalDate.parse(dateAdded, formatter);
                userStream = userStream.filter(u -> {
                    if (u.getCreatedAt() == null) return false;
                    return !u.getCreatedAt()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            if (lastActive != null && !lastActive.isEmpty()) {
                userStream = userStream.filter(u -> {
                    if (u.getLastActive() == null) return false;
                    return u.getLastActive().toString().contains(lastActive); // adjust if it's a date match
                });
            }

            List<UserModel> filteredUsers = userStream.toList();

            // Pagination logic
            int totalUsers = filteredUsers.size();
            List<UserModel> paginatedUsers;
            if (size == 0) {
                paginatedUsers = filteredUsers; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalUsers);
                int toIndex = Math.min(fromIndex + size, totalUsers);
                paginatedUsers = filteredUsers.subList(fromIndex, toIndex);
            }

            List<UserDTO> userDTOs = new ArrayList<>();

            for (UserModel user : paginatedUsers) {
                List<Group> groups = userMapper.findGroupsByUserId(user.getId());

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

                            List<Permission> permissions = userMapper.findPermissionsByUserAndSubModule(user.getId(), sub.getId());
                            subDTO.setPermissions(permissions);
                            return subDTO;
                        }).collect(Collectors.toList());

                        moduleDTO.setSubModules(subDTOs);
                        return moduleDTO;
                    }).collect(Collectors.toList());

                    groupDTO.setModules(moduleDTOs);
                    return groupDTO;
                }).collect(Collectors.toList());

                // Filter by permission at the end
                if (permission != null && !permission.isEmpty()) {
                    boolean hasPermission = groupDTOs.stream()
                            .flatMap(g -> g.getModules().stream())
                            .flatMap(m -> m.getSubModules().stream())
                            .flatMap(s -> s.getPermissions().stream())
                            .anyMatch(p -> p.getName().equalsIgnoreCase(permission));

                    if (!hasPermission) continue;
                }

                user.setPassword(""); // Remove sensitive data

                UserDTO dto = new UserDTO();
                dto.setUser(user);
                dto.setGroups(groupDTOs);
                userDTOs.add(dto);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", userDTOs);
            response.put("totalData", totalUsers);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) userDTOs.size() / size));

            return ResponseMap.response(status.getSuccessCode(), userName + "s " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while filtering users");
            exceptionErrorLogs.setError_message(exception.getMessage());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getUser(Long userId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

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
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> changeState(Long userId, Boolean state) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }
            // check if operator exist
            UserModel isOperator = userMapper.findById(userId);
            if (isOperator == null) {
                return ResponseMap.response(status.getNotFoundCode(), userName + " " + status.getNotFoundDesc(), "");
            }
            int isStatus = userMapper.changeStatus(userId, state);
            if (isStatus != 1) {
                return ResponseMap.response(status.getUpdateCode(), userName + " " + status.getUpdateFailureDesc(), "");
            }
            String desc = state ? "Activated" : "Deactivated" + " User [" + isOperator.getEmail() + "]";
            UserModel user = userMapper.findById(userId);
//            handleAddCache(operator);
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("user");
            auditNotificationDTO.setCreatedOperator(user);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), state ? " User Activated Successfully" : "User Deactivated Successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }


    public Map<String, Object> createGroupPermission(CreateGroupRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }

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
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Created group [" + group.getTitle() + "]");
            auditNotificationDTO.setType("group");
//            auditNotificationDTO.setCreatedOperator(user);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(),  request.getGroup().getTitle() + " Group " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating group [ACTION]: {}", exception.getMessage().trim(), exception);
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
                return ResponseMap.response(status.getNotFoundCode(), "Group " + status.getNotFoundDesc(), "");
            }

            List<GroupPermission> groupDTOs = groups.stream().map(group -> {
                GroupPermission groupDTO = new GroupPermission();
                groupDTO.setGroup(group);

                List<Permission> permissions = userMapper.findPermissionsByGroup(group.getId());
                permissions.forEach(permission -> {
                    permission.setSubModuleId(0L);
                });
                groupDTO.setPermissions(permissions);

//                List<Module> modules = userMapper.findModulesByGroupId(group.getId());
//
//                List<ModuleWithSubModules> moduleDTOs = modules.stream().map(module -> {
//                    ModuleWithSubModules moduleDTO = new ModuleWithSubModules();
//                    moduleDTO.setModule(module);
//
//                    List<SubModule> subModules = userMapper.findSubModulesByModuleId(module.getId());

//                    List<SubModuleWithPermissions> subDTOs = subModules.stream().map(sub -> {
//                        SubModuleWithPermissions subDTO = new SubModuleWithPermissions();
//                        subDTO.setSubModule(sub);
//
//                        List<Permission> permissions = userMapper.findPermissionsByGroupAndSubModule(group.getId(), sub.getId());
//                        subDTO.setPermissions(permissions);

//                        return subDTO;
//                    }).collect(Collectors.toList());
//
//                    moduleDTO.setSubModules(subDTOs);
//                    return moduleDTO;
//                }).collect(Collectors.toList());

//                groupDTO.setModules(moduleDTOs);
                return groupDTO;
            }).collect(Collectors.toList());

            return ResponseMap.response(status.getSuccessCode(),  "Group Permission" + status.getDesc(), groupDTOs);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

//
//    @Override
//    public Map<String, Object> getFilteredUsers(
//            String email,
//            String permission,
//            String dateAddedTo) {
//
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        try {
//            // Optional caching (implement your own if needed)
//            StringBuilder cacheKeyBuilder = new StringBuilder("users");
//            if (email != null && !email.isEmpty()) cacheKeyBuilder.append("_email_").append(email);
//            if (permission != null && !permission.isEmpty()) cacheKeyBuilder.append("_perm_").append(permission);
//            if (dateAddedFrom != null) cacheKeyBuilder.append("_from_").append(dateAddedFrom);
//            if (dateAddedTo != null) cacheKeyBuilder.append("_to_").append(dateAddedTo);
//            String cacheKey = cacheKeyBuilder.toString();
//
//            Object cachedUsers = userCache.get(cacheKey);
//            if (cachedUsers != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached users " + status.getDesc(), cachedUsers);
//            }
//
//            // Ideally fetch all users with minimal join
//            List<UserModel> allUsers = userMapper.findAllUsersWithGroupsAndPermissions();
//
//            List<UserModel> filteredUsers = allUsers.stream()
//                    .filter(user -> email == null || email.isEmpty() || user.getEmail().equalsIgnoreCase(email))
//                    .filter(user -> {
//                        if (permission == null || permission.isEmpty()) return true;
//                        return user.getGroups().stream()
//                                .flatMap(group -> group.getModules().stream())
//                                .flatMap(module -> module.getSubModules().stream())
//                                .flatMap(sub -> sub.getPermissions().stream())
//                                .anyMatch(perm -> perm.getName().equalsIgnoreCase(permission));
//                    })
//                    .filter(user -> {
//                        if (dateAddedFrom != null && user.getCreatedAt().toLocalDate().isBefore(dateAddedFrom)) return false;
//                        if (dateAddedTo != null && user.getCreatedAt().toLocalDate().isAfter(dateAddedTo)) return false;
//                        return true;
//                    })
//                    .collect(Collectors.toList());
//
//            userCache.put(cacheKey, filteredUsers);
//            return ResponseMap.response(status.getSuccessCode(), "Filtered users " + status.getDesc(), filteredUsers);
//
//        } catch (Exception exception) {
//            log.error("Error occurred while filtering users: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to filter users");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }



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
