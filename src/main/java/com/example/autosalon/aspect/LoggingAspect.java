package com.example.autosalon.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Логирует время выполнения всех публичных методов в пакете service и его подпакетах.
     */
    @Around("execution(* com.example.autosalon.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("▶ ВЫЗОВ: {}.{}", className, methodName);

        Object result;
        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("◀ ВЫПОЛНЕН: {}.{} за {} мс", className, methodName, elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("✗ ОШИБКА: {}.{} после {} мс - {}", className, methodName, elapsed, ex.getMessage());
            throw ex;
        }
        return result;
    }
}
