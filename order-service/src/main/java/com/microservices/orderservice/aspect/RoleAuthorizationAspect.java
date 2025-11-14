package com.microservices.orderservice.aspect;

import com.microservices.common.annotation.RequireRole;
import com.microservices.common.dto.UserInfo;
import com.microservices.common.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RoleAuthorizationAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Request context not available");
        }

        HttpServletRequest request = attributes.getRequest();
        UserInfo userInfo = (UserInfo) request.getAttribute("userInfo");

        if (userInfo == null || !userInfo.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to {}", joinPoint.getSignature().toShortString());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        UserRole[] requiredRoles = requireRole.value();
        UserRole userRole = userInfo.getRole();

        boolean hasRequiredRole = Arrays.stream(requiredRoles)
            .anyMatch(role -> role == userRole);

        if (!hasRequiredRole) {
            log.warn("Access denied for user {} with role {} to {}", 
                userInfo.getUserId(), userRole, joinPoint.getSignature().toShortString());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Insufficient permissions. Required role: " + Arrays.toString(requiredRoles));
        }
    }
}

