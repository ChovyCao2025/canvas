package org.chovy.canvas.engine.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.redis.RedisDelayQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** 轮询 Redis 中的特殊节点超时计时器，并恢复暂停中的执行。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecialNodeTimeoutPoller {

    private final RedisDelayQueue delayQueue;
    private final CanvasExecutionService executionService;

    /**
     * pollDueTimeouts 处理 engine.scheduler 场景的业务逻辑。
     */
    @Scheduled(fixedDelayString = "${canvas.special-timeout.poll-delay-ms:1000}")
    public void pollDueTimeouts() {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (RedisDelayQueue.SpecialNodeTimeout item : delayQueue.pollDueSpecialNodeTimeouts()) {
            Map<String, Object> payload = timeoutPayload(item);
            executionService.trigger(
                            item.canvasId(),
                            item.userId(),
                            item.triggerType(),
                            item.nodeType(),
                            item.nodeId(),
                            payload,
                            item.timeoutId(),
                            false)
                    .subscribe(
                            result -> handleTriggerResult(item, result),
                            e -> {
                                delayQueue.requeueSpecialNodeTimeout(item);
                                log.error("[DELAY-Q] timeout trigger failed id={}: {}",
                                        item.timeoutId(), e.getMessage());
                            });
        }
    }

    /**
     * 根据恢复触发结果确认或重新入队超时任务。
     *
     * @param item 超时任务
     * @param result trigger 返回结果
     */
    private void handleTriggerResult(RedisDelayQueue.SpecialNodeTimeout item, Map<String, Object> result) {
        if (shouldRequeue(result)) {
            delayQueue.requeueSpecialNodeTimeout(item);
            log.warn("[DELAY-Q] timeout trigger deferred id={} result={}", item.timeoutId(), result);
            return;
        }
        delayQueue.ackSpecialNodeTimeout(item);
        log.debug("[DELAY-Q] timeout fired id={}", item.timeoutId());
    }

    /**
     * 判断触发结果是否需要重新入队等待下次恢复。
     *
     * @param result trigger 返回结果
     * @return true 表示执行被限流或锁占用，需要重新入队
     */
    private boolean shouldRequeue(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        if (result.containsKey(MapFieldKeys.OVERFLOW)) {
            return true;
        }
        Object skipped = result.get(MapFieldKeys.SKIPPED);
        return "resume-lock".equals(skipped);
    }

    /**
     * 构造特殊节点超时恢复的触发载荷。
     *
     * @param item 超时任务
     * @return 传给 trigger 主链路的超时上下文
     */
    static Map<String, Object> timeoutPayload(RedisDelayQueue.SpecialNodeTimeout item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.EXECUTION_ID, item.executionId());
        payload.put(MapFieldKeys.VERSION_ID, item.versionId());
        payload.put(MapFieldKeys.TIMEOUT_TIMER_KEY, item.timerKey());
        payload.put(MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS, item.scheduledAtEpochMs());
        payload.put(MapFieldKeys.TIMEOUT_FIRE_AT_EPOCH_MS, item.fireAtEpochMs());
        payload.put(MapFieldKeys.TIMEOUT_SECONDS, item.timeoutSec());
        return payload;
    }
}
