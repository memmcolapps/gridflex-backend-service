package org.memmcol.gridflexbackendservice.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> dataManagementDashboard(String band, String year, String meterClass);

    Map<String, Object> vendingDashboard();

    Map<String, Object> billingDashboard();
}
