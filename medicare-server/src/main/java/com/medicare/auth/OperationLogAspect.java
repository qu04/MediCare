package com.medicare.auth;

import com.medicare.dto.Result;
import com.medicare.entity.OperationLog;
import com.medicare.entity.SysUser;
import com.medicare.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Around("execution(* com.medicare.controller..*(..))")
    public Object recordOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/system/")) {
            return joinPoint.proceed();
        }

        SysUser currentUser = AuthInterceptor.getCurrentUser(request);
        String module = resolveModule(uri);
        String action = resolveAction(request.getMethod(), joinPoint.getSignature().getName());

        try {
            Object result = joinPoint.proceed();
            String message = extractSuccessMessage(result, action);
            operationLogService.record(module, action, OperationLog.STATUS_SUCCESS, message, currentUser, request, null);
            return result;
        } catch (Throwable ex) {
            operationLogService.record(module, action, OperationLog.STATUS_FAILED, ex.getMessage(), currentUser, request, null);
            throw ex;
        }
    }

    private String resolveModule(String uri) {
        String[] parts = uri.split("/");
        return parts.length >= 3 ? parts[2].toUpperCase() : "UNKNOWN";
    }

    private String resolveAction(String method, String methodName) {
        return switch (method.toUpperCase()) {
            case "POST" -> "CREATE";
            case "PUT" -> methodName.toLowerCase().contains("password") ? "CHANGE_PASSWORD" : "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "QUERY";
        };
    }

    private String extractSuccessMessage(Object result, String action) {
        if (result instanceof Result<?> res && res.getMessage() != null) {
            return action + ":" + res.getMessage();
        }
        return action + ":success";
    }
}
