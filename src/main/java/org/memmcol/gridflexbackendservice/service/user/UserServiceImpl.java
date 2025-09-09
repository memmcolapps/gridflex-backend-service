package org.memmcol.gridflexbackendservice.service.user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.Module;
import org.memmcol.gridflexbackendservice.model.user.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

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
    private HttpServletRequest httpServletRequest;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userName = "User";

    private final IMap<String, Object> userCache;

    private final IMap<String, Object> auditCache;

    public UserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.userCache = hazelcastInstance.getMap("userCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createUser(CreateUserRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            UserModel operator = request.getUser();
            operator.setPassword(passwordEncoder.encode(operator.getPassword()));

            // check if operator exist
            UserModel isOperator = userMapper.findByEmail(operator.getEmail(), um.getOrgId());
            if (isOperator != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(userName + " " + status.getExistDesc());
            }

            // check if groupId exist
            UUID isGroupId = userMapper.checkGroupId(request.getGroupId(), um.getOrgId());
            if (isGroupId == null){
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
            }

            operator.setOrgId(um.getOrgId());

            // Insert into operators
            userMapper.insertUser(operator);
            UUID userId = operator.getId();
            
            userMapper.assignUserToGroup(userId, request.getGroupId(), um.getOrgId());

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            String desc = "User newly created";
            user.setPassword("");
            handleAddCache(user);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(userName);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setCreatedUser(user);
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

    @Transactional
    @Override
    public Map<String, Object> updateUser(UserModel request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

//            UserModel operator = request.getUser();
            request.setPassword(passwordEncoder.encode(request.getPassword()));
            // check if operator exist
            UserModel isOperator = userMapper.findById(request.getId(), um.getOrgId());
            if (isOperator == null){
                throw new GlobalExceptionHandler.NotFoundException(userName + " " + status.getNotFoundDesc());
            }

//            // check if groupId exist
//            UUID isGroupId = userMapper.checkGroupId(request.getGroupId(), um.getOrgId());
//            if (isGroupId == null){
//                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
//            }

            request.setOrgId(um.getOrgId());

            // Insert into operators
            userMapper.updateUser(request);
            UUID userId = request.getId();
//            userMapper.updateUserToGroup(userId, isGroupId, um.getOrgId());

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            String desc = "User edited";
            user.setPassword("");
            handleAddCache(user);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(userName);
            auditNotificationDTO.setCreatedUser(user);
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

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getUsers(
            String firstname, String lastname, String email, String permission,
            String dateAdded, String lastActive, int page, int size) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("users_"+um.getOrgId());
            if (firstname != null && !firstname.isEmpty()) cacheKeyBuilder.append("_firstname_").append(firstname);
            if (lastname != null && !lastname.isEmpty()) cacheKeyBuilder.append("_lastname_").append(lastname);
            if (email != null && !email.isEmpty()) cacheKeyBuilder.append("_email_").append(email);
            if (permission != null && !permission.isEmpty()) cacheKeyBuilder.append("_permission_").append(permission);
            if (dateAdded != null && !dateAdded.isEmpty()) cacheKeyBuilder.append("_dateAdded_").append(dateAdded);
            if (lastActive != null && !lastActive.isEmpty()) cacheKeyBuilder.append("_lastActive_").append(lastActive);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedUser = userCache.get(cacheKey);
            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Users " + status.getDesc(), cachedUser);
            }
            List<UserModel> enrichedUsers = new ArrayList<>();

//            List<UserModel> users = userMapper.findAllUsers(); // Fetch all users
            List<UserModel> users = operatorMapper.findAllUsers(um.getOrgId());

            for (UserModel user : users) {
                /// Retrieve user data from database
                UserModel userDTO = operatorMapper.findAuthByUserId(user.getId(), um.getOrgId());

                userDTO.setPassword("");
                List<Node> nodes = operatorMapper.getNodeWithChildren(userDTO.getNodeId(), um.getOrgId());

                Map<UUID, Node> nodeMap = new HashMap<>();
                Node root = null;

                for (Node node : nodes) {
                    node.setNodesTree(new ArrayList<>());
                    nodeMap.put(node.getId(), node);
                }

                for (Node node : nodes) {
                    if (node.getId().equals(user.getNodeId())) {
                        root = node; // this is the node we're querying for
                    }
                    if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                        Node parent = nodeMap.get(node.getParentId());
                        parent.getNodesTree().add(node);
                    }
                }
                userDTO.setNodes(root);
                enrichedUsers.add(userDTO); // store the enriched user
            }

            // Apply filtering
            Stream<UserModel> userStream = enrichedUsers.stream();

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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedUsers);
            response.put("totalData", totalUsers);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedUsers.size() / size));

            userCache.put(cacheKey, response);

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

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getUser(UUID userId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            Object cachedUser = userCache.get(userId.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + userName + " " + status.getDesc(), cachedUser);
            }

            UserModel user = userMapper.findById(userId, um.getOrgId());
            if (user == null) {
                throw new GlobalExceptionHandler.NotFoundException(userName + " " + status.getNotFoundDesc());
            }

            /// Retrieve user data from database
            UserModel userDTO = operatorMapper.findAuthByUserId(userId, um.getOrgId());

            userDTO.setPassword("");
            List<Node> nodes = operatorMapper.getNodeWithChildren(userDTO.getNodeId(), um.getOrgId());

            Map<UUID, Node> nodeMap = new HashMap<>();
            Node root = null;

            for (Node node : nodes) {
                node.setNodesTree(new ArrayList<>());
                nodeMap.put(node.getId(), node);
            }

            for (Node node : nodes) {
                if (node.getId().equals(user.getNodeId())) {
                    root = node; // this is the node we're querying for
                }
                if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                    Node parent = nodeMap.get(node.getParentId());
                    parent.getNodesTree().add(node);
                }
            }
            userDTO.setNodes(root);

            handleAddCache(userDTO);


            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getDesc(), userDTO);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeState(UUID userId, Boolean state) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            // check if operator exist
            UserModel isOperator = userMapper.findById(userId, um.getOrgId());
            if (isOperator == null) {
                throw new GlobalExceptionHandler.NotFoundException(userName + " " + status.getNotFoundDesc());
            }
            int isStatus = userMapper.changeStatus(userId, state);
            if (isStatus != 1) {
                throw new GlobalExceptionHandler.NotFoundException(userName + " " + status.getUpdateFailureDesc());
            }
            String desc = state ? "User activated" : "User deactivated";
            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            user.setPassword("");
            handleAddCache(user);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(userName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedUser(user);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), state ? " User activated successfully" : "User deactivated successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    public Map<String, Object> createGroupPermission(CreateGroupRequest request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            UUID orgId =  um.getOrgId();

            Group group = new Group();
            group.setGroupTitle(request.getGroupTitle());
            group.setCreatedAt(request.getCreatedAt());
            group.setUpdatedAt(request.getUpdatedAt());
            group.setOrgId(orgId);

            /// Check if group already exist (No duplication title allowed)
            String isGroupTitle = userMapper.checkGroupName(request.getGroupTitle());
            if(isGroupTitle != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Group title '" + request.getGroupTitle() + "' already exist.");
            }

            /// Insert group and retrieve group ID
            userMapper.insertGroup(group); // Automatically sets group.id
            UUID groupId = group.getId();

            Permission permission = new Permission();
            permission.setOrgId(orgId);
            permission.setEdit(request.getPermission().getEdit());
            permission.setApprove(request.getPermission().getApprove());
            permission.setDisable(request.getPermission().getDisable());
            permission.setView(request.getPermission().getView());

            /// Insert permission
            userMapper.insertPermission(permission);

            for (ModuleWithSubModules moduleWithSubs : request.getModules()) {

                Module module = new Module();
                module.setName(moduleWithSubs.getName());
                module.setAccess(moduleWithSubs.getAccess());
                module.setOrgId(orgId);
                module.setGroupId(groupId);

                /// Create and insert module
                userMapper.insertModule(module);  // ID will be set here
                UUID moduleId = module.getId();   // Auto-generated ID

                for (SubModuleWithPermissions smwp : moduleWithSubs.getSubModules()) {
                    SubModule subModule = new SubModule();
                    subModule.setName(smwp.getName());
                    subModule.setAccess(smwp.getAccess());
                    subModule.setModuleId(moduleId);
                    subModule.setOrgId(orgId);

                    /// Create and insert submodule
                    userMapper.insertSubModule(subModule);
                }
            }

            /// Assign permission to the group created
            userMapper.assignPermissionToGroup(groupId, permission.getId(), um.getOrgId());

            String desc = capitalizeFirstLetter(request.getGroupTitle() + " newly created");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("Group");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
