package org.memmcol.gridflexbackendservice.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> dataManagementDashboard(String band, String year, String meterCategory);

    Map<String, Object> vendingDashboard(String band, String year, String meterClass);

    Map<String, Object> billingDashboard();

    Map<String, Object> hesDashboard();

    ;
}
