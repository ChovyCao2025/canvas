package org.chovy.canvas.engine.wait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasWaitSubscription;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitResumeService {

    private final WaitSubscriptionService waitSubscriptionService;
    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;

    public int resumeEventWaits(String eventCode, String userId, Map<String, Object> eventAttributes, String eventId) {
        List<CanvasWaitSubscription> waits = waitSubscriptionService.findActiveEventWaits(eventCode, userId);
        int resumed = 0;
        for (CanvasWaitSubscription wait : waits) {
            if (completeAndTrigger(wait, eventAttributes, eventId)) {
                resumed++;
            }
        }
        return resumed;
    }

    public int resumeDueWaits(int limit) {
        List<CanvasWaitSubscription> waits = waitSubscriptionService.findExpiredActiveWaits(LocalDateTime.now(), limit);
        int resumed = 0;
        for (CanvasWaitSubscription wait : waits) {
            String status = timeoutDriven(wait)
                    ? WaitSubscriptionService.STATUS_EXPIRED
                    : WaitSubscriptionService.STATUS_COMPLETED;
            Map<String, Object> payload = resumePayload(wait, status, Map.of(), null);
            String payloadJson = toJson(payload);
            int updated = WaitSubscriptionService.STATUS_EXPIRED.equals(status)
                    ? waitSubscriptionService.expireWait(wait.getId(), payloadJson)
                    : waitSubscriptionService.completeWait(wait.getId(), payloadJson);
            if (updated > 0) {
                trigger(wait, status, payload);
                resumed++;
            }
        }
        return resumed;
    }

    private boolean completeAndTrigger(
            CanvasWaitSubscription wait,
            Map<String, Object> eventAttributes,
            String eventId
    ) {
        Map<String, Object> payload = resumePayload(
                wait,
                WaitSubscriptionService.STATUS_COMPLETED,
                eventAttributes == null ? Map.of() : eventAttributes,
                eventId
        );
        String payloadJson = toJson(payload);
        int updated = waitSubscriptionService.completeWait(wait.getId(), payloadJson);
        if (updated <= 0) {
            return false;
        }
        trigger(wait, WaitSubscriptionService.STATUS_COMPLETED, payload);
        return true;
    }

    private boolean timeoutDriven(CanvasWaitSubscription wait) {
        return WaitSubscriptionService.WAIT_TYPE_EVENT.equals(wait.getWaitType())
                || WaitSubscriptionService.WAIT_TYPE_GOAL.equals(wait.getWaitType());
    }

    private Map<String, Object> resumePayload(
            CanvasWaitSubscription wait,
            String status,
            Map<String, Object> eventAttributes,
            String eventId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>(storedPayload(wait));
        payload.put("sourceNodeId", wait.getNodeId());
        payload.put("waitSubscriptionId", wait.getId());
        payload.put("waitType", wait.getWaitType());
        payload.put("eventCode", wait.getEventCode());
        if (eventId != null) {
            payload.put("eventId", eventId);
        }
        if (!eventAttributes.isEmpty()) {
            payload.put("eventAttributes", eventAttributes);
        }
        if (WaitSubscriptionService.WAIT_TYPE_GOAL.equals(wait.getWaitType())) {
            payload.put("__goalResumeStatus", status);
        } else {
            payload.put("__waitResumeStatus", status);
        }
        return payload;
    }

    private Map<String, Object> storedPayload(CanvasWaitSubscription wait) {
        if (wait.getResumePayload() == null || wait.getResumePayload().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(wait.getResumePayload(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[WAIT] resume_payload 解析失败 waitId={} reason={}", wait.getId(), e.getMessage());
            return Map.of();
        }
    }

    private void trigger(CanvasWaitSubscription wait, String status, Map<String, Object> payload) {
        boolean goal = WaitSubscriptionService.WAIT_TYPE_GOAL.equals(wait.getWaitType());
        boolean expired = WaitSubscriptionService.STATUS_EXPIRED.equals(status);
        String nodeType = goal ? NodeType.GOAL_CHECK : NodeType.WAIT;
        String triggerType = goal
                ? (expired ? TriggerType.GOAL_CHECK_TIMEOUT : TriggerType.GOAL_CHECK_RESUME)
                : (expired ? TriggerType.WAIT_TIMEOUT : TriggerType.WAIT_RESUME);
        String msgId = wait.getExecutionId() + ":wait:" + wait.getId() + ":" + status;

        executionService.trigger(
                        wait.getCanvasId(),
                        wait.getUserId(),
                        triggerType,
                        nodeType,
                        wait.getNodeId(),
                        payload,
                        msgId,
                        false)
                .subscribe(
                        ignored -> log.info("[WAIT] 恢复执行 waitId={} status={}", wait.getId(), status),
                        e -> log.error("[WAIT] 恢复执行失败 waitId={}: {}", wait.getId(), e.getMessage())
                );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("等待恢复 payload 序列化失败", e);
        }
    }
}
