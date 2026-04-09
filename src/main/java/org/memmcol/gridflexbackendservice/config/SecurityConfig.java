package org.memmcol.gridflexbackendservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.components.CustomAccessDeniedHandler;
import org.memmcol.gridflexbackendservice.components.CustomAuthorizationFilter;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.perm_evaluator.PermissionEvaluator;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
	private CustomAccessDeniedHandler customAccessDeniedHandler;

	@Autowired
	private ObjectMapper objectMapper;

    @Autowired
    ResponseProperties responseProperties;

	private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
//                authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance, genericHandler, objectMapper,responseProperties);
//        adminAuthFilter.setFilterProcessesUrl(basePath + "/auth/service/admin/login");
//
//        // disable csrf
//        http.csrf((csrf) -> csrf.disable());
//
//        // header
//        http
//                .headers(headers -> headers
//                                // contentTypeOptions is enabled by default
//                                .frameOptions(frameOptions -> frameOptions.sameOrigin())
//                        // ... other header configurations
//                );
//
//        // Authorization
//        http.authorizeHttpRequests((authorize) -> authorize
//                .requestMatchers(
//                        basePath + "/auth/service/admin/login/**",
//                        basePath + "/auth/service/logout/**",
//                        basePath + "/auth/service/generate-otp/**",
//                        basePath + "/auth/service/forget-password/**",
//                        basePath + "/actuator/prometheus",
//                        basePath + "/service/alerts",
//                        basePath + "/service/reports/summary",
//                        basePath + "/service/trigger/daily",
//                        basePath + "/service/trigger/monthly",
//                        basePath + "/band/service/clear-cache"
//                ).permitAll()
//                .requestMatchers(
//                        basePath + "/band/service/create",
//                        basePath + "/band/service/update",
//                        basePath + "/band/service/change-state",
//                        basePath + "/band/service/all",
//                        basePath + "/band/service/single",
//                        basePath + "/tariff/service/single",
//                        basePath + "/tariff/service/export",
//                        basePath + "/tariff/service/all",
//                        basePath + "/tariff/service/create",
//                        basePath + "/tariff/service/change-state",
//                        basePath + "/tariff/service/bulk-approve",
//                        basePath + "/user/service/single-user",
//                        basePath + "/user/service/all",
//                        basePath + "/user/service/change-state",
//                        basePath + "/user/service/update",
//                        basePath + "/user/service/create",
//                        basePath + "/user/service/group/update",
//                        basePath + "/user/service/groups",
//                        basePath + "/user/service/create/group-permission",
//                        basePath + "/user/service/update/group-permission",
//                        basePath + "/user/service/group/change-state",
//                        basePath + "/customer/service/create",
//                        basePath + "/customer/service/update",
//                        basePath + "/customer/service/change-state",
//                        basePath + "/customer/service/download/template/excel",
//                        basePath + "/customer/service/all",
//                        basePath + "/customer/service/single",
//                        basePath + "/customer/service/bulk-upload",
//                        basePath + "/customer/service/download/template/csv",
//                        basePath + "/node/service/create/node/region-bhub-service-center",
//                        basePath + "/node/service/update/node/region-bhub-service-center",
//                        basePath + "/node/service/create/node/substation-transformer-feeder-line",
//                        basePath + "/node/service/update/node/substation-transformer-feeder-line",
//                        basePath + "/node/service/single",
//                        basePath + "/node/service/all",
//                        basePath + "/manufacturer/service/create",
//                        basePath + "/manufacturer/service/update",
//                        basePath + "/node/service/businessHub",
//                        basePath + "/manufacturer/service/single",
//                        basePath + "/manufacturer/service/all",
//                        basePath + "/audit-log/service/all",
//                        basePath + "/audit-log/service/incident/report",
//                        basePath + "/audit-log/service/single",
//                        basePath + "/audit-log/service/incident/report/get",
//                        basePath + "/debit-credit-adjustment/service/create",
//                        basePath + "/debit-credit-adjustment/service/meter-liability",
//                        basePath + "/debit-credit-adjustment/service/reconcile-dept",
//                        basePath + "/debit-credit-adjustment/service/all",
//                        basePath + "/debit-credit-adjustment/service/single",
//                        basePath + "/debt-setting/service/liability-cause/create",
//                        basePath + "/debt-setting/service/liability-cause/update",
//                        basePath + "/debt-setting/service/liability-cause/all",
//                        basePath + "/debt-setting/service/liability-cause/single",
//                        basePath + "/debt-setting/service/liability-cause/approve",
//                        basePath + "/debt-setting/service/percentage-range/create",
//                        basePath + "/debt-setting/service/percentage-range/update",
//                        basePath + "/debt-setting/service/percentage-range/bulk-approve",
//                        basePath + "/debt-setting/service/liability-cause/bulk-approve",
//                        basePath + "/debt-setting/service/percentage-range/all",
//                        basePath + "/debt-setting/service/percentage-range/single",
//                        basePath + "/debt-setting/service/percentage-range/approve",
//                        basePath + "/meter/service/create",
//                        basePath + "/meter/service/update",
//                        basePath + "/meter/service/all",
//                        basePath + "/meter/service/single",
//                        basePath + "/meter/service/change-state",
//                        basePath + "/meter/service/approve",
//                        basePath + "/meter/service/bulk-approve",
//                        basePath + "/meter/service/migrate",
//                        basePath + "/meter/service/manufacturers",
//                        basePath + "/meter/service/assign",
//                        basePath + "/meter/service/cin/assign",
//                        basePath + "/meter/service/customer",
//                        basePath + "/meter/service/bulk-upload",
//                        basePath + "/meter/service/allocate",
//                        basePath + "/meter/service/detach",
//                        basePath + "/billing/service/meter/reading/create",
//                        basePath + "/billing/service/meter/reading/generate",
//                        basePath + "/billing/service/meter/reading/update",
//                        basePath + "/billing/service/meter/reading/all",
//                        basePath + "/billing/service/meter/reading/download/template/csv",
//                        basePath + "/billing/service/meter/reading/download/template/excel",
//                        basePath + "/billing/service/meter/consumption",
//                        basePath + "/billing/service/meter/consumption/all",
//                        basePath + "/billing/service/virtual/md-meter/energy/import",
//                        basePath + "/billing/service/virtual/md-meter/energy/import/assetId/all",
//						basePath + "/billing/service/virtual/non-md-meter/energy/import/assetId/all",
//						basePath + "/billing/service/feeder/reading/create",
//						basePath + "/billing/service/feeder/reading/update",
//						basePath + "/billing/service/feeder/overall/consumption",
//                        basePath + "/billing/service/meter/reading/bulk-upload",
//                        basePath + "/meter/service/download/allocate/template/excel",
//                        basePath + "/meter/service/download/allocate/template/csv",
//                        basePath + "/meter/service/download/assign/template/excel",
//                        basePath + "/meter/service/download/v-assign/template/excel",
//                        basePath + "/meter/service/download/assign/template/csv",
//                        basePath + "/meter/service/download/template/excel",
//                        basePath + "/meter/service/download/template/csv",
//                        basePath + "/meter/service/virtual/export",
//                        basePath + "/meter/service/export",
//                        basePath + "/vending/service/generate/token/credit",
//                        basePath + "/vending/service/generate/token/credit/calculate",
//                        basePath + "/vending/service/generate/kct",
//                        basePath + "/vending/service/generate/meter-kct",
//                        basePath + "/vending/service/generate/token/kct-clear-tamper",
//                        basePath + "/vending/service/generate/token/clear-credit",
//                        basePath + "/vending/service/generate/token/clear-tamper",
//                        basePath + "/vending/service/generate/token/compensation",
//                        basePath + "/vending/service/generate/token/all",
//                        basePath + "/vending/service/generate/token/print",
//                        basePath + "/dashboard/service/data-management",
//                        basePath + "/dashboard/service/billing",
//                        basePath + "/dashboard/service/vending",
//                        basePath + "/dashboard/service/hes",
//                        basePath + "/hes/service/communication/report",
//                        basePath + "/hes/service/event",
//                        basePath + "/hes/service/profile",
//                        basePath + "/hes/service/model",
//                        basePath + "/hes/service/communication/range/report",
//                        basePath + "/hes/service/meter-status/stream",
//                        basePath + "/hes/service/stream",
//                        basePath + "/hes/service/data/schedule"
//                )
//                .access((context, authSupplier) -> {
//                    Authentication authentication =  context.get();
//
//                    if (authentication != null && authentication.isAuthenticated()) {
//                        HttpServletRequest request = authSupplier.getRequest();
//                        boolean accessGranted = permissionEvaluator.checkAccess(request, authentication);
//                        return new AuthorizationDecision(accessGranted);
//                    }
//
//                    return new AuthorizationDecision(false);
//                })
//                .anyRequest().authenticated()
//        ).exceptionHandling(ex -> ex
//                .accessDeniedHandler(customAccessDeniedHandler)
//        );
//
//        http.addFilter(adminAuthFilter);
//        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//        http.addFilterBefore(new CustomAuthorizationFilter(hazelcastInstance), UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }

	@SuppressWarnings("removal")
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, safeAuditService, hazelcastInstance, genericHandler, objectMapper, responseProperties);
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
						basePath + "/auth/service/test",
						basePath + "/auth/service/admin/login/**",
						basePath + "/auth/service/logout/**",
						basePath + "/auth/service/generate-otp/**",
						basePath + "/auth/service/forget-password/**",
						basePath + "/actuator/prometheus",
						basePath + "/service/alerts",
						basePath + "/service/reports/summary",
						basePath + "/service/trigger/daily",
						basePath + "/service/trigger/monthly",
						basePath + "/band/service/clear-cache",
						basePath + "/data-collection/schedules/**"
				).permitAll()
				.requestMatchers(
						basePath + "/band/service/create",
						basePath + "/band/service/update",
						basePath + "/band/service/change-state",
						basePath + "/band/service/all",
						basePath + "/band/service/single",
						basePath + "/tariff/service/single",
						basePath + "/tariff/service/export",
						basePath + "/tariff/service/all",
						basePath + "/tariff/service/create",
						basePath + "/tariff/service/change-state",
						basePath + "/tariff/service/bulk-approve",
						basePath + "/user/service/single-user",
						basePath + "/user/service/all",
						basePath + "/user/service/change-state",
						basePath + "/user/service/update",
						basePath + "/user/service/create",
						basePath + "/user/service/group/update",
						basePath + "/user/service/groups",
						basePath + "/user/service/create/group-permission",
						basePath + "/user/service/update/group-permission",
						basePath + "/user/service/group/change-state",
						basePath + "/customer/service/create",
						basePath + "/customer/service/update",
						basePath + "/customer/service/change-state",
						basePath + "/customer/service/download/template/excel",
						basePath + "/customer/service/all",
						basePath + "/customer/service/single",
						basePath + "/customer/service/bulk-upload",
						basePath + "/customer/service/download/template/csv",
						basePath + "/node/service/create/node/region-bhub-service-center",
						basePath + "/node/service/update/node/region-bhub-service-center",
						basePath + "/node/service/create/node/substation-transformer-feeder-line",
						basePath + "/node/service/update/node/substation-transformer-feeder-line",
						basePath + "/node/service/single",
						basePath + "/node/service/all",
						basePath + "/manufacturer/service/create",
						basePath + "/manufacturer/service/update",
						basePath + "/node/service/businessHub",
						basePath + "/manufacturer/service/single",
						basePath + "/manufacturer/service/all",
						basePath + "/audit-log/service/all",
						basePath + "/audit-log/service/incident/report",
						basePath + "/audit-log/service/single",
						basePath + "/audit-log/service/incident/report/get",
						basePath + "/debit-credit-adjustment/service/create",
						basePath + "/debit-credit-adjustment/service/meter-liability",
						basePath + "/debit-credit-adjustment/service/reconcile-dept",
						basePath + "/debit-credit-adjustment/service/all",
						basePath + "/debit-credit-adjustment/service/single",
						basePath + "/debit-credit-adjustment/download/template/excel",
						basePath + "/debit-credit-adjustment/download/template/csv",
						basePath + "/debit-credit-adjustment/bulk-upload",
						basePath + "/debt-setting/service/liability-cause/create",
						basePath + "/debt-setting/service/liability-cause/update",
						basePath + "/debt-setting/service/liability-cause/all",
						basePath + "/debt-setting/service/liability-cause/single",
						basePath + "/debt-setting/service/liability-cause/approve",
						basePath + "/debt-setting/service/percentage-range/create",
						basePath + "/debt-setting/service/percentage-range/update",
						basePath + "/debt-setting/service/percentage-range/bulk-approve",
						basePath + "/debt-setting/service/liability-cause/bulk-approve",
						basePath + "/debt-setting/service/percentage-range/all",
						basePath + "/debt-setting/service/percentage-range/single",
						basePath + "/debt-setting/service/percentage-range/approve",
						basePath + "/meter/service/create",
						basePath + "/meter/service/update",
						basePath + "/meter/service/all",
						basePath + "/meter/service/single",
						basePath + "/meter/service/change-state",
						basePath + "/meter/service/approve",
						basePath + "/meter/service/bulk-approve",
						basePath + "/meter/service/migrate",
						basePath + "/meter/service/manufacturers",
						basePath + "/meter/service/assign",
						basePath + "/meter/service/cin/assign",
						basePath + "/meter/service/customer",
						basePath + "/meter/service/bulk-upload",
						basePath + "/meter/service/allocate",
						basePath + "/meter/service/detach",
						basePath + "/billing/service/meter/reading/create",
						basePath + "/billing/service/meter/reading/generate",
						basePath + "/billing/service/meter/reading/update",
						basePath + "/billing/service/meter/reading/all",
						basePath + "/billing/service/meter/reading/download/template/csv",
						basePath + "/billing/service/meter/reading/download/template/excel",
						basePath + "/billing/service/meter/consumption",
						basePath + "/billing/service/meter/consumption/all",
						basePath + "/billing/service/virtual/md-meter/energy/import",
						basePath + "/billing/service/virtual/md-meter/energy/import/assetId/all",
						basePath + "/billing/service/virtual/non-md-meter/energy/import/assetId/all",
						basePath + "/billing/service/feeder/reading/create",
						basePath + "/billing/service/feeder/reading/update",
						basePath + "/billing/service/feeder/overall/consumption",
						basePath + "/billing/service/meter/reading/bulk-upload",
						basePath + "/meter/service/download/allocate/template/excel",
						basePath + "/meter/service/download/allocate/template/csv",
						basePath + "/meter/service/download/assign/template/excel",
						basePath + "/meter/service/download/v-assign/template/excel",
						basePath + "/meter/service/download/assign/template/csv",
						basePath + "/meter/service/download/template/excel",
						basePath + "/meter/service/download/template/csv",
						basePath + "/meter/service/virtual/export",
						basePath + "/meter/service/export",
						basePath + "/vending/service/generate/token/credit",
						basePath + "/vending/service/generate/token/credit/calculate",
						basePath + "/vending/service/generate/kct",
						basePath + "/vending/service/generate/meter-kct",
						basePath + "/vending/service/generate/token/kct-clear-tamper",
						basePath + "/vending/service/generate/token/clear-credit",
						basePath + "/vending/service/generate/token/clear-tamper",
						basePath + "/vending/service/generate/token/compensation",
						basePath + "/vending/service/generate/token/all",
						basePath + "/vending/service/generate/token/print",
						basePath + "/dashboard/service/data-management",
						basePath + "/dashboard/service/billing",
						basePath + "/dashboard/service/vending",
						basePath + "/dashboard/service/hes",
						basePath + "/hes/service/communication/report",
						basePath + "/hes/service/event",
						basePath + "/hes/service/profile",
						basePath + "/hes/service/model",
						basePath + "/hes/service/communication/range/report",
						basePath + "/hes/service/meter-status/stream",
						basePath + "/hes/service/stream",
						basePath + "/hes/service/data/schedule",
						basePath + "/hes/service/set/schedule",
						basePath + "/hes/service/set/cron",
						basePath + "/hes/service/profile-events",
						basePath + "/hes/service/dlms/set-clock",
						basePath + "/hes/service/dlms/set-ctpt",
						basePath + "/hes/service/dlms/set-apn",
						basePath + "/hes/service/dlms/set-ip-port",
						basePath +"/hes/service/meter-configuration"
				)
				.access((context, authSupplier) -> {
					Authentication authentication =  context.get();

					if (authentication != null && authentication.isAuthenticated()) {
						HttpServletRequest request = authSupplier.getRequest();
						boolean accessGranted = permissionEvaluator.checkAccess(request, authentication);
						return new AuthorizationDecision(accessGranted);
					}

					return new AuthorizationDecision(false);
				})
				.anyRequest().authenticated()
		).exceptionHandling(ex -> ex
				.accessDeniedHandler(customAccessDeniedHandler)
		);
