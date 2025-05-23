package org.memmcol.gridflexbackendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.user.ModuleWithSubModules;
import org.memmcol.gridflexbackendservice.model.user.Permission;
import org.memmcol.gridflexbackendservice.model.user.SubModuleWithPermissions;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
	@Autowired
	private AuthMapper authMapper;
	@Autowired private ResponseProperties status;
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserModel userDTO = authMapper.findAuthByUserEmail(email);

		if (userDTO == null) {
			throw new UsernameNotFoundException("User " + status.getNotFoundDesc());
		}

		System.out.println("User " + userDTO.getEmail());
		System.out.println("User " + userDTO.getGroups().getPermissions().getOrgId());
//		UserModel user = userDTO;

		if (!userDTO.getStatus()) {
			log.info("User is blocked: {}", userDTO.getStatus());
			throw new LockedException("User is disabled");
		}

		Set<GrantedAuthority> authorities = new HashSet<>();
		List<Map<String, Object>> groupModulePermissionTree = new ArrayList<>();

		Map<String, Object> groupMap = new HashMap<>();
		Map<String, Object> groupOrgmap = new HashMap<>();
		groupOrgmap.put("orgId", userDTO.getId());
		groupOrgmap.put("groupTitle", userDTO.getGroups().getGroupTitle());
		groupMap.put("group", groupOrgmap);

		// Handle Permissions
		Permission permissions = userDTO.getGroups().getPermissions();
		Map<String, Object> permissionMap = new HashMap<>();
		permissionMap.put("edit", permissions.getEdit());
		permissionMap.put("disable", permissions.getDisable());
		permissionMap.put("approve", permissions.getApprove());
		permissionMap.put("view", permissions.getView());

		groupMap.put("permissions", permissionMap);

		// Add each permission as GrantedAuthority
//		authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Optional: Default role
		authorities.add(new SimpleGrantedAuthority("PERM_EDIT_" + permissions.getEdit().toString().toUpperCase()));
		authorities.add(new SimpleGrantedAuthority("PERM_DISABLE_" + permissions.getDisable().toString().toUpperCase()));
		authorities.add(new SimpleGrantedAuthority("PERM_APPROVE_" + permissions.getApprove().toString().toUpperCase()));
		authorities.add(new SimpleGrantedAuthority("PERM_VIEW_" + permissions.getView().toString().toUpperCase()));


		List<Map<String, Object>> modulesList = new ArrayList<>();
				if (userDTO.getGroups().getModules() != null) {
					for (ModuleWithSubModules moduleDTO : userDTO.getGroups().getModules()) {
						Map<String, Object> moduleMap = new HashMap<>();
						Map<String, Object> simpleModule = new HashMap<>();
						simpleModule.put("name", moduleDTO.getName());
						simpleModule.put("access", moduleDTO.getAccess());
						moduleMap.put("module", simpleModule);
//						modulesList.add(moduleMap);
						List<Map<String, Object>> submodulesList = new ArrayList<>();
						if (moduleDTO.getSubModules() != null) {
							for (SubModuleWithPermissions subDTO : moduleDTO.getSubModules()) {
								Map<String, Object> submoduleMap = new HashMap<>();
								Map<String, Object> simpleSubModule = new HashMap<>();
								simpleSubModule.put("name", subDTO.getName());
								simpleSubModule.put("access", subDTO.getAccess());
								submoduleMap.put("submodule", simpleSubModule);
//								submoduleMap.put("submodule", subDTO);
								submodulesList.add(submoduleMap);
							}
						}

						moduleMap.put("submodules", submodulesList);
						modulesList.add(moduleMap);
					}
				}

				groupMap.put("modules", modulesList);
				groupModulePermissionTree.add(groupMap);
//			}
//		}

		String permissionsJson = "";
		try {
			permissionsJson = mapper.writeValueAsString(groupModulePermissionTree);
			log.debug("User Permission Tree JSON: {}", permissionsJson);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error serializing permissions JSON", e);
            throw new RuntimeException(e);
        }
		log.info("User found in the database: {}", userDTO.getEmail());
		authMapper.updateLoginState(email);
        return new CustomUserDetails(
				userDTO.getEmail(),
				userDTO.getPassword(),
				authorities,
				permissionsJson
		);
	}
}



//if (userDTO.getGroups() != null) {
//			for (GroupWithPermissionsDTO groupDTO : userDTO.getGroups()) {
//				Map<String, Object> groupMap = new HashMap<>();
//				groupMap.put("group", groupDTO.getTitle());
//
//				List<Map<String, Object>> modulesList = new ArrayList<>();
//				if (groupDTO.getModules() != null) {
//					for (ModuleWithSubModules moduleDTO : groupDTO.getModules()) {
//						Map<String, Object> moduleMap = new HashMap<>();
//						moduleMap.put("module", moduleDTO.getName());
//
//						List<Map<String, Object>> submodulesList = new ArrayList<>();
//						if (moduleDTO.getSubModules() != null) {
//							for (SubModuleWithPermissions subDTO : moduleDTO.getSubModules()) {
//								Map<String, Object> submoduleMap = new HashMap<>();
//								submoduleMap.put("submodule", subDTO.getName());
//
//								List<String> permissionNames = subDTO.getPermissions().stream()
//										.map(permission -> {
//											authorities.add(new SimpleGrantedAuthority(permission.getName()));
//											return permission.getName();
//										})
//										.collect(Collectors.toList());
//
//								submoduleMap.put("permissions", permissionNames);
//								submodulesList.add(submoduleMap);
//							}
//						}
//
//						moduleMap.put("submodules", submodulesList);
//						modulesList.add(moduleMap);
//					}
//				}
//
//				groupMap.put("modules", modulesList);
//				groupModulePermissionTree.add(groupMap);
//			}
//		}

