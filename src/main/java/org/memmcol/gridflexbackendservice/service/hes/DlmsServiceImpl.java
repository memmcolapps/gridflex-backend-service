package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public Map<String, Object> setClock(List<String> serials, LocalDateTime dateTime) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : serials) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setClock")
                                .queryParam("serial", serial)
                                .queryParam("dateTime", dateTime.format(formatter))
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
    public Map<String, Object> setCtpt(List<String> serials,
                                       long ctNumerator,
                                       long ctDenominator,
                                       long ptNumerator,
                                       long ptDenominator) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : serials) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setCtpt")
                                .queryParam("serial", serial)
                                .queryParam("ctNumerator", ctNumerator)
                                .queryParam("ctDenominator", ctDenominator)
                                .queryParam("ptNumerator", ptNumerator)
                                .queryParam("ptDenominator", ptDenominator)
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
    public Map<String, Object> setApn(List<String> serials, String apn) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : serials) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setApn")
                                .queryParam("serial", serial)
                                .queryParam("apn", apn)
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
    public Map<String, Object> setIpPort(List<String> serials, String ip, int port) {
        String token = auth.getAccessToken();
        Map<String, Object> bulkResponse = new HashMap<>();

        for (String serial : serials) {
            try {
                Map<String, Object> resp = dlmsWriteOpsClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/setIpPort")
                                .queryParam("serial", serial)
                                .queryParam("ip", ip)
                                .queryParam("port", port)
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
