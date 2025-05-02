package org.memmcol.gridflexbackendservice.config;

import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class  SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

    @Autowired
    private AuthMapper operatorMapper;

	@Autowired
	private AuditRepository auditRepository;

    @Qualifier("hazelcastInstance")
    @Autowired
	private HazelcastInstance hazelcastInstance;

	@Autowired
	private ExceptionAuditRepository exceptionAuditRepository;

	private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@SuppressWarnings("removal")
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		CustomAuthenticationFilter userAuthFilter = new CustomAuthenticationFilter(
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance);
		userAuthFilter.setFilterProcessesUrl("/auth/service/login");

		CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance);
		adminAuthFilter.setFilterProcessesUrl("/auth/service/admin/login");

		// disable csrf
		http.csrf((csrf) -> csrf.disable());

		// header
		http.headers(headers -> headers.contentTypeOptions().disable()// Set X-Content-Type-Options header to nosniff
				.frameOptions().deny() // Set X-Frame-Options header to DENY
				.xssProtection()); // Enable XSS protection

		// Authorization
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(
						"/auth/service/login/**", "/auth/service/admin/login/**", "/auth/service/logout/**", "/auth/service/generate-otp/**","/auth/service/forget-password/**",
						"/band/service/create/**", "/band/service/update/**", "/band/service/all-band/**", "/band/service/single-band/**", "/band/service/change-state/**",
						"/tariff/service/create/**", "/tariff/service/all-tariff/**", "/tariff/service/single-tariff/**", "/tariff/service/change-state/**", "/tariff/service/filter-tariff/**", "/tariff/service/filter/unique-id/**"
						).permitAll()
				.requestMatchers("/service/**").hasAuthority("ROLE_WRITE")
				.anyRequest().authenticated());

//		http.addFilter(customAuthenticationFilter);
		http.addFilter(userAuthFilter);
		http.addFilter(adminAuthFilter);
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new CustomAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(authenticationProvider);
	}

}
