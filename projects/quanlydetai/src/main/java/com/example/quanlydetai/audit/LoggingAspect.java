package com.example.quanlydetai.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Logs operation metadata only. Arguments, result bodies and exception messages may contain
 * secrets.
 */
@Aspect
@Component
@Order(0)
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around(
            "execution(public * com.example.quanlydetai.service..*(..)) &&"
                    + " @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object record(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        String outcome = "SUCCESS";
        try {
            return call.proceed();
        } catch (Throwable ex) {
            outcome = "FAILED:" + ex.getClass().getSimpleName();
            throw ex;
        } finally {
            log.info(
                    "operation={} outcome={} durationMs={}",
                    call.getSignature().toShortString(),
                    outcome,
                    (System.nanoTime() - start) / 1_000_000);
        }
    }
}
