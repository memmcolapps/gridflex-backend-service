package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
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


import java.util.Map;

@Service
public class HesClientServiceImpl implements HesService {
    @Autowired
    private ResponseProperties status;

    @Autowired
    private HesAuthServiceImpl auth;

    @Autowired
    private WebClient webClient;

    @Autowired
    private GenericHandler genericHandler;

    private final IMap<String, Object> hesTokenCache;

    public HesClientServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hesTokenCache = hazelcastInstance.getMap("hesTokenCache");
    }

    public Map<String, Object> dashboard() {
        String token = auth.getAccessToken();

        try {
            Map<String, Object> resp = webClient.get()
                    .uri("/api/dashboard/summary")
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("HES service error: " + body))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException webClientResponseException) {
            genericHandler.logIncidentReport("fetching hes dashboard service failed");
            genericHandler.logAndSaveException(webClientResponseException, "fetching hes dashboard");
            // handles HTTP errors
            throw webClientResponseException;
        } catch (Exception exception) {
            genericHandler.logIncidentReport("fetching hes dashboard service failed");
            genericHandler.logAndSaveException(exception, "fetching hes dashboard");
            throw exception;
        }
    }
}
