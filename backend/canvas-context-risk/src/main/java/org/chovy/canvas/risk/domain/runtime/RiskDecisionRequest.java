package org.chovy.canvas.risk.domain.runtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
public final class RiskDecisionRequest {

    /**
     * RiskDecisionRequest 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskDecisionRequest 的 requestId 字段。
     */
    private final String requestId;


    /**
     * RiskDecisionRequest 的 sceneKey 字段。
     */
    private final String sceneKey;


    /**
     * RiskDecisionRequest 的 eventTime 字段。
     */
    private final Instant eventTime;


    /**
     * RiskDecisionRequest 的 event 字段。
     */
    private final Map<String, Object> event;


    /**
     * RiskDecisionRequest 的 subject 字段。
     */
    private final Map<String, Object> subject;


    /**
     * RiskDecisionRequest 的 context 字段。
     */
    private final Map<String, Object> context;


    /**
     * RiskDecisionRequest 的 suppliedFeatures 字段。
     */
    private final Map<String, Object> suppliedFeatures;


    /**
     * RiskDecisionRequest 的 deadlineMs 字段。
     */
    private final int deadlineMs;


    /**
     * 创建 RiskDecisionRequest。
     *
     * @param tenantId RiskDecisionRequest 的 tenantId 字段
     * @param requestId RiskDecisionRequest 的 requestId 字段
     * @param sceneKey RiskDecisionRequest 的 sceneKey 字段
     * @param eventTime RiskDecisionRequest 的 eventTime 字段
     * @param event RiskDecisionRequest 的 event 字段
     * @param subject RiskDecisionRequest 的 subject 字段
     * @param context RiskDecisionRequest 的 context 字段
     * @param suppliedFeatures RiskDecisionRequest 的 suppliedFeatures 字段
     * @param deadlineMs RiskDecisionRequest 的 deadlineMs 字段
     */
    public RiskDecisionRequest(Long tenantId, String requestId, String sceneKey, Instant eventTime, Map<String, Object> event, Map<String, Object> subject, Map<String, Object> context, Map<String, Object> suppliedFeatures, int deadlineMs) {
        event = event == null ? Map.of() : new LinkedHashMap<>(event);
                subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
                context = context == null ? Map.of() : new LinkedHashMap<>(context);
                suppliedFeatures = suppliedFeatures == null ? Map.of() : new LinkedHashMap<>(suppliedFeatures);
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.sceneKey = sceneKey;
        this.eventTime = eventTime;
        this.event = event;
        this.subject = subject;
        this.context = context;
        this.suppliedFeatures = suppliedFeatures;
        this.deadlineMs = deadlineMs;
    }

    /**
     * 返回 RiskDecisionRequest 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskDecisionRequest 的 requestId 字段。
     *
     * @return requestId 字段值
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回 RiskDecisionRequest 的 sceneKey 字段。
     *
     * @return sceneKey 字段值
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回 RiskDecisionRequest 的 eventTime 字段。
     *
     * @return eventTime 字段值
     */
    public Instant eventTime() {
        return eventTime;
    }

    /**
     * 返回 RiskDecisionRequest 的 event 字段。
     *
     * @return event 字段值
     */
    public Map<String, Object> event() {
        return event;
    }

    /**
     * 返回 RiskDecisionRequest 的 subject 字段。
     *
     * @return subject 字段值
     */
    public Map<String, Object> subject() {
        return subject;
    }

    /**
     * 返回 RiskDecisionRequest 的 context 字段。
     *
     * @return context 字段值
     */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * 返回 RiskDecisionRequest 的 suppliedFeatures 字段。
     *
     * @return suppliedFeatures 字段值
     */
    public Map<String, Object> suppliedFeatures() {
        return suppliedFeatures;
    }

    /**
     * 返回 RiskDecisionRequest 的 deadlineMs 字段。
     *
     * @return deadlineMs 字段值
     */
    public int deadlineMs() {
        return deadlineMs;
    }

    /**
     * 比较当前 RiskDecisionRequest 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionRequest other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(requestId, other.requestId)
                && Objects.equals(sceneKey, other.sceneKey)
                && Objects.equals(eventTime, other.eventTime)
                && Objects.equals(event, other.event)
                && Objects.equals(subject, other.subject)
                && Objects.equals(context, other.context)
                && Objects.equals(suppliedFeatures, other.suppliedFeatures)
                && deadlineMs == other.deadlineMs;
    }

    /**
     * 计算 RiskDecisionRequest 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, requestId, sceneKey, eventTime, event, subject, context, suppliedFeatures, deadlineMs);
    }

    /**
     * 返回 RiskDecisionRequest 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionRequest[tenantId=" + tenantId + ", requestId=" + requestId + ", sceneKey=" + sceneKey + ", eventTime=" + eventTime + ", event=" + event + ", subject=" + subject + ", context=" + context + ", suppliedFeatures=" + suppliedFeatures + ", deadlineMs=" + deadlineMs + "]";
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
