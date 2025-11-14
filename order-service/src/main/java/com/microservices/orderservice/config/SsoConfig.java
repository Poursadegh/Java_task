package com.microservices.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SsoConfig {

    @Value("${sso.token.header:X-Auth-Token}")
    private String tokenHeader;

    @Value("${sso.validate.enabled:true}")
    private boolean validateEnabled;

    public String getTokenHeader() {
        return tokenHeader;
    }

    public boolean isValidateEnabled() {
        return validateEnabled;
    }
}

