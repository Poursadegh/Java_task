package com.microservices.paymentservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.common.dto.UserInfo;
import com.microservices.common.enums.UserRole;
import com.microservices.paymentservice.config.SsoConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private final SsoConfig ssoConfig;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!ssoConfig.isValidateEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userInfoJson = request.getHeader("X-User-Info");
        String userRoleHeader = request.getHeader("X-User-Role");
        
        if (userInfoJson != null && !userInfoJson.isEmpty()) {
            try {
                UserInfo userInfo = objectMapper.readValue(userInfoJson, UserInfo.class);
                request.setAttribute("userInfo", userInfo);
            } catch (Exception e) {
                log.error("Error parsing user info", e);
            }
        } else if (userRoleHeader != null && !userRoleHeader.isEmpty()) {
            try {
                UserRole role = UserRole.valueOf(userRoleHeader);
                UserInfo userInfo = UserInfo.builder()
                    .role(role)
                    .authenticated(true)
                    .build();
                request.setAttribute("userInfo", userInfo);
            } catch (Exception e) {
                log.error("Error parsing user role", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}

