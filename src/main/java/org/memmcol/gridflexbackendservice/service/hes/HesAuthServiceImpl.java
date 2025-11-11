package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.model.hes.AuthResponse;
import org.memmcol.gridflexbackendservice.model.hes.RefreshData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
public class HesAuthServiceImpl {

//    private final WebClient client = WebClient.create("https://thirdparty.com");


    private String accessToken;
    private String refreshToken;  // original refresh token from login
    private Instant expiryTime;

    @Autowired
    private WebClient webClient;

    private final String clientId = "123e4567-e89b-12d3-a456-426614174000";
    private final String clientSecret = "5D8F2A3B4C5D6E7F8A9B0C1D2E3F4A5B6C7D8E9F0A1B2C3D4E5F6A7B8C9D0E1";

    /**
     * Get a valid access token. Refreshes if expired.
     */
    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            if (refreshToken != null) {
                refreshAccessToken();
            } else {
                authenticate();  // initial login
            }
        }
        return accessToken;
    }

    /**
     * Initial login using clientId and clientSecret
     */
    private void authenticate() {
        AuthResponse response = webClient.post()
                .uri("/api/auth/token")
                .bodyValue(Map.of("clientId", clientId, "clientSecret", clientSecret))
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .block();

        assert response != null;
        this.accessToken = response.getAccessToken();
        this.refreshToken = response.getRefreshToken();
        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
    }

    /**
     * Refresh access token using refresh token
     */
    private void refreshAccessToken() {
        RefreshData response = webClient.post()
                .uri("/api/auth/refresh")
                .bodyValue(Map.of("refreshToken", refreshToken))
                .retrieve()
                .bodyToMono(RefreshData.class)
                .block();

        assert response != null;
        this.accessToken = response.getAccessToken();
        this.expiryTime = Instant.now().plusSeconds(response.getExpiresIn() - 30);
    }

}


//    private volatile String accessToken;
//    private volatile String refreshToken;
//    private volatile Instant expiry;
//
//    public synchronized String getValidAccessToken() {
//
//        if (accessToken != null && Instant.now().isBefore(expiry)) {
//            return accessToken;
//        }
//
//        if (refreshToken != null) {
//            try {
//                return refreshAccessToken();
//            } catch (Exception ex) {
//                // Refresh failed; proceed to full login
//            }
//        }
//
//        return fullAuthenticate();
//    }
//
//    private String fullAuthenticate() {
//        AuthResponse res = client.post()
//                .uri("/auth/login")
//                .bodyValue(Map.of(
//                        "client_id", "xxx",
//                        "client_secret", "yyy"
//                ))
//                .retrieve()
//                .bodyToMono(AuthResponse.class)
//                .block();
//
//        accessToken = res.getAccessToken();
//        refreshToken = res.getRefreshToken();
//        expiry = Instant.now().plusSeconds(res.getExpiresIn());
//
//        return accessToken;
//    }
//
//    private String refreshAccessToken() {
//
//        RefreshResponse res = client.post()
//                .uri("/auth/refresh")
//                .bodyValue(Map.of(
//                        "refresh_token", refreshToken
//                ))
//                .retrieve()
//                .bodyToMono(RefreshResponse.class)
//                .block();
//
//        accessToken = res.getAccessToken();
//        expiry = Instant.now().plusSeconds(res.getExpiresIn());
//
//        return accessToken;
//    }