package org.memmcol.gridflexbackendservice.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> dataManagementDashboard();

    Map<String, Object> vendingDashboard();

    Map<String, Object> billingDashboard();
}
