package org.memmcol.gridflexbackendservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
public class SwaggerConfig {


    @Value("${security.header.key}")
    private String apiHeaderKey;

    @Bean
    public OpenAPI baseOpenAPI() {
        System.out.println("header-key: " + apiHeaderKey);
        return new OpenAPI()
                .info(new Info()
                        .title("GridFlex API")
                        .version("1.0"))
                .servers(List.of(
                        new Server().url("https://sbctest.memmserve.com/api/v1")
                ))
                // ================= SECURITY SCHEMES =================
                .components(new Components()

                        // API KEY (for /client/auth/**)
                        .addSecuritySchemes("apiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(apiHeaderKey)
                        )

                        // JWT (for all other secured APIs)
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

    @Bean
//    @Profile("staging")

    public GroupedOpenApi stagingOdysseyApis() {
        return GroupedOpenApi.builder()
                .group("Odyssey")
                .pathsToMatch(
//                        "/client/auth/**",
                        "/odyssey/**")
                .addOpenApiCustomizer(openApi ->
                        openApi.addSecurityItem(
                                new SecurityRequirement().addList("bearerAuth")
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi stagingClientApis() {
        return GroupedOpenApi.builder()
                .group("Client Authentication")
                .pathsToMatch(
                        "/client/auth/**")
                .addOpenApiCustomizer(openApi ->
                        openApi.addSecurityItem(
                                new SecurityRequirement().addList("apiKeyAuth")
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch(
                        "/client/setup/**",
                        "/service/**",
                        "/api/licence/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.setSecurity(List.of(
                            new SecurityRequirement().addList("bearerAuth")
                    ));
                })
                .build();
    }

    // ================= DEV (FULL ACCESS) =================
//    @Bean
//    @Profile("dev")
//    public GroupedOpenApi devAllApis() {
//        return GroupedOpenApi.builder()
//                .group("all-apis")
//                .pathsToMatch("/**")
//                .build();
//    }

//    @Bean
////    @Profile("dev")
//    public GroupedOpenApi devAdminApis() {
//        return GroupedOpenApi.builder()
//                .group("admin-apis")
//                .pathsToMatch(
//                        "/client/**",
//                        "/service/**",
//                        "/api/licence/**")
//                .build();
//    }

//    @Bean
////    @Profile("dev")
//    public GroupedOpenApi devOdysseyApis() {
//        return GroupedOpenApi.builder()
//                .group("odyssey-apis")
//                .pathsToMatch(
//                        "/client/auth/**",
//                        "/odyssey/**")
//                .build();
//    }

    // ================= STAGING (LIMITED ACCESS) =================
//    @Bean
////    @Profile("staging")
//    public GroupedOpenApi stagingPublicApis() {
//        return GroupedOpenApi.builder()
//                .group("public-apis")
//                .pathsToMatch(
//                        "/meter/**",
//                        "/dashboard/**",
//                        "/audit-log/**",
//                        "/auth/**",
//                        "/billing/**",
//                        "/customer/**",
//                        "/band/**",
//                        "/tariff/",
//                        "/debit-credit-adjustment/**",
//                        "/debt-setting/**",
//                        "/hes/**",
//                        "/manufacturer/**",
//                        "/node/**",
//                        "/organization/**",
//                        "/user/**",
//                        "/vending/**"
//                )
//                .build();
//    }

//    @Bean
//    @Profile("staging")
//    public GroupedOpenApi stagingAdminApis() {
//        return GroupedOpenApi.builder()
//                .group("admin-apis")
//                .pathsToMatch(
//                        "/client/**",
//                        "/service/**",
//                        "/api/licence/**")
//                .build();
//    }

    // ================= PROD (OPTIONAL: ONLY PUBLIC OR NONE) =================
//    @Bean
//    @Profile("prod")
//    public GroupedOpenApi prodPublicApis() {
//        return GroupedOpenApi.builder()
//                .group("public-apis")
//                .pathsToMatch(
//                        "/meter/**",
//                        "/dashboard/**",
//                        "/audit-log/**",
//                        "/auth/**",
//                        "/billing/**",
//                        "/customer/**",
//                        "/band/**",
//                        "/tariff/",
//                        "/debit-credit-adjustment/**",
//                        "/debt-setting/**",
//                        "/hes/**",
//                        "/manufacturer/**",
//                        "/node/**",
//                        "/organization/**",
//                        "/user/**",
//                        "/vending/**"
//                )
//                .build();
//    }
//
//    @Bean
//    @Profile("prod")
//    public GroupedOpenApi prodOdysseyApis() {
//        return GroupedOpenApi.builder()
//                .group("odyssey-apis")
//                .pathsToMatch(
//                        "/client/auth/**",
//                        "/odyssey/**")
//                .build();
//    }

//    @Bean
//    @Profile("prod")
//    public GroupedOpenApi prodAdminApis() {
//        return GroupedOpenApi.builder()
//                .group("admin-apis")
//                .pathsToMatch(
//                        "/client/**",
//                        "/service/**",
//                        "/api/licence/**")
//                .build();
//    }
}
