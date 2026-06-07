package org.chovy.canvas.infrastructure.redis;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.chovy.canvas.engine.trigger.InFlightExecutionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

/**
 * Kill Switch 订阅者（设计文档 13.9节）。
 *
 * 订阅 Redis Pub/Sub 频道 canvas:kill:{canvasId}，
 * 收到消息后取消该画布所有正在执行的 Reactor 订阅（FORCE 模式）。
 *
 * 设计文档时序图：
 *   CS -PUBLISH-> Redis -[kill signal]-> EE1/EE2
 *   EE: 拒绝新触发 + 标记当前执行为 KILLED
 */
@Slf4j
@Component
public class KillSwitchSubscriber {

    /** 响应式 Redis 连接工厂，用于创建 Pub/Sub 监听容器。 */
    private final ReactiveRedisConnectionFactory factory;
    /** 本机执行注册表，用于取消正在运行的画布执行。 */
    private final InFlightExecutionRegistry      registry;
    /** 后台订阅注册表，统一托管 Redis Pub/Sub 订阅生命周期。 */
    private final BackgroundSubscriptionRegistry backgroundSubscriptions;
    /** Redis Pub/Sub 监听容器，关闭时需要主动释放连接。 */
    private ReactiveRedisMessageListenerContainer listenerContainer;
    /** Kill Switch 订阅句柄，关闭时需要取消订阅。 */
    private Disposable subscription;

    /** Kill Switch 的 Redis 模式订阅频道。 */
    private static final String KILL_PATTERN = "canvas:kill:*";

    public KillSwitchSubscriber(ReactiveRedisConnectionFactory factory,
                                InFlightExecutionRegistry registry) {
        this(factory, registry, new BackgroundSubscriptionRegistry());
    }

    @Autowired
    public KillSwitchSubscriber(ReactiveRedisConnectionFactory factory,
                                InFlightExecutionRegistry registry,
                                BackgroundSubscriptionRegistry backgroundSubscriptions) {
        this.factory = factory;
        this.registry = registry;
        this.backgroundSubscriptions = backgroundSubscriptions == null
                ? new BackgroundSubscriptionRegistry()
                : backgroundSubscriptions;
    }

    /**
     * 执行 subscribe 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     */
    @PostConstruct
    void subscribe() {
        try {
            listenerContainer = new ReactiveRedisMessageListenerContainer(factory);

            // 使用 pSubscribe（模式订阅）匹配所有 canvasId 的 kill 频道
            subscription = backgroundSubscriptions.track(
                    "kill-switch-redis",
                    listenerContainer.receive(new org.springframework.data.redis.listener.PatternTopic(KILL_PATTERN))
                    .doOnNext(msg -> {
                        String channel = msg.getChannel();   // "canvas:kill:{canvasId}"
                        String mode    = msg.getMessage();   // "GRACEFUL" or "FORCE"
                        try {
                            // 频道名承载 canvasId，消息体只表达处理模式，便于按画布做模式订阅。
                            Long canvasId = Long.parseLong(
                                    channel.replace("canvas:kill:", ""));

                            if ("FORCE".equals(mode)) {
                                // FORCE 需要跨 JVM 取消本机正在执行的订阅，其他实例收到同一 Pub/Sub 消息各自处理。
                                int cancelled = registry.cancelAll(canvasId);
                                log.warn("[KILL] FORCE 模式，取消 {} 个执行 canvasId={}",
                                        cancelled, canvasId);
                            } else {
                                // GRACEFUL：拒绝新触发但不打断正在执行的
                                // 执行引擎在 TriggerPreCheckService 中检查 canvas.status
                                // 画布已被标记 status=4(KILLED)，新触发会被前置检查拦截
                                log.info("[KILL] GRACEFUL 模式，新触发将被拦截 canvasId={}", canvasId);
                            }
                        } catch (NumberFormatException e) {
                            log.error("[KILL] 解析 canvasId 失败 channel={}", channel);
                        }
                    }
            ),
                    e -> log.error("[KILL] Kill Switch 订阅异常: {}", e.getMessage()));

            log.info("[KILL] Kill Switch 订阅启动，监听模式: {}", KILL_PATTERN);
        } catch (Exception e) {
            log.warn("[KILL] Kill Switch 订阅失败（Redis 未连接时忽略）: {}", e.getMessage());
        }
    }

    /** 关闭服务时释放 Redis Pub/Sub 订阅和监听容器。 */
    @PreDestroy
    void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (listenerContainer != null) {
            try {
                listenerContainer.destroy();
            } catch (Exception e) {
                log.warn("[KILL] Kill Switch 订阅容器关闭失败: {}", e.getMessage());
            }
        }
    }
}