//		 // Authorization
//		http.authorizeHttpRequests((authorize) -> authorize
//				.requestMatchers("/auth/service/admin/login/**", "/auth/service/logout/**", "/auth/service/generate-otp/**",
//						"/auth/service/forget-password/**", "/actuator/prometheus", "/service/alerts", "/service/reports/summary",
//						"/service/trigger/daily", "/service/trigger/monthly", "/band/service/clear-cache"
//				).permitAll()
//				.requestMatchers("/band/service/create", "/band/service/update", "/band/service/change-state",
//						"/band/service/all",  "/band/service/single", "/tariff/service/single", "/tariff/service/export",
//						"/tariff/service/all", "/tariff/service/create", "/tariff/service/change-state", "/tariff/service/bulk-approve",
//						"/user/service/single-user", "/user/service/all", "/user/service/change-state", "/user/service/update",  "/user/service/create", "/user/service/group/update",
//						"/user/service/groups",  "/user/service/create/group-permission", "/user/service/update/group-permission", "/user/service/group/change-state",
//						"/customer/service/create", "/customer/service/update", "/customer/service/change-state", "/customer/service/download/template/excel",
//						"/customer/service/all",  "/customer/service/single", "/customer/service/bulk-upload", "/customer/service/download/template/csv",
//						"/node/service/create/node/region-bhub-service-center", "/node/service/update/node/region-bhub-service-center",
//						"/node/service/create/node/substation-transformer-feeder-line", "/node/service/update/node/substation-transformer-feeder-line",
//						"/node/service/single", "/node/service/all", "/manufacturer/service/create", "/manufacturer/service/update", "/node/service/businessHub",
//						"/manufacturer/service/single", "/manufacturer/service/all", "/audit-log/service/all", "/audit-log/service/incident/report",
//						"/audit-log/service/single","/audit-log/service/incident/report/get","/debit-credit-adjustment/service/create",
//						"/debit-credit-adjustment/service/meter-liability", "/debit-credit-adjustment/service/reconcile-dept", "/debit-credit-adjustment/service/all",
//						"/debit-credit-adjustment/service/single", "/debt-setting/service/liability-cause/create", "/debt-setting/service/liability-cause/update",
//						"/debt-setting/service/liability-cause/all", "/debt-setting/service/liability-cause/single", "/debt-setting/service/liability-cause/approve",
//						"/debt-setting/service/percentage-range/create", "/debt-setting/service/percentage-range/update", "/debt-setting/service/percentage-range/bulk-approve",
//						"/debt-setting/service/liability-cause/bulk-approve",
//						"/debt-setting/service/percentage-range/all", "/debt-setting/service/percentage-range/single", "/debt-setting/service/percentage-range/approve",
//						"/meter/service/create", "/meter/service/update", "/meter/service/all", "/meter/service/single", "/meter/service/change-state", "/meter/service/approve","/meter/service/bulk-approve",
//						"/meter/service/migrate", "/meter/service/manufacturers", "/meter/service/assign", "/meter/service/cin/assign", "/meter/service/customer", "/meter/service/bulk-upload",
//						"/meter/service/allocate", "meter/service/detach","/billing/service/meter/reading/create","/billing/service/meter/reading/generate", "/billing/service/meter/reading/update",
//						"/billing/service/meter/reading/all", "/billing/service/meter/reading/download/template/csv", "/billing/service/meter/reading/download/template/excel","/billing/service/meter/consumption",
//						"/billing/service/meter/consumption/all", "/billing/service/virtual/md-meter/energy/import", "/billing/service/virtual/md-meter/energy/import/assetId/all", "/virtual/non-md-meter/energy/import/assetId/all",
//						"/billing/service/virtual/non-md-meter/energy/import", "/billing/service/meter/reading/bulk-upload", "/billing/service/feeder/reading/create", "/billing/service/feeder/reading/update",
//						"/billing/service/feeder/overall/consumption",
//						"/meter/service/download/allocate/template/excel", "/meter/service/download/allocate/template/csv", "/meter/service/download/assign/template/excel",
//						"/meter/service/download/v-assign/template/excel", "/meter/service/download/v-assign/template/excel",
//						"/meter/service/download/assign/template/csv", "/meter/service/download/template/excel", "/meter/service/download/template/csv", "/meter/service/virtual/export", "/meter/service/export",
//						"/vending/service/generate/token/credit", "/vending/service/generate/token/credit/calculate", "/vending/service/generate/kct", "/vending/service/generate/meter-kct",
//						"/vending/service/generate/token/kct-clear-tamper", "/vending/service/generate/token/clear-credit", "/vending/service/generate/token/clear-tamper",
//						"/vending/service/generate/token/compensation", "/vending/service/generate/token/all", "/vending/service/generate/token/print", "/dashboard/service/data-management",
//						"/dashboard/service/billing", "/dashboard/service/vending", "/dashboard/service/hes", "/hes/service/communication/report", "/hes/service/event", "/hes/service/profile",
//						"/hes/service/model", "/hes/service/communication/range/report","/hes/service/meter-status/stream", "/hes/service/stream", "/hes/service/data/schedule"
//				)
//				.access((context, authSupplier) -> {
//					// Get the Authentication object from the Supplier"/customer/service/download/template/csv",
//					Authentication authentication =  context.get();
//
//					// Check if authentication is present and valid
//					if (authentication != null && authentication.isAuthenticated()) {
//						HttpServletRequest request = authSupplier.getRequest();
//
//						// Use your custom permission evaluator to check access
//						boolean accessGranted = permissionEvaluator.checkAccess(request, authentication);
//
//						// Return AuthorizationDecision based on the result of your custom logic
//						return new AuthorizationDecision(accessGranted);
//					}
//
//					// If authentication is not valid, deny access
//					return new AuthorizationDecision(false);
//				})
//				.anyRequest().authenticated()
//		).exceptionHandling(ex -> ex
//				.accessDeniedHandler(customAccessDeniedHandler)
//		);


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
