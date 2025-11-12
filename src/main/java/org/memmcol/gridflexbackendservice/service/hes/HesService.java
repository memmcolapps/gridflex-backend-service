package org.memmcol.gridflexbackendservice.service.hes;


import java.util.Map;

public interface HesService {
    Map<String, Object> dashboard();

    Map<String, Object> communicationReport(int page, int size, String type, String search);
}
