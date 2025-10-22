package org.memmcol.gridflexbackendservice.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> dataManagementDashboard(String band, String year, String meterCategory);

    Map<String, Object> vendingDashboard();

    Map<String, Object> billingDashboard();
}
