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
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

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
        if (!keyEvaluator.evaluateBoolean(annotation.condition(), method, pjp.getArgs(), null, true)) {
            return proceed(pjp);
        }
        Class<?> returnType = method.getReturnType();
        if (Mono.class.isAssignableFrom(returnType)) {
            Optional<Object> cached = cache.getIfPresent(key);
            if (cached.isPresent()) {
                return Mono.just(cached.get());
            }
            return ((Mono<?>) proceed(pjp)).flatMap(result -> {
                if (shouldStore(annotation, method, pjp.getArgs(), result)) {
                    cache.put(key, result);
                }
                return Mono.justOrEmpty(result);
            });
        }
        if (Optional.class.isAssignableFrom(returnType)) {
            Optional<Object> cached = cache.getIfPresent(key);
            if (cached.isPresent()) {
                return cached;
            }
            Optional<?> result = (Optional<?>) proceed(pjp);
            Object value = result.orElse(null);
            if (shouldStore(annotation, method, pjp.getArgs(), value)) {
                cache.put(key, value);
            }
            return result;
        }
        if (annotation.unless().isBlank() && annotation.cacheNull()) {
            return cache.get(key, () -> proceed(pjp)).orElse(null);
        }
        Optional<Object> cached = cache.getIfPresent(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        Object result = proceed(pjp);
        if (shouldStore(annotation, method, pjp.getArgs(), result)) {
            cache.put(key, result);
        }
        return result;
    }

    @Around("@annotation(annotation)")
    public Object aroundEvict(ProceedingJoinPoint pjp, TieredCacheEvict annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        if (annotation.beforeInvocation()) {
            evictKeys(annotation.name(), key);
            return proceed(pjp);
        }
        Object result = proceed(pjp);
        runAfterCommit(annotation.afterCommit(), () -> evictKeys(annotation.name(), key));
        return result;
    }

    @Around("@annotation(annotation)")
    public Object aroundPut(ProceedingJoinPoint pjp, TieredCachePut annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        Object result = proceed(pjp);
        if (result instanceof Optional<?> optional) {
            Object value = optional.orElse(null);
            runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, value));
            return result;
        }
        if (result instanceof Mono<?> mono) {
            return mono.doOnNext(value ->
                    runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, value)));
        }
        runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, result));
        return result;
    }

    private boolean shouldStore(TieredCached annotation, Method method, Object[] args, Object result) {
        if (result == null && !annotation.cacheNull()) {
            return false;
        }
        return !keyEvaluator.evaluateBoolean(annotation.unless(), method, args, result, false);
    }

    private void evictKeys(String cacheName, Object keyExpressionValue) {
        keys(keyExpressionValue).forEach(key -> resolver.evictIfPresent(cacheName, key));
    }

    private Stream<?> keys(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream();
        }
        if (value != null && value.getClass().isArray()) {
            if (value instanceof Object[] array) {
                return Stream.of(array);
            }
            int length = java.lang.reflect.Array.getLength(value);
            return java.util.stream.IntStream.range(0, length)
                    .mapToObj(index -> java.lang.reflect.Array.get(value, index));
        }
        return Stream.of(value);
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
