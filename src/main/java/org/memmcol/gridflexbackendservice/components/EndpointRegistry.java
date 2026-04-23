package org.memmcol.gridflexbackendservice.components;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EndpointRegistry {

    private static final Map<String, List<String>> MAPPING = Map.of(
            "Meter Management", List.of("/meter/**", "/dashboard/service/data-management/**",
                    "/manufacturer/**"),
            "Band Management", List.of("/band/**"),
            "Tariff", List.of("/tariff/**"),
            "Debt Management", List.of("/debt-setting/**", "/debit-credit-adjustment/**"),
            "Customer Management", List.of("/customer/**"),
            "User Management", List.of("/user/**"),
//            "Review and Approval", List.of(""),
            "Vending", List.of("/vending/**", "/dashboard/service/vending/**"),
            "HES", List.of("/hes/**", "/dashboard/service/hes/**"),
            "Organization", List.of("/organization/**", "/node/**")
    );

    public List<String> getEndpoints(String submodule) {
        return MAPPING.getOrDefault(submodule, List.of());
    }
}
