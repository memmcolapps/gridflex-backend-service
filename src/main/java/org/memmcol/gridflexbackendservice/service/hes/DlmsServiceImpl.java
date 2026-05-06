package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.HesMapper;
import org.memmcol.gridflexbackendservice.model.hes.ObisMapping;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @Autowired
    private HesMapper hesMapper;

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
        String ipPorts = ip+":"+port;
        try {
            Map<String, Object> resp = dlmsWriteOpsClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/setIpPort")
                            .queryParam("serial", serial)
                            .queryParam("ipPorts", ipPorts)
                            .build())
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    // ✅ Change .map() to .flatMap() here
                                    .flatMap(body -> Mono.error(new RuntimeException("set ip port service error: " + body)))
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

    @Override
    public Map<String, Object> readMeter(String serial, String type) {
        String token = auth.getAccessToken();

        try {
            List<ObisMapping> obisInfo = hesMapper.getObisCodeByMeterModel(serial, type);

            if (obisInfo == null || obisInfo.isEmpty()) {
                throw new RuntimeException("No OBIS mapping found for type: " + type);
            }

            if (obisInfo.size() == 1) {
                return fetchSingleObis(serial, obisInfo.get(0), token, type);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (ObisMapping obis : obisInfo) {
                String desc = obis.getDescription();
                try {
                    Map<String, Object> resp = fetchSingleObisRaw(serial, obis, token, type);

                    resp.put("description", desc);
                    results.add(resp);
                } catch (Exception ex) {
                    genericHandler.logAndSaveException(ex, "read " + type + " OBIS: " + obis.getObisCodeCombined());
                }
            }

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), results);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("read "+type+" service failed");
            genericHandler.logAndSaveException(e, "read "+type+" service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("read "+type+" service failed");
            genericHandler.logAndSaveException(e, "read  "+type);
            throw e;
        }
    }

    private Map<String, Object>
    fetchSingleObis(String serial, ObisMapping obisInfo, String token, String type) {
        Map<String, Object> resp = fetchSingleObisRaw(serial, obisInfo, token, type);
        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
    }

    private Map<String, Object> fetchSingleObisRaw(String serial, ObisMapping obisInfo, String token, String type) {
        String obis = obisInfo.getObisCodeCombined();
        System.out.println("obis: "+obis);

        return dlmsWriteOpsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/obis")
                        .queryParam("serial", serial)
                        .queryParam("obis", obis)
                        .build())
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("read " + type + " service error: " + body))
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
