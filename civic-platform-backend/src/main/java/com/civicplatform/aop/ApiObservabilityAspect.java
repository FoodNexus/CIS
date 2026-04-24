package com.civicplatform.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting observability for controller/service execution.
 */
@Aspect
@Component
@Slf4j
public class ApiObservabilityAspect {

    @Value("${app.observability.slow-threshold-ms:600}")
    private long slowThresholdMs;

    /**
     * Logs duration and errors for API and service methods.
     */
    @Around("execution(public * com.civicplatform.controller..*(..)) || execution(public * com.civicplatform.service..*(..))")
    public Object aroundTrackedMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String signature = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= slowThresholdMs) {
                log.warn("Slow call {} took {} ms", signature, elapsed);
            } else {
                log.debug("Call {} took {} ms", signature, elapsed);
            }
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Call {} failed after {} ms: {}", signature, elapsed, t.getMessage());
            throw t;
        }
    }
}
