package org.memmcol.gridflexbackendservice.service;

import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.Operator;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
	@Autowired private AuthMapper operatorMapper;

	@Autowired private ResponseProperties status;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Operator user = operatorMapper.findByAuthEmail(email);
		if (user == null) {
			throw new UsernameNotFoundException("Operator "+status.getNotFoundDesc()); 
		} else if(!user.isUstate()) {
			log.info("User is blocked: {}", user.isUstate()); 
			throw new LockedException("User is blocked");
		} else {
			log.info("User found in the database: {}", user.isUstate());

			operatorMapper.updateLoginState(email);
			Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
			user.getRoles().forEach(role -> {
				authorities.add(new SimpleGrantedAuthority(role.getOperatorRole()));
			});
			return new User(user.getEmail(),  user.getPasswordEncrypt(), authorities);
		}
	}

}
