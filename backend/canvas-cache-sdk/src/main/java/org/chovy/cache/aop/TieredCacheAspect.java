package org.chovy.cache.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.chovy.cache.TieredCache;
import org.chovy.cache.annotation.TieredCacheEvict;
import org.chovy.cache.annotation.TieredCachePut;
import org.chovy.cache.annotation.TieredCached;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Aspect
@RequiredArgsConstructor
public class TieredCacheAspect {
    private final AnnotationCacheResolver resolver;
    private final SpelKeyEvaluator keyEvaluator;

    @Around("@annotation(annotation)")
    public Object aroundCached(ProceedingJoinPoint pjp, TieredCached annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        TieredCache<Object, Object> cache = resolver.resolve(annotation);
        return cache.get(key, () -> proceed(pjp)).orElse(null);
    }

    @Around("@annotation(annotation)")
    public Object aroundEvict(ProceedingJoinPoint pjp, TieredCacheEvict annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        if (annotation.beforeInvocation()) {
            resolver.getExisting(annotation.name()).invalidate(key);
            return proceed(pjp);
        }
        Object result = proceed(pjp);
        runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).invalidate(key));
        return result;
    }

    @Around("@annotation(annotation)")
    public Object aroundPut(ProceedingJoinPoint pjp, TieredCachePut annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        Object result = proceed(pjp);
        runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, result));
        return result;
    }

    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(e);
        }
    }

    private void runAfterCommit(boolean afterCommit, Runnable action) {
        if (afterCommit && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
