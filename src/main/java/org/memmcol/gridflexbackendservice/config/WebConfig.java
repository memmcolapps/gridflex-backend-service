package org.memmcol.gridflexbackendservice.config;

import org.memmcol.gridflexbackendservice.components.LicenceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired(required = false)
    private LicenceInterceptor licenceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (licenceInterceptor != null) {
            registry.addInterceptor(licenceInterceptor)
                    .addPathPatterns("/**");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