//            auditNotificationDTO.setCreatedUser(user);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(),  "Group '"+ request.getGroupTitle() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating group [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getGroups() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            List<Group> groups = userMapper.getGroups(um.getOrgId());
            if (groups == null) {
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
//                return ResponseMap.response(status.getNotFoundCode(), "Group " + status.getNotFoundDesc(), "");
            }

            List<GroupWithPermissionsDTO> groupDTOs = groups.stream().map(group -> {
                GroupWithPermissionsDTO groupDTO = new GroupWithPermissionsDTO();
                groupDTO.setId(group.getId());
                groupDTO.setGroupTitle(group.getGroupTitle());
                groupDTO.setOrgId(group.getOrgId());

                Permission permissions = userMapper.findPermissionsByGroup(group.getId(), um.getOrgId());

                List<ModuleWithSubModules> moduleDTO = userMapper.getModule(group.getId());

                groupDTO.setModules(moduleDTO);

                groupDTO.setPermissions(permissions);

                return groupDTO;
            }).collect(Collectors.toList());

            return ResponseMap.response(status.getSuccessCode(),  "Group Permission " + status.getDesc(), groupDTOs);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }


    private void handleAddCache(UserModel user) {
        userCache.remove(user.getId().toString()+"_"+user.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : userCache.keySet()) {
            if (key.startsWith("users_"+user.getOrgId())) {
                userCache.remove(key);
            }
        }
        userCache.put(user.getId().toString()+"_"+user.getOrgId(), user);  // Cache updated or deleted entity
    }
}
