package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpStatus;


import java.util.Map;

@Service
public class HesClientServiceImpl {
    @Autowired
    private ResponseProperties status;

    @Autowired
    private HesAuthServiceImpl auth;

    @Autowired
    private WebClient webClient;

    public Map<String, Object> loadSomething() {
        System.out.println("loadSomething");
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
            // handles HTTP errors
            throw webClientResponseException;
        } catch (Exception exception) {
            throw exception;
        }
    }
}

//    public Map<String, Object> loadSomething() {
//        System.out.println("loadSomething");
//        String token = auth.getAccessToken();
//
//        Map<String, Object> resp = webClient.get()
//                .uri("/api/dashboard/summary")
//                .headers(h -> h.setBearerAuth(token))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                .block();
//        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
//    }