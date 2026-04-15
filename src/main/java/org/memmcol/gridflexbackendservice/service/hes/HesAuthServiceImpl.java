package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.model.hes.AuthResponse;
import org.memmcol.gridflexbackendservice.model.hes.RefreshData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
public class HesAuthServiceImpl {

    private final WebClient authWebClient;    // for auth endpoints
    private final WebClient realtimeWebClient; // for protected endpoints

    private String accessToken;
    private String refreshToken;  // original refresh token from login
    private Instant expiryTime;

    private final String clientId = "123e4567-e89b-12d3-a456-426614174000";
    private final String clientSecret = "5D8F2A3B4C5D6E7F8A9B0C1D2E3F4A5B6C7D8E9F0A1B2C3D4E5F6A7B8C9D0E1";

    private final IMap<String, Object> debtCache;

//    private final IMap<String, Object> auditCache;


//    public HesAuthServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
//        this.debtCache = hazelcastInstance.getMap("hesTokenCache");
//    }

    public HesAuthServiceImpl(
            WebClient.Builder builder,
            @Qualifier("realtimeWebClient") WebClient realtimeWebClient,
            @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance,
            @Value("${external.hes-endpoint.base-url}") String baseUrl) {

//        this.authWebClient = builder.baseUrl("http://172.16.2.46:9061").build();
        this.authWebClient = builder.baseUrl("http://127.0.0.1:9061").build();
        this.realtimeWebClient = realtimeWebClient;
        this.debtCache = hazelcastInstance.getMap("hesTokenCache");
    }

    /**
     * Get a valid access token. Refreshes if expired.
     */
    public synchronized String getAccessToken() {
        System.out.println("getAccessToken");
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            if (refreshToken != null) {
                System.out.println("refreshToken");
                refreshAccessToken();
            } else {
                System.out.println("accessToken");
                authenticate();  // initial login
            }
        }
        return accessToken;
    }

    private void authenticate() {
        AuthResponse response = authWebClient.post()
                .uri("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("clientId", clientId, "clientSecret", clientSecret))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    throw new RuntimeException("Auth failed ["
                                            + clientResponse.statusCode() + "]: " + errorBody);
                                })
                )
                .bodyToMono(AuthResponse.class)
                .block();

        if (response == null) throw new IllegalStateException("Null auth response");
        this.accessToken = response.getAccessToken();
        this.refreshToken = response.getRefreshToken();
        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
    }

    private void refreshAccessToken() {
        RefreshData response = authWebClient.post()  // ← use authWebClient here too
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/refresh")
                        .queryParam("refreshToken", refreshToken)
                        .build())
                .retrieve()
                .bodyToMono(RefreshData.class)
                .block();

        if (response == null) throw new IllegalStateException("Null refresh response");
        this.accessToken = response.getAccessToken();
        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
    }

//    public synchronized String getAccessToken1() {
//        System.out.println("getAccessToken");
//        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
//            System.out.println("accessToken");
//            authenticate();  // initial login
//        } else {
//            System.out.println("refreshToken");
//            refreshAccessToken(); //refresh expired token
//        }
//        System.out.println("return statement");
//        return accessToken;
//    }

    /**
     * Initial login using clientId and clientSecret
     */
//    private void authenticate() {
//        AuthResponse response = webClient.post()
//                .uri("/api/auth/token")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(Map.of("clientId", clientId, "clientSecret", clientSecret))
//                .retrieve()
//                .onStatus(
//                        status -> status.is4xxClientError() || status.is5xxServerError(),
//                        clientResponse -> clientResponse.bodyToMono(String.class)
//                                .map(errorBody -> {
//                                    // This logs the EXACT error message from API A
//                                    throw new RuntimeException("Auth failed ["
//                                            + clientResponse.statusCode() + "]: " + errorBody);
//                                })
//                )
//                .bodyToMono(AuthResponse.class)
//                .block();
//
////        assert response != null;
//        // ✅ Use this instead
//        if (response == null) {
//            throw new IllegalStateException("Auth response from API A was null");
//        }
//        this.accessToken = response.getAccessToken();
//        this.refreshToken = response.getRefreshToken();
//        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
//    }

    /**
     * Refresh access token using refresh token
     */
//    private void refreshAccessToken() {
//
//        RefreshData response = webClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/auth/refresh")
//                        .queryParam("refreshToken", refreshToken)
//                        .build())
//                .retrieve()
//                .bodyToMono(RefreshData.class)
//                .block();
//
//        assert response != null;
//        this.accessToken = response.getAccessToken();
//        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
//    }

}