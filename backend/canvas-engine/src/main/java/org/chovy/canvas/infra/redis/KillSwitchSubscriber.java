package org.chovy.canvas.infra.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.trigger.InFlightExecutionRegistry;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
public class KillSwitchSubscriber {

    private final ReactiveRedisConnectionFactory factory;
    private final InFlightExecutionRegistry      registry;

    private static final String KILL_PATTERN = "canvas:kill:*";

    @PostConstruct
    void subscribe() {
        try {
            ReactiveRedisMessageListenerContainer container =
                    new ReactiveRedisMessageListenerContainer(factory);

            // 使用 pSubscribe（模式订阅）匹配所有 canvasId 的 kill 频道
            container.receive(new org.springframework.data.redis.listener.PatternTopic(KILL_PATTERN))
                    .doOnNext(msg -> {
                        String channel = msg.getChannel();   // "canvas:kill:{canvasId}"
                        String mode    = msg.getMessage();   // "GRACEFUL" or "FORCE"
                        try {
                            Long canvasId = Long.parseLong(
                                    channel.replace("canvas:kill:", ""));

                            if ("FORCE".equals(mode)) {
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
                    })
                    .doOnError(e -> log.error("[KILL] Kill Switch 订阅异常: {}", e.getMessage()))
                    .subscribe();

            log.info("[KILL] Kill Switch 订阅启动，监听模式: {}", KILL_PATTERN);
        } catch (Exception e) {
            log.warn("[KILL] Kill Switch 订阅失败（Redis 未连接时忽略）: {}", e.getMessage());
        }
    }
}
