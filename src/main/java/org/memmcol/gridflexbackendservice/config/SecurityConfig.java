package org.memmcol.gridflexbackendservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.components.CustomAccessDeniedHandler;
import org.memmcol.gridflexbackendservice.components.CustomAuthorizationFilter;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.service.perm_evaluator.PermissionEvaluator;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
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
	private PermissionEvaluator permissionEvaluator;

	@Autowired
	private GenericHandler genericHandler;

	@Autowired
	private ExceptionAuditRepository exceptionAuditRepository;

	@Autowired
	private CustomAccessDeniedHandler customAccessDeniedHandler;

	@Autowired
	private ObjectMapper objectMapper;

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

		CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance, genericHandler, objectMapper);
		adminAuthFilter.setFilterProcessesUrl("/auth/service/admin/login");

		// disable csrf
		http.csrf((csrf) -> csrf.disable());

		// header
		http.headers(headers -> headers.contentTypeOptions().disable()// Set X-Content-Type-Options header to nosniff
				.frameOptions().deny() // Set X-Frame-Options header to DENY
				.xssProtection()); // Enable XSS protection

		 // Authorization
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/auth/service/admin/login/**", "/auth/service/logout/**", "/auth/service/generate-otp/**",
						"/auth/service/forget-password/**", "/actuator/prometheus", "/service/alerts", "/service/reports/summary",
						"/service/trigger/daily", "/service/trigger/monthly", "/band/service/clear-cache"
				).permitAll()
				.requestMatchers("/band/service/create", "/band/service/update", "/band/service/change-state",
						"/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/export",
						"/tariff/service/all", "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve",
						"/user/service/single-user", "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create", "/user/service/group/update",
						"/user/service/groups",  "/user/service/create/group-permission", "/user/service/update/group-permission", "/user/service/group/change-state",
						"/customer/service/create", "/customer/service/update", "/customer/service/change-state", "/customer/service/download/template/excel",
						"/customer/service/all",  "/customer/service/single", "/customer/service/bulk-upload", "/customer/service/download/template/csv",
						"/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
						"/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
						"/node/service/single", "/node/service/all", "/manufacturer/service/create", "/manufacturer/service/update", "/node/service/businessHub",
						"/manufacturer/service/single", "/manufacturer/service/all", "/audit-log/service/all", "/audit-log/service/incident/report",
						"/audit-log/service/single","/audit-log/service/incident/report/get","/debit-credit-adjustment/service/create",
						"/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept", "/debit-credit-adjustment/service/all",
						"/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
						"/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve",
						"/debt-setting/service/percentage-range/create", "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/bulk-approve",
						"/debt-setting/service/liability-cause/bulk-approve",
						"/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve",
						"/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "/meter/service/change-state", "/meter/service/approve","/meter/service/bulk-approve",
						"/meter/service/migrate", "/meter/service/manufacturers", "/meter/service/assign", "/meter/service/cin/assign", "meter/service/customer", "meter/service/bulk-upload",
						"meter/service/allocate", "meter/service/detach","/billing/service/meter/reading/service/create","/billing/service/meter/reading/service/generate", "/billing/service/meter/reading/service/update",
						"/billing/service/meter/reading/service/all", "/billing/service/meter/reading/service/download/template/csv", "/billing/service/meter/reading/service/download/template/excel",
						"/billing/service/meter/reading/service/bulk-upload", "/meter/service/download/allocate/template/excel", "/meter/service/download/allocate/template/csv", "/meter/service/download/assign/template/excel",
						"/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
						"/meter/service/download/assign/template/csv", "/meter/service/download/template/excel", "/meter/service/download/template/csv", "/meter/service/virtual/export", "/meter/service/export",
						"/vending/service/generate/token/credit", "/vending/service/generate/token/credit/calculate", "/vending/service/generate/kct", "/vending/service/generate/meter-kct",
						"/vending/service/generate/token/kct-clear-tamper", "/vending/service/generate/token/clear-credit", "/vending/service/generate/token/clear-tamper",
						"/vending/service/generate/token/compensation", "/vending/service/generate/token/all", "/vending/service/generate/token/print", "/dashboard/service/data-management",
						"/dashboard/service/billing-management", "/dashboard/service/vending", "/hes/service/dashboard"
						)
				.access((context, authSupplier) -> {
					// Get the Authentication object from the Supplier"/customer/service/download/template/csv",
					Authentication authentication =  context.get();

					// Check if authentication is present and valid
					if (authentication != null && authentication.isAuthenticated()) {
						HttpServletRequest request = authSupplier.getRequest();

						// Use your custom permission evaluator to check access
						boolean accessGranted = permissionEvaluator.checkAccess(request, authentication);

						// Return AuthorizationDecision based on the result of your custom logic
						return new AuthorizationDecision(accessGranted);
					}

					// If authentication is not valid, deny access
					return new AuthorizationDecision(false);
				})
				.anyRequest().authenticated()
		).exceptionHandling(ex -> ex
				.accessDeniedHandler(customAccessDeniedHandler)
		);


//		http.addFilter(customAuthenticationFilter);
//		http.addFilter(userAuthFilter);
		http.addFilter(adminAuthFilter);
//		http.addFilter(portalAuthFilter);
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new CustomAuthorizationFilter(hazelcastInstance), UsernamePasswordAuthenticationFilter.class);

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
