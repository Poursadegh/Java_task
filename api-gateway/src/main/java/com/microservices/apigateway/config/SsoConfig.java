package com.microservices.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SsoConfig {

    @Value("${sso.service.url:http://sso-service:8083}")
    private String ssoServiceUrl;

    @Value("${sso.token.header:X-Auth-Token}")
    private String tokenHeader;

    @Value("${sso.validate.enabled:true}")
    private boolean validateEnabled;

    public String getSsoServiceUrl() {
        return ssoServiceUrl;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public boolean isValidateEnabled() {
        return validateEnabled;
    }
}

