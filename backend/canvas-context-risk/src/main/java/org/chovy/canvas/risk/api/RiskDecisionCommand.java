package org.chovy.canvas.risk.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 定义 RiskDecisionCommand 的风控模块职责和数据契约。
 */
public final class RiskDecisionCommand {

    /**
     * RiskDecisionCommand 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskDecisionCommand 的 requestId 字段。
     */
    private final String requestId;


    /**
     * RiskDecisionCommand 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskDecisionCommand 的 eventTime 字段。
     */
    private final Instant eventTime;


    /**
     * RiskDecisionCommand 的 subject 字段。
     */
    private final Map<String, Object> subject;


    /**
     * RiskDecisionCommand 的 event 字段。
     */
    private final Map<String, Object> event;


    /**
     * RiskDecisionCommand 的 context 字段。
     */
    private final Map<String, Object> context;


    /**
     * RiskDecisionCommand 的 features 字段。
     */
    private final Map<String, Object> features;


    /**
     * RiskDecisionCommand 的 deadlineMs 字段。
     */
    private final int deadlineMs;


    /**
     * 创建 RiskDecisionCommand。
     *
     * @param tenantId RiskDecisionCommand 的 tenantId 字段
     * @param requestId RiskDecisionCommand 的 requestId 字段
     * @param sceneKey RiskDecisionCommand 的 sceneKey 字段
     * @param eventTime RiskDecisionCommand 的 eventTime 字段
     * @param subject RiskDecisionCommand 的 subject 字段
     * @param event RiskDecisionCommand 的 event 字段
     * @param context RiskDecisionCommand 的 context 字段
     * @param features RiskDecisionCommand 的 features 字段
     * @param deadlineMs RiskDecisionCommand 的 deadlineMs 字段
     */
    public RiskDecisionCommand(Long tenantId, String requestId, String sceneKey, Instant eventTime, Map<String, Object> subject, Map<String, Object> event, Map<String, Object> context, Map<String, Object> features, int deadlineMs) {
        subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
                event = event == null ? Map.of() : new LinkedHashMap<>(event);
                context = context == null ? Map.of() : new LinkedHashMap<>(context);
                features = features == null ? Map.of() : new LinkedHashMap<>(features);
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.sceneKey = sceneKey;
        this.eventTime = eventTime;
        this.subject = subject;
        this.event = event;
        this.context = context;
        this.features = features;
        this.deadlineMs = deadlineMs;
    }

    /**
     * 返回 RiskDecisionCommand 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskDecisionCommand 的 requestId 字段。
     *
     * @return requestId 字段值
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回 RiskDecisionCommand 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskDecisionCommand 的 eventTime 字段。
     *
     * @return eventTime 字段值
     */
    public Instant eventTime() {
        return eventTime;
    }

    /**
     * 返回 RiskDecisionCommand 的 subject 字段。
     *
     * @return subject 字段值
     */
    public Map<String, Object> subject() {
        return subject;
    }

    /**
     * 返回 RiskDecisionCommand 的 event 字段。
     *
     * @return event 字段值
     */
    public Map<String, Object> event() {
        return event;
    }

    /**
     * 返回 RiskDecisionCommand 的 context 字段。
     *
     * @return context 字段值
     */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * 返回 RiskDecisionCommand 的 features 字段。
     *
     * @return features 字段值
     */
    public Map<String, Object> features() {
        return features;
    }

    /**
     * 返回 RiskDecisionCommand 的 deadlineMs 字段。
     *
     * @return deadlineMs 字段值
     */
    public int deadlineMs() {
        return deadlineMs;
    }

    /**
     * 比较当前 RiskDecisionCommand 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionCommand other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(requestId, other.requestId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(eventTime, other.eventTime)
                && Objects.equals(subject, other.subject)
                && Objects.equals(event, other.event)
                && Objects.equals(context, other.context)
                && Objects.equals(features, other.features)
                && deadlineMs == other.deadlineMs;
    }

    /**
     * 计算 RiskDecisionCommand 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, requestId, sceneKey, eventTime, subject, event, context, features, deadlineMs);
    }

    /**
     * 返回 RiskDecisionCommand 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionCommand[tenantId=" + tenantId + ", requestId=" + requestId + ", sceneKey=" + sceneKey + ", eventTime=" + eventTime + ", subject=" + subject + ", event=" + event + ", context=" + context + ", features=" + features + ", deadlineMs=" + deadlineMs + "]";
    }
}
