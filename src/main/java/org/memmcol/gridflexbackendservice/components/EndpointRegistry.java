package org.memmcol.gridflexbackendservice.components;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EndpointRegistry {

    // There route required permission to be accessible
    private static final Map<String, List<String>> MAPPING = Map.of(
            "METER_MANAGEMENT", List.of("/meter/**", "/manufacturer/service/**",
                    "/band/service/all", "/tariff/service/all", "/node/service/all"),
            "BAND_MANAGEMENT", List.of("/band/**","/tariff/service/all"),
            "TARIFF", List.of("/tariff/**", "/band/service/all"),
            "DEBT_MANAGEMENT", List.of("/debt-setting/**", "/debit-credit-adjustment/**", "/meter/service/all"),
            "CUSTOMER_MANAGEMENT", List.of("/customer/**"),
            "USER_MANAGEMENT", List.of("/user/**", "/node/**"),
            "VENDING", List.of("/vending/**", "/dashboard/service/vending/**"),
            "HES", List.of("/hes/**", "/dashboard/service/hes/**", "/meter/service/all"),
            "ORGANIZATION", List.of("/organization/**", "/node/**")
    );

    public List<String> getEndpoints(String code) {
        if (code == null) return List.of();

        String normalized = code.trim().toUpperCase();

        System.out.println("LOOKUP CODE: " + normalized);

        return MAPPING.getOrDefault(normalized, List.of());
//        return MAPPING.getOrDefault(submodule, List.of());
//        if (submodule == null) return List.of();
//
//        return MAPPING.getOrDefault(submodule.trim().toLowerCase(), List.of());
    }
}

//            "Meter Management", List.of("/meter/**", "/dashboard/service/data-management/**",
//                    "/manufacturer/**", "/audit-log/service"),
//            "Band Management", List.of("/band/**"),
//            "Tariff", List.of("/tariff/**"),
//            "Debt Management", List.of("/debt-setting/**", "/debit-credit-adjustment/**"),
//            "Customer Management", List.of("/customer/**"),
//            "User Management", List.of("/user/**", "/dashboard/service/data-management/**"),
////            "Review and Approval", List.of(""),
//            "Vending", List.of("/vending/**", "/dashboard/service/vending/**", "/audit-log/service"),
//            "HES", List.of("/hes/**", "/dashboard/service/hes/**"),
//            "Organization", List.of("/organization/**", "/node/**")