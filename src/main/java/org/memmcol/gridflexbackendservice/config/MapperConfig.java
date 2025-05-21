package org.memmcol.gridflexbackendservice.config;

import org.memmcol.gridflexbackendservice.util.UUIDTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@MapperScan("org.memmcol.gridflexbackendservice.mapper")
public class MapperConfig {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> configuration.getTypeHandlerRegistry()
                .register(UUID.class, new UUIDTypeHandler());
    }
}
