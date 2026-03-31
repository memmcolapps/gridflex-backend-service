package org.memmcol.gridflexbackendservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("tokenGenWebClient")
    public WebClient tokenGenWebClient( @Value("${external.token-gen.base-url}") String baseUrl ) {

        return WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Bean
    @Qualifier("realtimeWebClient")
    public WebClient realtimeWebClient( @Value("${external.hes-realtime.base-url}") String baseUrl ) {

        return WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Bean
    @Qualifier("dlmsWriteOpsClient")
    public WebClient dlmsWriteOpsWebClient( @Value("${external.dlms-write-ops.base-url}") String baseUrl ) {

        return WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Bean
    @Qualifier("hesWebClient")
    public WebClient hesWebClient( @Value("${external.hes-endpoint.base-url}") String baseUrl ) {

        return WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

//    @Bean
//    public WebClient webClient() {
//        return WebClient.builder()
//                .baseUrl("http://localhost:9061/api/realtime")
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .build();
//    }
}
