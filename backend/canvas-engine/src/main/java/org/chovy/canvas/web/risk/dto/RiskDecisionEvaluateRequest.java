package org.chovy.canvas.web.risk.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 风控在线评估请求 DTO。
 *
 * @param tenantId 请求体租户提示，实际租户以认证上下文为准
 * @param requestId 请求幂等编号
 * @param sceneKey 场景业务键
 * @param subject 主体属性
 * @param eventTime 事件时间
 * @param event 事件事实
 * @param context 决策上下文
 * @param features 请求内特征快照
 * @param options 评估选项
 */
public record RiskDecisionEvaluateRequest(
        Long tenantId,
        String requestId,
        String sceneKey,
        Map<String, Object> subject,
        String eventTime,
        Map<String, Object> event,
        Map<String, Object> context,
        Map<String, Object> features,
        Options options
) {

    public RiskDecisionEvaluateRequest {
        subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
        event = event == null ? Map.of() : new LinkedHashMap<>(event);
        context = context == null ? Map.of() : new LinkedHashMap<>(context);
        features = features == null ? Map.of() : new LinkedHashMap<>(features);
    }

    /**
     * 返回替换租户提示后的请求副本。
     */
    public RiskDecisionEvaluateRequest withTenantId(Long newTenantId) {
        return new RiskDecisionEvaluateRequest(newTenantId, requestId, sceneKey, subject, eventTime,
                event, context, features, options);
    }

    /**
     * 返回替换场景键后的请求副本。
     */
    public RiskDecisionEvaluateRequest withSceneKey(String newSceneKey) {
        return new RiskDecisionEvaluateRequest(tenantId, requestId, newSceneKey, subject, eventTime,
                event, context, features, options);
    }

    /**
     * 返回替换主体属性后的请求副本。
     */
    public RiskDecisionEvaluateRequest withSubject(Map<String, Object> newSubject) {
        return new RiskDecisionEvaluateRequest(tenantId, requestId, sceneKey, newSubject, eventTime,
                event, context, features, options);
    }

    /**
     * 返回替换事件时间后的请求副本。
     */
    public RiskDecisionEvaluateRequest withEventTime(String newEventTime) {
        return new RiskDecisionEvaluateRequest(tenantId, requestId, sceneKey, subject, newEventTime,
                event, context, features, options);
    }

    /**
     * 返回替换请求超时时间后的请求副本。
     */
    public RiskDecisionEvaluateRequest withDeadlineMs(int deadlineMs) {
        Options newOptions = new Options(
                options == null ? null : options.modeOverride(),
                options != null && options.includeTrace(),
                deadlineMs);
        return new RiskDecisionEvaluateRequest(tenantId, requestId, sceneKey, subject, eventTime,
                event, context, features, newOptions);
    }

    /**
     * 风控在线评估选项。
     *
     * @param modeOverride 运行模式覆盖值
     * @param includeTrace 是否包含追踪信息
     * @param deadlineMs 请求超时时间
     */
    public record Options(
            String modeOverride,
            boolean includeTrace,
            Integer deadlineMs
    ) {
    }
}
