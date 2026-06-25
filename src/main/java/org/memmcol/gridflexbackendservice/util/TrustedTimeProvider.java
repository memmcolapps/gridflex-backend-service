package org.memmcol.gridflexbackendservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TrustedTimeProvider {

    private static final String TIME_API_URL = "https://timeapi.io/api/time/current/zone?timeZone=Africa/Lagos";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private TrustedTimeProvider() {
    }

    public static LocalDateTime getCurrentTime() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TIME_API_URL))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String dateTimeStr = json.get("dateTime").asText();
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch trusted time, falling back to system time: " + e.getMessage());
        }
        return LocalDateTime.now();
    }
}
