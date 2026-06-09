package org.chovy.canvas.domain.risk.runtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时风控决策输入；Map 会防御性复制，避免构造后变化影响回放哈希。
 *
 * @param tenantId 租户编号
 * @param requestId 请求幂等编号
 * @param sceneKey 场景业务键
 * @param eventTime 事件发生时间
 * @param event 事件事实
 * @param subject 主体标识和属性
 * @param context 决策上下文
 * @param suppliedFeatures 请求内显式特征快照
 * @param deadlineMs 决策超时时间
 */
public record RiskDecisionRequest(
        Long tenantId,
        String requestId,
        String sceneKey,
        Instant eventTime,
        Map<String, Object> event,
        Map<String, Object> subject,
        Map<String, Object> context,
        Map<String, Object> suppliedFeatures,
        int deadlineMs
) {

    public RiskDecisionRequest {
        event = event == null ? Map.of() : new LinkedHashMap<>(event);
        subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
        context = context == null ? Map.of() : new LinkedHashMap<>(context);
        suppliedFeatures = suppliedFeatures == null ? Map.of() : new LinkedHashMap<>(suppliedFeatures);
    }

    /**
     * 返回替换事件事实后的请求副本。
     */
    public RiskDecisionRequest withEvent(Map<String, Object> newEvent) {
        return new RiskDecisionRequest(tenantId, requestId, sceneKey, eventTime,
                newEvent, subject, context, suppliedFeatures, deadlineMs);
    }

    /**
     * 返回替换超时时间后的请求副本。
     */
    public RiskDecisionRequest withDeadlineMs(int newDeadlineMs) {
        return new RiskDecisionRequest(tenantId, requestId, sceneKey, eventTime,
                event, subject, context, suppliedFeatures, newDeadlineMs);
    }
}
