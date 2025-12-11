package com.thiefspin.bookingsystem.config;

import java.util.List;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConfigurationProperties(prefix = "cors")
@Setter
public class CorsConfig implements WebMvcConfigurer {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(allowedMethods.toArray(String[]::new))
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
