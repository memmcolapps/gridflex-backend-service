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
    public Map<String, Object> setClock(String serial, LocalDateTime dateTime) {
        String token = auth.getAccessToken();

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

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set clock service failed");
            genericHandler.logAndSaveException(e, "set clock service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set clock service failed");
            genericHandler.logAndSaveException(e, "set clock");
            throw e;
        }
    }

    @Override
    public Map<String, Object> setCtpt(String serial,
                                       long ctNumerator,
                                       long ctDenominator,
                                       long ptNumerator,
                                       long ptDenominator) {
        String token = auth.getAccessToken();

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

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set ctpt service failed");
            genericHandler.logAndSaveException(e, "set ctpt service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set ctpt service failed");
            genericHandler.logAndSaveException(e, "set ctpt");
            throw e;
        }
    }

    @Override
    public Map<String, Object> setApn(String serial, String apn) {
        String token = auth.getAccessToken();

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

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set apn service failed");
            genericHandler.logAndSaveException(e, "set apn service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set apn service failed");
            genericHandler.logAndSaveException(e, "set apn");
            throw e;
        }
    }

    @Override
    public Map<String, Object> setIpPort(String serial, String ip, int port) {
        String token = auth.getAccessToken();

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

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set ip port service failed");
            genericHandler.logAndSaveException(e, "set ip port service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set ip port service failed");
            genericHandler.logAndSaveException(e, "set ip port");
            throw e;
        }
    }
}
