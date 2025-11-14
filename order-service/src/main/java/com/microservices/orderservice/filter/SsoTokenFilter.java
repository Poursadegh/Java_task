package com.microservices.orderservice.filter;

import com.microservices.orderservice.config.SsoConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class SsoTokenFilter extends OncePerRequestFilter {

    private final SsoConfig ssoConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!ssoConfig.isValidateEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(ssoConfig.getTokenHeader());
        
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing authentication token");
            return;
        }

        request.setAttribute("authToken", token);
        filterChain.doFilter(request, response);
    }
}

