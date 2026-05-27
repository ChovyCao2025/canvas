package org.chovy.canvas.engine.wait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 等待节点恢复服务。
 *
 * <p>负责两类恢复场景：
 * <ol>
 *   <li>事件驱动恢复（{@link #resumeEventWaits}）：
 *       事件上报时，查找与该 eventCode + userId 匹配的 ACTIVE 订阅，
 *       CAS 更新为 COMPLETED 后 fire-and-forget 触发引擎继续执行；</li>
 *   <li>超时驱动恢复（{@link #resumeDueWaits}）：
 *       定时任务扫描 expiresAt ≤ now 的 ACTIVE 记录，更新为 EXPIRED 或 COMPLETED
 *       后触发引擎走超时分支。</li>
 * </ol>
 *
 * <p>分布式安全（多机部署）：
 * <ul>
 *   <li>CAS 更新（{@code WHERE status='ACTIVE'}）保证同一订阅只有一台机器恢复成功；</li>
 *   <li>恢复触发调用 {@link CanvasExecutionService#trigger}，内部通过 Redis resumeLock
 *       （SETNX）防止多台机器并发恢复同一份上下文；</li>
 *   <li>超时扫描需确保多实例不重复处理：当前依靠 DB CAS（WHERE status='ACTIVE'）保证幂等，
 *       但可能存在多台机器同时扫出同一批记录的短窗口——CAS 失败的机器安全跳过。</li>
 * </ul>
 *
 * <p>⚠️ 已知问题（WAIT 恢复 + 配额限制的交互）：
 * {@link #trigger} 调用 {@code executionService.trigger(dryRun=false)}，
 * 该路径会经过 {@link TriggerPreCheckService#checkWithoutQuotaAccounting}（含冷却期 DB 软检查）
 * 和 {@link TriggerPreCheckService#consumeQuotaAndRecord}（扣减配额）。
 * 若画布配置了 cooldownSeconds，且原始触发与恢复时间间隔在冷却期内，恢复会被误拒；
 * 若画布有 perUserTotalLimit，每次 WAIT 恢复都会额外消耗用户配额。
 * 根治方案：在 isResume=true 时跳过 checkWithoutQuotaAccounting 和 consumeQuotaAndRecord。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitResumeService {

    /** 等待订阅服务，用于查询和 CAS 完成等待记录。 */
    private final WaitSubscriptionService waitSubscriptionService;
    /** 画布执行服务。 */
    private final CanvasExecutionService executionService;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;

    /**
     * 事件驱动恢复：查找该用户在该 eventCode 下所有 ACTIVE 等待，逐一恢复。
     *
     * <p>查询时过滤 expiresAt > now（或为 null），避免恢复已超时的订阅。
     * 遍历时若 CAS 更新失败（updated=0），视为已被其他机器/线程恢复，安全跳过。
     *
     * @return 实际恢复成功的订阅数量
     */
    public int resumeEventWaits(String eventCode, String userId, Map<String, Object> eventAttributes, String eventId) {
        List<CanvasWaitSubscriptionDO> waits = waitSubscriptionService.findActiveEventWaits(eventCode, userId);
        int resumed = 0;
        for (CanvasWaitSubscriptionDO wait : waits) {
            if (!matchesEventFilters(wait, eventAttributes == null ? Map.of() : eventAttributes)) {
                continue;
            }
            if (completeAndTrigger(wait, eventAttributes, eventId)) {
                resumed++;
            }
        }
        return resumed;
    }

    /** 恢复或过期到期的时间等待订阅。 */
    public int resumeDueWaits(int limit) {
        List<CanvasWaitSubscriptionDO> waits = waitSubscriptionService.findExpiredActiveWaits(LocalDateTime.now(), limit);
        int resumed = 0;
        for (CanvasWaitSubscriptionDO wait : waits) {
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

    /**
     * CAS 完成订阅 + fire-and-forget 触发引擎恢复执行。
     *
     * <p>步骤：
     * <ol>
     *   <li>组装 resumePayload（包含原快照 + 事件属性 + 恢复状态标记）；</li>
     *   <li>{@code completeWait}：{@code UPDATE ... WHERE status='ACTIVE'} CAS，失败则跳过；</li>
     *   <li>{@code trigger}：异步触发引擎，.subscribe() 是 fire-and-forget，不等待执行完成。</li>
     * </ol>
     *
     * @return 是否成功触发恢复（CAS 成功）
     */
    private boolean completeAndTrigger(
            CanvasWaitSubscriptionDO wait,
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
        // CAS：WHERE id=? AND status='ACTIVE'，多机并发时只有一台成功
        int updated = waitSubscriptionService.completeWait(wait.getId(), payloadJson);
        if (updated <= 0) {
            return false;
        }
        trigger(wait, WaitSubscriptionService.STATUS_COMPLETED, payload);
        return true;
    }

    /** 判断等待订阅到期后是否应走超时分支而非普通完成分支。 */
    private boolean timeoutDriven(CanvasWaitSubscriptionDO wait) {
        return WaitSubscriptionService.WAIT_TYPE_EVENT.equals(wait.getWaitType())
                || WaitSubscriptionService.WAIT_TYPE_GOAL.equals(wait.getWaitType());
    }

    /** 按订阅中保存的过滤条件判断事件属性是否满足恢复条件。 */
    @SuppressWarnings("unchecked")
    private boolean matchesEventFilters(CanvasWaitSubscriptionDO wait, Map<String, Object> eventAttributes) {
        String filtersJson = wait.getEventFilters();
        if (filtersJson == null || filtersJson.isBlank()) {
            return true;
        }
        try {
            Map<String, Object> filters = objectMapper.readValue(filtersJson, new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                Object actual = eventAttributes.get(entry.getKey());
                Object expected = entry.getValue();
                if (expected instanceof Map<?, ?> operators) {
                    if (!matchesOperators(actual, (Map<String, Object>) operators)) {
                        return false;
                    }
                } else if (!Objects.equals(String.valueOf(actual), String.valueOf(expected))) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("[WAIT] eventFilters 解析失败 waitId={} reason={}", wait.getId(), e.getMessage());
            return false;
        }
    }

    /** 逐个计算过滤条件操作符，支持等值、比较和集合包含。 */
    private boolean matchesOperators(Object actual, Map<String, Object> operators) {
        for (Map.Entry<String, Object> operator : operators.entrySet()) {
            String op = operator.getKey();
            Object expected = operator.getValue();
            boolean matched = switch (op) {
                case "eq" -> Objects.equals(String.valueOf(actual), String.valueOf(expected));
                case "ne" -> !Objects.equals(String.valueOf(actual), String.valueOf(expected));
                case "gt" -> compareNumbers(actual, expected) > 0;
                case "gte" -> compareNumbers(actual, expected) >= 0;
                case "lt" -> compareNumbers(actual, expected) < 0;
                case "lte" -> compareNumbers(actual, expected) <= 0;
                case "in" -> expected instanceof List<?> values
                        && values.stream().anyMatch(value -> Objects.equals(String.valueOf(actual), String.valueOf(value)));
                default -> false;
            };
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    /** 将两个值转为数字后比较，无法转换时返回不匹配。 */
    private int compareNumbers(Object actual, Object expected) {
        Number actualNumber = toNumber(actual);
        Number expectedNumber = toNumber(expected);
        if (actualNumber == null || expectedNumber == null) {
            return -1;
        }
        return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue());
    }

    /** 将 Number 或数字字符串转换为数值对象。 */
    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return null;
        }
        return Double.parseDouble(value.toString());
    }

    /** 合并原始恢复载荷、等待订阅信息和事件属性，生成恢复触发 payload。 */
    private Map<String, Object> resumePayload(
            CanvasWaitSubscriptionDO wait,
            String status,
            Map<String, Object> eventAttributes,
            String eventId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>(storedPayload(wait));
        payload.put(MapFieldKeys.SOURCE_NODE_ID, wait.getNodeId());
        payload.put(MapFieldKeys.WAIT_SUBSCRIPTION_ID, wait.getId());
        payload.put(MapFieldKeys.WAIT_TYPE, wait.getWaitType());
        payload.put(MapFieldKeys.EVENT_CODE, wait.getEventCode());
        if (eventId != null) {
            payload.put(MapFieldKeys.EVENT_ID, eventId);
        }
        if (!eventAttributes.isEmpty()) {
            payload.put(MapFieldKeys.EVENT_ATTRIBUTES, eventAttributes);
        }
        if (WaitSubscriptionService.WAIT_TYPE_GOAL.equals(wait.getWaitType())) {
            payload.put(MapFieldKeys.GOAL_RESUME_STATUS, status);
        } else {
            payload.put(MapFieldKeys.WAIT_RESUME_STATUS, status);
        }
        return payload;
    }

    /** 读取等待订阅中保存的恢复载荷，解析失败时返回空载荷。 */
    private Map<String, Object> storedPayload(CanvasWaitSubscriptionDO wait) {
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

    /**
     * 触发引擎恢复执行（fire-and-forget）。
     *
     * <p>triggerType / nodeType 根据 waitType 和 status 决定：
     * <pre>
     *   GOAL_CHECK + EXPIRED  → GOAL_CHECK_TIMEOUT / GOAL_CHECK
     *   GOAL_CHECK + COMPLETED → GOAL_CHECK_RESUME / GOAL_CHECK
     *   UNTIL_EVENT + EXPIRED → WAIT_TIMEOUT / WAIT
     *   UNTIL_EVENT + COMPLETED → WAIT_RESUME / WAIT
     * </pre>
     *
     * <p>msgId 格式：{executionId}:wait:{waitId}:{status}，保证同一订阅恢复的幂等性。
     * 注意：msgId 非 null 时引擎会做 dedup 检查，可防止 resumeEventWaits 和超时扫描同时触发
     * 的重复恢复（两者 msgId 中 status 不同，可同时通过 dedup；但 CAS 已在上游保证只有一条
     * 能更新为 COMPLETED/EXPIRED，因此幂等保护此处退化为防守性措施）。
     */
    private void trigger(CanvasWaitSubscriptionDO wait, String status, Map<String, Object> payload) {
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
                        false)  // dryRun=false，走完整 triggerInternal 路径（含冷却期检查，见类注释 ⚠️）
                .subscribe(
                        ignored -> log.info("[WAIT] 恢复执行 waitId={} status={}", wait.getId(), status),
                        e -> log.error("[WAIT] 恢复执行失败 waitId={}: {}", wait.getId(), e.getMessage())
                );
    }

    /** 将恢复载荷序列化为等待订阅表中的 JSON 文本。 */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("等待恢复 payload 序列化失败", e);
        }
    }
}
