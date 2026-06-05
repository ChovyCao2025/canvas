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

/** Polls Redis-backed special-node timeout timers and resumes paused executions. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecialNodeTimeoutPoller {

    private final RedisDelayQueue delayQueue;
    private final CanvasExecutionService executionService;

    @Scheduled(fixedDelayString = "${canvas.special-timeout.poll-delay-ms:1000}")
    public void pollDueTimeouts() {
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

    private void handleTriggerResult(RedisDelayQueue.SpecialNodeTimeout item, Map<String, Object> result) {
        if (shouldRequeue(result)) {
            delayQueue.requeueSpecialNodeTimeout(item);
            log.warn("[DELAY-Q] timeout trigger deferred id={} result={}", item.timeoutId(), result);
            return;
        }
        delayQueue.ackSpecialNodeTimeout(item);
        log.debug("[DELAY-Q] timeout fired id={}", item.timeoutId());
    }

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
