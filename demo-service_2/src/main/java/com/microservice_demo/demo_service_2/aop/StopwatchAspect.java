package com.microservice_demo.demo_service_2.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class StopwatchAspect {

    @Around("@annotation(com.microservice_demo.demo_service_2.aop.Stopwatch)")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig    = (MethodSignature) pjp.getSignature();
        String          klass  = pjp.getTarget().getClass().getSimpleName();
        String          method = sig.getMethod().getName();

        long start = System.currentTimeMillis();
        try {
            Object result  = pjp.proceed();
            long   elapsed = System.currentTimeMillis() - start;
            log.info("[STOPWATCH] {}.{} completed in {} ms", klass, method, elapsed);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[STOPWATCH] {}.{} failed after {} ms — {}",
                    klass, method, elapsed, ex.getMessage());
            throw ex;
        }
    }
}
