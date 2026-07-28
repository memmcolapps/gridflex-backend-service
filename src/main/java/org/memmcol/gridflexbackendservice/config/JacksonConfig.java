package org.memmcol.gridflexbackendservice.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.memmcol.gridflexbackendservice.util.StringTrimmerDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule stringTrimModule() {

        SimpleModule module = new SimpleModule();

        module.addDeserializer(
                String.class,
                new StringTrimmerDeserializer()
        );

        return module;
    }
}
