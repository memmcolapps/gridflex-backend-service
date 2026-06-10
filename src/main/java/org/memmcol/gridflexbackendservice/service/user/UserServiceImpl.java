package org.memmcol.gridflexbackendservice.service.user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.Module;
import org.memmcol.gridflexbackendservice.model.user.*;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.HandlePermission;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class UserServiceImpl implements  UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenericHandler genericHandler;

    private String userName = "User";

    private final IMap<String, Object> userCache;

    private boolean containsIgnoreCase(String field, String search) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(search);
    }

    private final IMap<String, Object> auditCache;

    public UserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.userCache = hazelcastInstance.getMap("userCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createUser(CreateUserRequest request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            UserModel operator = request.getUser();
            operator.setPassword(passwordEncoder.encode(operator.getPassword()));

            // check if operator exist
            if (userMapper.findByEmail(operator.getEmail(), um.getOrgId()) != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(userName + " Email " + status.getExistDesc());
            }

            // check if groupId exist
            Group isGroupId = userMapper.checkGroupId(request.getGroupId(), um.getOrgId());
            if (isGroupId == null) {
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
            }
            if (!isGroupId.getStatus()) {
                throw new GlobalExceptionHandler.NotFoundException("Group deactivated and cannot be assigned");
            }

            operator.setOrgId(um.getOrgId());
            userMapper.insertUser(operator);
            UUID userId = operator.getId();
            userMapper.assignUserToGroup(userId, request.getGroupId(), um.getOrgId());

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            user.setPassword("");
//            handleAddCache(user);

            AuditLog auditLog = buildAuditLog(um, "User created", userName, user, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {

            genericHandler.logIncidentReport("Creating user service failed");
            genericHandler.logAndSaveException(exception, "creating user");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateUserGroup(CreateUserRequest request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            UserModel operator = request.getUser();

            List<ModuleWithSubModules> module = um.getGroups().getModules();

            boolean hasUserManagementAccess = module.stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase("user management")
                            && Boolean.TRUE.equals(m.getAccess()));

            if (hasUserManagementAccess && operator.getId().equals(um.getId())) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("You cannot update your own group");
            }

//            operator.setPassword(passwordEncoder.encode(operator.getPassword()));

            UserModel users = userMapper.getUserByIdForUpdate(operator.getId(), um.getOrgId());
            // check if operator exist
            if (users == null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(userName + " " + status.getNotFoundDesc());
            }

            // check if groupId exist
            Group isGroupId = userMapper.checkGroupId(request.getGroupId(), um.getOrgId());
            if (isGroupId == null) {
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
            }
            if (!isGroupId.getStatus()) {
                throw new GlobalExceptionHandler.NotFoundException("Group deactivated and cannot be assigned");
            }

            operator.setOrgId(um.getOrgId());
            userMapper.updateUserGroup(operator);
            UUID userId = operator.getId();
            userMapper.updateUserToGroup(userId, request.getGroupId(), um.getOrgId());

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            user.setPassword("");
//            handleAddCache(user);

            AuditLog auditLog = buildAuditLog(um, "User edited", userName, user, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {

            genericHandler.logIncidentReport("Editing user service failed");
            genericHandler.logAndSaveException(exception, "editing user");
            throw exception;
        }
    }


    @Transactional
    @Override
    public Map<String, Object> updateUser(UserModel request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            if(!request.getId().equals(um.getId())) {
                throw new GlobalExceptionHandler.NotFoundException("Editing someone else record is not permissible");
            }

            UserModel isOperator = userMapper.getUserByIdForUpdate(request.getId(), um.getOrgId());
            if (isOperator == null) {
                throw new GlobalExceptionHandler.NotFoundException(userName + " " + status.getNotFoundDesc());
            }

//            if (isOperator.getEmail().equalsIgnoreCase(request.getEmail())){
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(userName + " Email " + status.getExistDesc());
//            }

            request.setOrgId(um.getOrgId());
            userMapper.updateUser(request);
            UUID userId = request.getId();

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
            user.setPassword("");
//            handleAddCache(user);

            AuditLog auditLog = buildAuditLog(um, "User edited", userName, user, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {

            genericHandler.logIncidentReport("Editing user service failed");
            genericHandler.logAndSaveException(exception, "editing user");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getUsers(
            String firstname, String lastname, String email, String permission,
            String dateAdded, String lastActive, String search, Boolean userStatus,
            String sortDirection, int page, int size) {
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
//            Object cachedUser = userCache.get(cacheKey);
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached Users " + status.getDesc(), cachedUser);
//            }
            List<UserModel> enrichedUsers = new ArrayList<>();

            List<UserModel> users = operatorMapper.findAllUsers(um.getOrgId(), 0, 0);

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
            String searchLower = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

            if (!searchLower.isEmpty()) {
                userStream = userStream.filter(user ->
                        containsIgnoreCase(user.getFirstname(), searchLower) ||
                        containsIgnoreCase(user.getLastname(), searchLower) ||
                        containsIgnoreCase(user.getEmail(), searchLower) ||
                        containsIgnoreCase(user.getPhoneNumber(), searchLower) ||
                        containsIgnoreCase(user.getLastActive(), searchLower) ||
                        user.getId() != null && user.getId().toString().toLowerCase(Locale.ROOT).contains(searchLower) ||
                        user.getGroups() != null && containsIgnoreCase(user.getGroups().getGroupTitle(), searchLower) ||
                        user.getNodes() != null && containsIgnoreCase(user.getNodes().getName(), searchLower));
            }

            if (userStatus != null) {
                userStream = userStream.filter(user ->
                        userStatus
                                ? Boolean.TRUE.equals(user.getStatus())
                                : !Boolean.TRUE.equals(user.getStatus()));
            }

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
//                            .toInstant()
//                            .atZone(ZoneId.systemDefault())
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

            List<UserModel> filteredUsers = new ArrayList<>(userStream.toList());
            Comparator<UserModel> comparator = Comparator
                    .comparing((UserModel user) -> user.getFirstname() == null ? "" : user.getFirstname(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(user -> user.getLastname() == null ? "" : user.getLastname(),
                            String.CASE_INSENSITIVE_ORDER);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }
            filteredUsers.sort(comparator);

            // Pagination logic
            int totalUsers = filteredUsers.size();
            List<UserModel> paginatedUsers;
            if (size <= 0) {
                paginatedUsers = filteredUsers; // Return all users
                page= 0;
            } else {
                int fromIndex = Math.min(page * size, totalUsers);
                int toIndex = Math.min(fromIndex + size, totalUsers);
                paginatedUsers = filteredUsers.subList(fromIndex, toIndex);
            }

            int totalUser = size <= 0 ? 1 : Math.max(1, (int) Math.ceil((double) totalUsers / size));

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedUsers);
            response.put("totalData", totalUsers);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", totalUser);

//            userCache.put(cacheKey, response);

            return ResponseMap.response(status.getSuccessCode(), userName + "s " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);

            genericHandler.logIncidentReport("Fetching users service failed");
            genericHandler.logAndSaveException(exception, "fetch users");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeGroupPermissionStatus(UUID groupId, Boolean state) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            UserGroup verify = userMapper.getUserGroup(groupId);
            if(verify != null){
                throw new GlobalExceptionHandler.NotFoundException("Cannot be deactivated because is in used");
            }

            userMapper.changeGroupStatus(groupId, state);

            String desc = state ? "Group activated" : "Group deactivated";

            AuditLog auditLog = buildAuditLog(user, desc, "Group", null, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), desc + " successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while updating user [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Changing group permission status service failed");
            genericHandler.logAndSaveException(exception, "changing group permission status");
            throw exception;
        }

    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getUser(UUID userId) {
        try {

            UserModel um = handleUserValidation();

//            Object cachedUser = userCache.get(userId.toString()+"_"+um.getOrgId());
//
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + userName + " " + status.getDesc(), cachedUser);
//            }

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

//            handleAddCache(userDTO);


            return ResponseMap.response(status.getSuccessCode(), userName + " " + status.getDesc(), userDTO);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching user service failed");
            genericHandler.logAndSaveException(exception, "fetching user status");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeState(UUID userId, Boolean state) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);
            // check if operator exist
            UserModel isOperator = userMapper.getUserByIdForUpdate(userId, um.getOrgId());
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
//            handleAddCache(user);
            AuditLog auditLog = buildAuditLog(user, desc, userName, user, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), state ? " User activated successfully" : "User deactivated successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);

            genericHandler.logIncidentReport("Changing user status service failed");
            genericHandler.logAndSaveException(exception, "changing user state");
            throw exception;
        }
    }

    @Transactional
    public Map<String, Object> createGroupPermission(CreateGroupRequest request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);
            UUID orgId =  um.getOrgId();

            Group group = new Group();
            group.setGroupTitle(request.getGroupTitle());
            group.setCreatedAt(request.getCreatedAt());
            group.setUpdatedAt(request.getUpdatedAt());
            group.setOrgId(orgId);
            group.setStatus(true);

            System.out.print("request.getGroupTitle(): " + request.getGroupTitle());
            /// Check if group already exist (No duplication title allowed)
            String isGroupTitle = userMapper.checkGroupName(request.getGroupTitle(), um.getOrgId());
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

                if(moduleWithSubs.getSubModules() != null && !moduleWithSubs.getSubModules().isEmpty()) {
                    for (SubModuleWithPermissions smwp : moduleWithSubs.getSubModules()) {
                        SubModule subModule = new SubModule();
                        subModule.setName(smwp.getName());
                        subModule.setAccess(smwp.getAccess());
                        subModule.setModuleId(moduleId);
                        subModule.setOrgId(orgId);

                        /// Create and insert submodule
                        userMapper.insertSubModule(subModule);
                    }
                } else {
                    String result = formatName(moduleWithSubs.getName());

                    if(moduleWithSubs.getName().equalsIgnoreCase("hes")
                            || result.equalsIgnoreCase("vending")
                            || result.equalsIgnoreCase("billing")
                            || result.equalsIgnoreCase("user management")) {
                        SubModule subModule = new SubModule();
                        subModule.setName(moduleWithSubs.getName());
                        subModule.setAccess(moduleWithSubs.getAccess());
                        subModule.setModuleId(moduleId);
                        subModule.setOrgId(orgId);

                        /// Create and insert submodule
                        userMapper.insertSubModule(subModule);
                    } else {
                        throw  new GlobalExceptionHandler.NotFoundException("Module " + moduleWithSubs.getName() + " " + status.getNotFoundDesc());
                    }
                }


            }

            /// Assign permission to the group created
            userMapper.assignPermissionToGroup(groupId, permission.getId(), um.getOrgId());

            String desc = capitalizeFirstLetter(request.getGroupTitle() + " created");
            AuditLog auditLog = buildAuditLog(um, desc, "Group", null, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(),  "Group "+ request.getGroupTitle() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Creating group permission service failed");
            genericHandler.logAndSaveException(exception, "create group permission failed");
            throw exception;
        }

    }

    public static String formatName(String value) {

        return Arrays.stream(value.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase()
                        + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @Transactional
    @Override
    public Map<String, Object> updateGroupPermission(CreateGroupRequest request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID orgId =  um.getOrgId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            Group group = new Group();
            group.setId(request.getId());
            group.setGroupTitle(request.getGroupTitle());
            group.setUpdatedAt(request.getUpdatedAt());

            Group isGroupId = userMapper.checkGroupId(group.getId(), orgId);
            if (isGroupId == null) {
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
            }

            /// Check if group already exist (No duplication title allowed)
//            String isGroupTitle = userMapper.checkGroupName(request.getGroupTitle());
////            group.setId(isGroupTitle.get);
//            if(isGroupTitle == null) {
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Group title '" + request.getGroupTitle() + "' already exist.");
//            }

            group.setId(isGroupId.getId());
            /// Insert group and retrieve group ID
            userMapper.updateGroup(group); // Automatically sets group.id
            UUID groupId = group.getId();

            Permission permission = new Permission();
            permission.setId(request.getPermission().getId());
            permission.setEdit(request.getPermission().getEdit());
            permission.setApprove(request.getPermission().getApprove());
            permission.setDisable(request.getPermission().getDisable());
            permission.setView(request.getPermission().getView());

            /// Insert permission
            userMapper.updatePermission(permission);

            for (ModuleWithSubModules moduleWithSubs : request.getModules()) {

                Module module = new Module();
                module.setName(moduleWithSubs.getName());
                module.setAccess(moduleWithSubs.getAccess());
                module.setOrgId(orgId);
                module.setGroupId(groupId);
                module.setId(moduleWithSubs.getId());

                if(module.getId() != null){
                    /// Update module
                    userMapper.updateModule(module);
                } else {
                    /// Create and insert module
                    userMapper.insertModule(module);
                }
                UUID moduleId = module.getId();
                 // Auto-generated ID

                for (SubModuleWithPermissions smwp : moduleWithSubs.getSubModules()) {
                    SubModule subModule = new SubModule();
                    subModule.setName(smwp.getName());
                    subModule.setAccess(smwp.getAccess());
                    subModule.setModuleId(moduleId);
                    subModule.setId(smwp.getId());
                    subModule.setOrgId(orgId);
                    subModule.setId(smwp.getId());

                    if(subModule.getId() != null){
                        /// Update submodule
                        userMapper.updateSubModule(subModule);
                    } else {
                        /// Create and insert submodule
                        userMapper.insertSubModule(subModule);
                    }

                }
            }

            /// Assign permission to the group created
//            userMapper.assignUpdatePermissionToGroup(groupId, permission.getId(), um.getOrgId());

            String desc = capitalizeFirstLetter(request.getGroupTitle() + " updated");
            AuditLog auditLog = buildAuditLog(um, desc, "Group", null, metadata);

            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(),  "Group '"+ request.getGroupTitle() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Updating group permission service failed");
            genericHandler.logAndSaveException(exception, "update group permission failed");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getGroups(String search, Boolean groupStatus, String sortDirection) {
        try {

            UserModel um = handleUserValidation();

            List<Group> groups = userMapper.getGroups(um.getOrgId());
            if (groups == null) {
                throw new GlobalExceptionHandler.NotFoundException("Group " + status.getNotFoundDesc());
            }

            String searchLower = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            List<GroupWithPermissionsDTO> groupDTOs = groups.stream()
                    .filter(group -> searchLower.isEmpty() ||
                            group.getGroupTitle() != null && group.getGroupTitle().toLowerCase(Locale.ROOT).contains(searchLower))
                    .filter(group -> groupStatus == null ||
                            groupStatus && !Boolean.FALSE.equals(group.getStatus()) ||
                            !groupStatus && Boolean.FALSE.equals(group.getStatus()))
                    .map(group -> {
                GroupWithPermissionsDTO groupDTO = new GroupWithPermissionsDTO();
                groupDTO.setId(group.getId());
                groupDTO.setGroupTitle(group.getGroupTitle());
                groupDTO.setOrgId(group.getOrgId());
                groupDTO.setStatus(group.getStatus());

                Permission permissions = userMapper.findPermissionsByGroup(group.getId(), um.getOrgId());

                List<ModuleWithSubModules> moduleDTO = userMapper.getModule(group.getId());

                groupDTO.setModules(moduleDTO);

                groupDTO.setPermissions(permissions);

                return groupDTO;
            }).collect(Collectors.toList());

            Comparator<GroupWithPermissionsDTO> comparator = Comparator.comparing(
                    group -> group.getGroupTitle() == null ? "" : group.getGroupTitle(),
                    String.CASE_INSENSITIVE_ORDER);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }
            groupDTOs.sort(comparator);

            return ResponseMap.response(status.getSuccessCode(),  "Group Permission " + status.getDesc(), groupDTOs);
        } catch (Exception exception) {
            log.error("Error occurred while fetching user [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching groups service failed");
            genericHandler.logAndSaveException(exception, "fetching groups status");
            throw exception;
        }

    }

    @Override
    public Map<String, Object> getOrgModule() {
        try {
            UserModel um = handleUserValidation();

            List<XYZ> resp = userMapper.getOrgAccessModule(um.getOrgId());

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
        } catch (Exception e){
            throw e;
        }
    }


    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedUser(createdEntity instanceof UserModel ? (UserModel) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
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
