package com.microservices.apigateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("dev")
public class DevConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DevConfiguration.class);

    @PostConstruct
    public void init() {
        logger.info("========================================");
        logger.info("DEV PROFILE ACTIVATED");
        logger.info("API Gateway is running in DEVELOPMENT mode");
        logger.info("Debug logging is enabled");
        logger.info("CORS is configured to allow all origins");
        logger.info("========================================");
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        logger.debug("CORS WebFilter configured for DEV environment");
        return new CorsWebFilter(source);
    }

    @Bean
    public DevEnvironmentInfo devEnvironmentInfo() {
        return new DevEnvironmentInfo();
    }

    public static class DevEnvironmentInfo {
        private final String environment = "DEVELOPMENT";
        private final boolean debugMode = true;
        private final boolean corsEnabled = true;

        public String getEnvironment() {
            return environment;
        }

        public boolean isDebugMode() {
            return debugMode;
        }

        public boolean isCorsEnabled() {
            return corsEnabled;
        }
    }
}

