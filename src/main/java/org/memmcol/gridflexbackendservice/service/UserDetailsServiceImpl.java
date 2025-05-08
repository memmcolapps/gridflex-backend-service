package org.memmcol.gridflexbackendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.*;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
	private final AuthMapper authMapper;
	private final ResponseProperties status;
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserDTO userDTO = authMapper.findAuthByUserEmail(email);

		if (userDTO == null || userDTO.getUser() == null) {
			throw new UsernameNotFoundException("User " + status.getNotFoundDesc());
		}

		UserModel user = userDTO.getUser();

		if (!user.getStatus()) {
			log.info("User is blocked: {}", user.getStatus());
			throw new LockedException("User is disabled");
		}

		log.info("User found in the database: {}", user.getEmail());
		authMapper.updateLoginState(email);

		Set<GrantedAuthority> authorities = new HashSet<>();
		List<Map<String, Object>> groupModulePermissionTree = new ArrayList<>();

		if (userDTO.getGroups() != null) {
			for (GroupWithPermissionsDTO groupDTO : userDTO.getGroups()) {
				Map<String, Object> groupMap = new HashMap<>();
				groupMap.put("group", groupDTO.getGroup().getTitle());

				List<Map<String, Object>> modulesList = new ArrayList<>();
				if (groupDTO.getModules() != null) {
					for (ModuleWithSubModules moduleDTO : groupDTO.getModules()) {
						Map<String, Object> moduleMap = new HashMap<>();
						moduleMap.put("module", moduleDTO.getModule().getName());

						List<Map<String, Object>> submodulesList = new ArrayList<>();
						if (moduleDTO.getSubModules() != null) {
							for (SubModuleWithPermissions subDTO : moduleDTO.getSubModules()) {
								Map<String, Object> submoduleMap = new HashMap<>();
								submoduleMap.put("submodule", subDTO.getSubModule().getName());

								List<String> permissionNames = subDTO.getPermissions().stream()
										.map(permission -> {
											authorities.add(new SimpleGrantedAuthority(permission.getName()));
											return permission.getName();
										})
										.collect(Collectors.toList());

								submoduleMap.put("permissions", permissionNames);
								submodulesList.add(submoduleMap);
							}
						}

						moduleMap.put("submodules", submodulesList);
						modulesList.add(moduleMap);
					}
				}

				groupMap.put("modules", modulesList);
				groupModulePermissionTree.add(groupMap);
			}
		}

		String permissionsJson = "";
		try {
			permissionsJson = mapper.writeValueAsString(groupModulePermissionTree);
			log.debug("User Permission Tree JSON: {}", permissionsJson);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error serializing permissions JSON", e);
            throw new RuntimeException(e);
        }

        return new CustomUserDetails(
				user.getEmail(),
				user.getPassword(),
				authorities,
				permissionsJson
		);
	}
}







//
//@Component
//@RequiredArgsConstructor
//public class UserDetailsServiceImpl implements UserDetailsService {
//
//	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
//
//	private final AuthMapper authMapper;
//	private final ResponseProperties status;
//
//	@Override
//	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//		UserDTO userDTO = authMapper.findAuthByUserEmail(email);
//
//		if (userDTO == null || userDTO.getUser() == null) {
//			throw new UsernameNotFoundException("User " + status.getNotFoundDesc());
//		}
//		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getUser().getEmail());
//		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getGroup().getTitle());
//		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getModule().getName());
//		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getSubModules().get(0).getSubModule().getName());
//		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getSubModules().get(0).getPermissions().get(0).getName());
//		UserModel user = userDTO.getUser();
//
//		if (!user.getStatus()) {
//			log.info("User is blocked: {}", user.getStatus());
//			throw new LockedException("User is disabled");
//		}
//
//		log.info("User found in the database: {}", user.getEmail());
//		authMapper.updateLoginState(email);
//
//		// Convert group/module/submodule/permissions into GrantedAuthorities
//		Set<GrantedAuthority> authorities = new HashSet<>();
//
//		if (userDTO.getGroups() != null) {
//			for (GroupWithPermissionsDTO groupDTO : userDTO.getGroups()) {
//				if (groupDTO.getModules() != null) {
//					for (ModuleWithSubModules moduleDTO : groupDTO.getModules()) {
//						if (moduleDTO.getSubModules() != null) {
//							for (SubModuleWithPermissions subDTO : moduleDTO.getSubModules()) {
//								if (subDTO.getPermissions() != null) {
//									for (Permission permission : subDTO.getPermissions()) {
//										System.out.println("permissions: "+permission.getName());
//										authorities.add(new SimpleGrantedAuthority(permission.getName()));
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//
//		return new User(user.getEmail(), user.getPassword(), authorities);
//	}
//}



//@Component
//@RequiredArgsConstructor
//public class UserDetailsServiceImpl implements UserDetailsService {
//	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
//	@Autowired private AuthMapper operatorMapper;
//
//	@Autowired private ResponseProperties status;
//
//    @Autowired private UserMapper userMapper;
//
//	@Override
//	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//		Operator user = operatorMapper.findAuthByEmail(email);
//
////		List<Group> groups = userMapper.findGroupsByUserId(userId);
//		if (user == null) {
//			throw new UsernameNotFoundException("Operator "+status.getNotFoundDesc());
//		} else if(!user.isUstate()) {
//			log.info("User is blocked: {}", user.isUstate());
//			throw new LockedException("User is blocked");
//		} else {
//			log.info("User found in the database: {}", user.isUstate());
//
//			operatorMapper.updateLoginState(email);
//			Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
////			user.getRoles().forEach(role -> {
////				authorities.add(new SimpleGrantedAuthority(role.getOperatorRole()));
////			});
//
//			// Group-based permissions
//			groups.getGroups().forEach(group -> {
//				group.getModules().forEach(module -> {
//					module.getSubModules().forEach(subModule -> {
//						subModule.getPermissions().forEach(permission -> {
//							authorities.add(new SimpleGrantedAuthority(permission.getName()));
//						});
//					});
//				});
//			});
//			return new User(user.getEmail(),  user.getPasswordEncrypt(), authorities);
//		}
//	}
//
//}
