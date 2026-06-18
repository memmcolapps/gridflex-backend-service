package org.memmcol.gridflexbackendservice.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserPermissionResponse {

    private String sub;

    private List<String> roles;

    @JsonProperty("permission_tree")
    private List<PermissionTree> permissionTree;

    @Data
    public static class PermissionTree {

        private Permissions permissions;

        private List<ModuleWrapper> modules;

        private Group group;
    }

    @Data
    public static class Permissions {

        private boolean view;

        private boolean edit;

        private boolean disable;

        private boolean approve;
    }

    @Data
    public static class ModuleWrapper {

        private Module module;

        private List<SubmoduleWrapper> submodules;
    }

    @Data
    public static class Module {

        private boolean access;

        private String name;
    }

    @Data
    public static class SubmoduleWrapper {

        private Submodule submodule;
    }

    @Data
    public static class Submodule {

        private String code;

        private boolean access;

        private String name;
    }

    @Data
    public static class Group {

        private UUID userId;

        private String nodeTitle;

        private String groupTitle;

        private UUID userNodeId;

        private String nodeType;

        private UUID nodeId;

        private UUID orgId;
    }
}
