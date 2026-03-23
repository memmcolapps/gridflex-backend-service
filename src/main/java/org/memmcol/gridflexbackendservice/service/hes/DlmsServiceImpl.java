package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.model.hes.DlmsBulkRequest;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DlmsServiceImpl implements DlmsService {

    @Autowired
    private ResponseProperties status;

    @Qualifier("dlmsWriteOpsClient")
    @Autowired
    private WebClient dlmsWriteOpsClient;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private HesAuthServiceImpl auth;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> setClock(DlmsBulkRequest.SetClockRequest request) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : request.getSerials()) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setClock")
                                .queryParam("serial", serial)
                                .queryParam("dateTime", request.getDateTime().format(DATE_TIME_FORMATTER))
                                .build())
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("set clock service error: " + body))
                        )
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                bulkResponse.put(serial, resp);

            } catch (Exception e) {
                genericHandler.logAndSaveException(e, "set clock for serial " + serial);
                bulkResponse.put(serial, "Error: " + e.getMessage());
            }
        }

        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), bulkResponse);
    }

    @Override
    public Map<String, Object> setCtpt(DlmsBulkRequest.SetCtptRequest request) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : request.getSerials()) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setCtpt")
                                .queryParam("serial", serial)
                                .queryParam("ctNumerator", request.getCtNumerator())
                                .queryParam("ctDenominator", request.getCtDenominator())
                                .queryParam("ptNumerator", request.getPtNumerator())
                                .queryParam("ptDenominator", request.getPtDenominator())
                                .build())
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("set ctpt service error: " + body))
                        )
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                bulkResponse.put(serial, resp);

            } catch (Exception e) {
                genericHandler.logAndSaveException(e, "set ctpt for serial " + serial);
                bulkResponse.put(serial, "Error: " + e.getMessage());
            }
        }

        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), bulkResponse);
    }

    @Override
    public Map<String, Object> setApn(DlmsBulkRequest.SetApnRequest request) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : request.getSerials()) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setApn")
                                .queryParam("serial", serial)
                                .queryParam("apn", request.getApn())
                                .build())
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("set apn service error: " + body))
                        )
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                bulkResponse.put(serial, resp);

            } catch (Exception e) {
                genericHandler.logAndSaveException(e, "set apn for serial " + serial);
                bulkResponse.put(serial, "Error: " + e.getMessage());
            }
        }

        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), bulkResponse);
    }

    @Override
    public Map<String, Object> setIpPort(DlmsBulkRequest.SetIpPortRequest request) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : request.getSerials()) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setIpPort")
                                .queryParam("serial", serial)
                                .queryParam("ip", request.getIp())
                                .queryParam("port", request.getPort())
                                .build())
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .map(body -> new RuntimeException("set ip port service error: " + body))
                        )
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                bulkResponse.put(serial, resp);

            } catch (Exception e) {
                genericHandler.logAndSaveException(e, "set ip port for serial " + serial);
                bulkResponse.put(serial, "Error: " + e.getMessage());
            }
        }

        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), bulkResponse);
    }
}
