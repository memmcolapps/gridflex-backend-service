package org.memmcol.gridflexbackendservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GridFlex API")
                        .version("1.0"));
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
    @Bean
//    @Profile("staging")
    public GroupedOpenApi stagingPublicApis() {
        return GroupedOpenApi.builder()
                .group("public-apis")
                .pathsToMatch(
                        "/meter/**",
                        "/dashboard/**",
                        "/audit-log/**",
                        "/auth/**",
                        "/billing/**",
                        "/customer/**",
                        "/band/**",
                        "/tariff/",
                        "/debit-credit-adjustment/**",
                        "/debt-setting/**",
                        "/hes/**",
                        "/manufacturer/**",
                        "/node/**",
                        "/organization/**",
                        "/user/**",
                        "/vending/**"
                )
                .build();
    }

    @Bean
//    @Profile("staging")
    public GroupedOpenApi stagingOdysseyApis() {
        return GroupedOpenApi.builder()
                .group("odyssey-apis")
                .pathsToMatch(
                        "/client/auth/**",
                        "/odyssey/**")
                .build();
    }

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
