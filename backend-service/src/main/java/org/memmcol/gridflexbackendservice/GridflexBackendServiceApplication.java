package org.memmcol.gridflexbackendservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class GridflexBackendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridflexBackendServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
