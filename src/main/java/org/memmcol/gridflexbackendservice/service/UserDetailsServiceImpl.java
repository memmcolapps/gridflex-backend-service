package org.memmcol.gridflexbackendservice.service;

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

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

	private final AuthMapper authMapper;
	private final ResponseProperties status;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserDTO userDTO = authMapper.findAuthByUserEmail(email);

		if (userDTO == null || userDTO.getUser() == null) {
			throw new UsernameNotFoundException("User " + status.getNotFoundDesc());
		}
		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getUser().getEmail());
		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getGroup().getTitle());
		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getModule().getName());
		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getSubModules().get(0).getSubModule().getName());
		System.out.println(">>>>>>>>>>>>>>>>>>>:: "+userDTO.getGroups().get(0).getModules().get(0).getSubModules().get(0).getPermissions().get(0).getName());
		UserModel user = userDTO.getUser();

		if (!user.getStatus()) {
			log.info("User is blocked: {}", user.getStatus());
			throw new LockedException("User is disabled");
		}

		log.info("User found in the database: {}", user.getEmail());
		authMapper.updateLoginState(email);

		// Convert group/module/submodule/permissions into GrantedAuthorities
		Set<GrantedAuthority> authorities = new HashSet<>();

		if (userDTO.getGroups() != null) {
			for (GroupWithPermissionsDTO groupDTO : userDTO.getGroups()) {
				System.out.println("Get groups with permission: "+groupDTO.getGroup().getTitle());
				if (groupDTO.getModules() != null) {
					for (ModuleWithSubModules moduleDTO : groupDTO.getModules()) {
						System.out.println("module with submodule: "+moduleDTO.getModule().getName());
						if (moduleDTO.getSubModules() != null) {
							for (SubModuleWithPermissions subDTO : moduleDTO.getSubModules()) {
								System.out.println("submodule with permission: "+subDTO.getSubModule().getName());
								if (subDTO.getPermissions() != null) {
									for (Permission permission : subDTO.getPermissions()) {
										System.out.println("permission: "+permission.getName());
										authorities.add(new SimpleGrantedAuthority(permission.getName()));
									}
								}
							}
						}
					}
				}
			}
		}

		return new User(user.getEmail(), user.getPassword(), authorities);
	}
}



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
