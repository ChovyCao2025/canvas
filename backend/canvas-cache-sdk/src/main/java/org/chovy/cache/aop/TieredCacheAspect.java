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
     * 拦截 {@link TieredCached} 并执行声明式缓存读取。
     *
     * <p>切面先计算 SpEL key 和 condition，再按返回类型处理同步值、Optional 和 Mono，未命中时执行原方法并按 unless/cacheNull 写回缓存。
     *
     * @param pjp 被拦截的业务方法调用
     * @param annotation 缓存读取注解
     * @return 缓存命中值或原方法执行结果
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
     * 拦截 {@link TieredCacheEvict} 并执行声明式缓存失效。
     *
     * <p>支持前置失效和后置失效；后置失效默认挂到事务提交后，避免事务回滚后缓存被错误清理。
     *
     * @param pjp 被拦截的业务方法调用
     * @param annotation 缓存失效注解
     * @return 原方法执行结果
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
     * 拦截 {@link TieredCachePut} 并把方法返回值写入缓存。
     *
     * <p>Optional 会写入其内部值或 null；Mono 会在元素产生时写入；普通返回值按 afterCommit 设置决定立即或提交后写入。
     *
     * @param pjp 被拦截的业务方法调用
     * @param annotation 缓存写入注解
     * @return 原方法执行结果
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
     * 判断目标方法返回值是否应写入缓存。
     *
     * <p>null 结果先受 cacheNull 控制，再执行 unless 表达式；unless 为 true 表示跳过写回。
     *
     * @param annotation 缓存读取注解
     * @param method 被拦截的方法
     * @param args 方法实参数组
     * @param result 目标方法返回值或 Mono 元素
     * @return true 表示允许写入缓存
     */
    private boolean shouldStore(TieredCached annotation, Method method, Object[] args, Object result) {
        if (result == null && !annotation.cacheNull()) {
            return false;
        }
        return !keyEvaluator.evaluateBoolean(annotation.unless(), method, args, result, false);
    }

    /**
     * 失效 SpEL 表达式解析出的一个或多个 key。
     *
     * <p>表达式结果可以是单值、集合或数组；每个 key 都会进入缓存实现的完整失效链路。
     *
     * @param cacheName 缓存实例名称
     * @param keyExpressionValue SpEL 表达式解析结果
     */
    private void evictKeys(String cacheName, Object keyExpressionValue) {
        // key 表达式可返回单个 key、集合或数组，批量失效时逐项触发缓存实现的失效链路。
        keys(keyExpressionValue).forEach(key -> resolver.evictIfPresent(cacheName, key));
    }

    /**
     * 将 key 表达式结果展开为 Stream。
     *
     * <p>支持集合、对象数组和基本类型数组，方便一个注解同时失效多个业务 key。
     *
     * @param value key 表达式解析结果
     * @return 待处理 key 的流
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
     * 执行原始业务方法并统一异常边界。
     *
     * <p>运行时异常原样抛出，受检异常包装为 RuntimeException，便于缓存切面保持统一调用签名。
     *
     * @param pjp 被拦截的业务方法调用
     * @return 原方法返回值
     */
    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据注解配置决定立即执行缓存动作或延后到事务提交后。
     *
     * <p>存在 Spring 事务同步且 afterCommit 为 true 时注册 afterCommit 回调，否则直接执行。
     *
     * @param afterCommit 是否等待事务提交
     * @param action 缓存写入或失效动作
     */
    private void runAfterCommit(boolean afterCommit, Runnable action) {
        if (afterCommit && TransactionSynchronizationManager.isSynchronizationActive()) {
            // 有事务同步时把缓存写入/失效挂到 afterCommit，避免事务回滚后缓存状态领先数据库。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 在当前事务成功提交后执行缓存动作。
                 *
                 * <p>缓存写入或失效只在提交后发生，避免目标方法返回但事务最终回滚时污染 L1/L2。
                 */
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
