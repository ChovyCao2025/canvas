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

/**
 * 分层缓存注解切面。
 *
 * <p>拦截 TieredCached、TieredCachePut 和 TieredCacheEvict 注解方法，串联 key 计算、缓存读取、方法调用和缓存写入/失效。
 * <p>该切面是声明式缓存能力进入业务代码的入口，必须保持幂等和异常边界清晰。
 */
@Aspect
@RequiredArgsConstructor
public class TieredCacheAspect {
    /** 注解缓存名称到缓存实例的解析器。 */
    private final AnnotationCacheResolver resolver;
    /** 注解 SpEL key、condition 和 unless 表达式计算器。 */
    private final SpelKeyEvaluator keyEvaluator;

    /**
     * 执行 around Cached 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param pjp pjp 方法执行所需的业务参数
     * @param annotation annotation 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    @Around("@annotation(annotation)")
    public Object aroundCached(ProceedingJoinPoint pjp, TieredCached annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        TieredCache<Object, Object> cache = resolver.resolve(annotation);
        if (!keyEvaluator.evaluateBoolean(annotation.condition(), method, pjp.getArgs(), null, true)) {
            // condition 不满足时完全绕过缓存，避免把不应缓存的调用写入后续层级。
            return proceed(pjp);
        }
        Class<?> returnType = method.getReturnType();
        if (Mono.class.isAssignableFrom(returnType)) {
            // 响应式返回值不能阻塞等待原方法结果，先查现有缓存，未命中后在订阅链路内回填。
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
            // Optional 方法保留原始返回类型，缓存层只存其中的业务值或空值占位。
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
            // 无 unless 且允许空值缓存时交给缓存实现的 loaderOverride，复用其穿透保护和回填流程。
            return cache.get(key, () -> proceed(pjp)).orElse(null);
        }
        // 需要根据 unless 判断结果时必须先执行原方法，因此只能手动 getIfPresent/proceed/put。
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

    /**
     * 执行 around Evict 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param pjp pjp 方法执行所需的业务参数
     * @param annotation annotation 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    @Around("@annotation(annotation)")
    public Object aroundEvict(ProceedingJoinPoint pjp, TieredCacheEvict annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        if (annotation.beforeInvocation()) {
            // 前置失效适用于原方法可能失败但仍需清理缓存的场景，不等待事务提交。
            evictKeys(annotation.name(), key);
            return proceed(pjp);
        }
        Object result = proceed(pjp);
        // 默认在方法成功后失效，若存在事务则延后到提交点，降低脏数据回填窗口。
        runAfterCommit(annotation.afterCommit(), () -> evictKeys(annotation.name(), key));
        return result;
    }

    /**
     * 执行 around Put 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param pjp pjp 方法执行所需的业务参数
     * @param annotation annotation 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    @Around("@annotation(annotation)")
    public Object aroundPut(ProceedingJoinPoint pjp, TieredCachePut annotation) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object key = keyEvaluator.evaluate(annotation.key(), method, pjp.getArgs());
        Object result = proceed(pjp);
        if (result instanceof Optional<?> optional) {
            Object value = optional.orElse(null);
            // Optional.empty 也写入缓存，让 cacheNull 策略决定是否形成短 TTL 空值保护。
            runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, value));
            return result;
        }
        if (result instanceof Mono<?> mono) {
            // Mono 只有真正产生元素后才写缓存，保持响应式链路的延迟执行语义。
            return mono.doOnNext(value ->
                    runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, value)));
        }
        runAfterCommit(annotation.afterCommit(), () -> resolver.getExisting(annotation.name()).put(key, result));
        return result;
    }

    /**
     * 判断 should Store 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param annotation annotation 方法执行所需的业务参数
     * @param method method 方法执行所需的业务参数
     * @param args args 方法执行所需的业务参数
     * @param result result 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean shouldStore(TieredCached annotation, Method method, Object[] args, Object result) {
        if (result == null && !annotation.cacheNull()) {
            return false;
        }
        return !keyEvaluator.evaluateBoolean(annotation.unless(), method, args, result, false);
    }

    /**
     * 删除、清理或失效 evict Keys 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param cacheName cacheName 方法执行所需的业务参数
     * @param keyExpressionValue keyExpressionValue 对应的缓存键、配置键或业务键
     */
    private void evictKeys(String cacheName, Object keyExpressionValue) {
        // key 表达式可返回单个 key、集合或数组，批量失效时逐项触发缓存实现的失效链路。
        keys(keyExpressionValue).forEach(key -> resolver.evictIfPresent(cacheName, key));
    }

    /**
     * 执行 keys 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 方法执行后的业务结果
     */
    private Stream<?> keys(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream();
        }
        if (value != null && value.getClass().isArray()) {
            if (value instanceof Object[] array) {
                return Stream.of(array);
            }
            // 基本类型数组不能直接转 Object[]，通过反射逐个取值后统一成 Stream。
            int length = java.lang.reflect.Array.getLength(value);
            return java.util.stream.IntStream.range(0, length)
                    .mapToObj(index -> java.lang.reflect.Array.get(value, index));
        }
        return Stream.of(value);
    }

    /**
     * 执行 proceed 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param pjp pjp 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
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

    /**
     * 执行 run After Commit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param afterCommit afterCommit 方法执行所需的业务参数
     * @param action action 方法执行所需的业务参数
     */
    private void runAfterCommit(boolean afterCommit, Runnable action) {
        if (afterCommit && TransactionSynchronizationManager.isSynchronizationActive()) {
            // 有事务同步时把缓存写入/失效挂到 afterCommit，避免事务回滚后缓存状态领先数据库。
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
