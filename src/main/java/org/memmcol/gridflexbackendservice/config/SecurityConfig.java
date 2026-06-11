package org.memmcol.gridflexbackendservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.components.CustomAccessDeniedHandler;
import org.memmcol.gridflexbackendservice.components.CustomAuthorizationFilter;
import org.memmcol.gridflexbackendservice.components.ThirdPartyAuthenticationFilter;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.perm_evaluator.PermissionEvaluator;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
//import org.memmcol.gridflexbackendservice.thirdPartyService.exception.ThirdPartyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	@Value("${app.base-path}")
	private String basePath;

	@Autowired
	private UserDetailsService userDetailsService;

    @Autowired
    private AuthMapper operatorMapper;

	private AuditRepository auditRepository;

    @Qualifier("hazelcastInstance")
    @Autowired
	private HazelcastInstance hazelcastInstance;

	@Autowired
	private PermissionEvaluator permissionEvaluator;

	@Autowired
	private GenericHandler genericHandler;

	@Autowired
	private ExceptionAuditRepository exceptionAuditRepository;

	@Autowired
	private SafeAuditService safeAuditService;

	@Autowired
	private CustomAccessDeniedHandler accessDeniedHandler;

	@Autowired
	private ObjectMapper objectMapper;

    @Autowired
    ResponseProperties responseProperties;

	@Autowired
	private ThirdPartyAuthenticationFilter thirdPartyAuthenticationFilter;

	@Value("${security.header.key}")
	private String adminHeaderKey;

	@Value("${security.admin.value}")
	private String adminHeaderValue;

	@Value("${security.user.value}")
	private String userHeaderValue;

	@Value("${security.setup.value}")
	private String setupHeaderValue;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return new ProviderManager(provider);
	}

	// ---------- SECURITY CHAIN ----------
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		CustomAuthenticationFilter authFilter =
				new CustomAuthenticationFilter(
						authenticationManager(),
						operatorMapper,
						safeAuditService,
						hazelcastInstance,
						genericHandler,
						objectMapper,
						responseProperties,
						adminHeaderKey,
						adminHeaderValue
				);

		authFilter.setFilterProcessesUrl("/auth/service/admin/login");

		http.csrf(csrf -> csrf.disable());

		http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests(auth -> auth

				// ================= SWAGGER (MUST BE FIRST) =================
				.requestMatchers(
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/v3/api-docs/**",
						"/swagger-resources/**",
						"/webjars/**",
						"/api/**",
						"/v3/api-docs/**",
						"/v3/api-docs/Client Authentication",
						"/v3/api-docs/Odyssey"
				).permitAll()

				// public endpoints - do not required authentication token to be accessible
				.requestMatchers(
						"/auth/service/admin/login",
						"/auth/service/generate-otp/**",
						"/auth/service/forget-password/**",
						"/auth/service/test",
						"/auth/service/logout/**",
						"/actuator/prometheus",
						"/service/alerts",
						"/service/reports/summary",
						"/service/trigger/daily",
						"/service/trigger/monthly",
						"/band/service/clear-cache",
						"/data-collection/schedules/**",
//						"/api/realtime/stream",
						"/meter/service/meterInfo-lookup",
						"/meter/service/readMeter-lookup",
//						"/standard/auth/token",
						"/client/setup",
						"client/**",
						"/client/auth/token",

						"/licence/service/generate-fingerprint",
						"/licence/service/get",
						"/licence/service/deactivate",
						"/licence/service/fingerprint",
						"/licence/service/validate",
						"/licence/service/upload"
//						"/uploads/**"
				).permitAll()
//				.requestMatchers("/v3/api-docs/Admin").hasAuthority("ADMIN")
//				.requestMatchers("/v3/api-docs/admin").hasAuthority("ADMIN")
				// protected endpoints
				.anyRequest().access((authentication, context) -> {
					HttpServletRequest request = context.getRequest();
					Authentication au = authentication.get();

					boolean allowed = permissionEvaluator.checkAccess(request, au);
					return new AuthorizationDecision(allowed);
				})
		);

		http.exceptionHandling(e -> e.accessDeniedHandler(accessDeniedHandler));

		http.addFilter(authFilter);
		http.addFilterBefore(thirdPartyAuthenticationFilter,
				UsernamePasswordAuthenticationFilter.class);

		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new CustomAuthorizationFilter(
				hazelcastInstance), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
