//package org.memmcol.gridflexbackendservice.service.hes;
//
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.map.IMap;
//import org.memmcol.gridflexbackendservice.model.hes.AuthResponse;
//import org.memmcol.gridflexbackendservice.model.hes.RefreshData;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.time.Instant;
//import java.util.Map;
//
//@Service
//public class HesAuthServiceImpl {
//
////    private final WebClient client = WebClient.create("https://thirdparty.com");
//
//
//    private String accessToken;
//    private String refreshToken;  // original refresh token from login
//    private Instant expiryTime;
//
//    @Autowired
//    private WebClient webClient;
//
//    private final String clientId = "123e4567-e89b-12d3-a456-426614174000";
//    private final String clientSecret = "5D8F2A3B4C5D6E7F8A9B0C1D2E3F4A5B6C7D8E9F0A1B2C3D4E5F6A7B8C9D0E1";
//
//    private final IMap<String, Object> debtCache;
//
////    private final IMap<String, Object> auditCache;
//
//
//    public HesAuthServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
//        this.debtCache = hazelcastInstance.getMap("hesTokenCache");
//    }
//    /**
//     * Get a valid access token. Refreshes if expired.
//     */
//    public synchronized String getAccessToken() {
//        System.out.println("getAccessToken");
//        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
//            if (refreshToken != null) {
//                System.out.println("refreshToken");
//                refreshAccessToken();
//            } else {
//                System.out.println("accessToken");
//                authenticate();  // initial login
//            }
//        }
//        System.out.println("return statement");
//        return accessToken;
//    }
//
////    public synchronized String getAccessToken1() {
////        System.out.println("getAccessToken");
////        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
////            System.out.println("accessToken");
////            authenticate();  // initial login
////        } else {
////            System.out.println("refreshToken");
////            refreshAccessToken(); //refresh expired token
////        }
////        System.out.println("return statement");
////        return accessToken;
////    }
//
//    /**
//     * Initial login using clientId and clientSecret
//     */
//    private void authenticate() {
//        AuthResponse response = webClient.post()
//                .uri("/api/auth/token")
//                .bodyValue(Map.of("clientId", clientId, "clientSecret", clientSecret))
//                .retrieve()
//                .bodyToMono(AuthResponse.class)
//                .block();
//
//        assert response != null;
//        this.accessToken = response.getAccessToken();
//        this.refreshToken = response.getRefreshToken();
//        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
//    }
//
//    /**
//     * Refresh access token using refresh token
//     */
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
//
//}