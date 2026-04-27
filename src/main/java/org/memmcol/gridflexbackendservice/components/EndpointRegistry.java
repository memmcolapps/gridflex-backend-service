package org.memmcol.gridflexbackendservice.components;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EndpointRegistry {

    private static final Map<String, List<String>> MAPPING = Map.of(
            "METER_MANAGEMENT", List.of("/meter/**", "/dashboard/service/data-management/**"),
            "BAND_MANAGEMENT", List.of("/band/**"),
            "TARIFF", List.of("/tariff/**"),
            "DEBT_MANAGEMENT", List.of("/debt-setting/**", "/debit-credit-adjustment/**"),
            "CUSTOMER_MANAGEMENT", List.of("/customer/**"),
            "USER_MANAGEMENT", List.of("/user/**"),
            "VENDING", List.of("/vending/**", "/dashboard/service/vending/**"),
            "HES", List.of("/hes/**", "/dashboard/service/hes/**"),
            "ORGANIZATION", List.of("/organization/**", "/node/**")
//            "/audit-log/service/**",
//            "/dashboard/service/data-management/**"

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