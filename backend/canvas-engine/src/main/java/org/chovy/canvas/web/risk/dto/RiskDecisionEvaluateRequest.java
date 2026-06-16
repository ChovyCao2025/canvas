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
public final class RiskDecisionEvaluateRequest {

    /**
     * 租户标识。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("tenantId")
    private final Long tenantId;

    /**
     * 请求幂等标识。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("requestId")
    private final String requestId;

    /**
     * 场景业务键。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("sceneKey")
    private final String sceneKey;

    /**
     * subject 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("subject")
    private final Map<String, Object> subject;

    /**
     * eventTime 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("eventTime")
    private final String eventTime;

    /**
     * event 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("event")
    private final Map<String, Object> event;

    /**
     * context 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("context")
    private final Map<String, Object> context;

    /**
     * features 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("features")
    private final Map<String, Object> features;

    /**
     * options 字段值。
     */
    @com.fasterxml.jackson.annotation.JsonProperty("options")
    private final Options options;

    /**
     * 创建 RiskDecisionEvaluateRequest 实例。
     *
     * @param tenantId 租户标识
     * @param requestId 请求幂等标识
     * @param sceneKey 场景业务键
     * @param subject subject 字段值
     * @param eventTime eventTime 字段值
     * @param event event 字段值
     * @param context context 字段值
     * @param features features 字段值
     * @param options options 字段值
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public RiskDecisionEvaluateRequest(@com.fasterxml.jackson.annotation.JsonProperty("tenantId") Long tenantId, @com.fasterxml.jackson.annotation.JsonProperty("requestId") String requestId, @com.fasterxml.jackson.annotation.JsonProperty("sceneKey") String sceneKey, @com.fasterxml.jackson.annotation.JsonProperty("subject") Map<String, Object> subject, @com.fasterxml.jackson.annotation.JsonProperty("eventTime") String eventTime, @com.fasterxml.jackson.annotation.JsonProperty("event") Map<String, Object> event, @com.fasterxml.jackson.annotation.JsonProperty("context") Map<String, Object> context, @com.fasterxml.jackson.annotation.JsonProperty("features") Map<String, Object> features, @com.fasterxml.jackson.annotation.JsonProperty("options") Options options) {
        subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
        event = event == null ? Map.of() : new LinkedHashMap<>(event);
        context = context == null ? Map.of() : new LinkedHashMap<>(context);
        features = features == null ? Map.of() : new LinkedHashMap<>(features);

        this.tenantId = tenantId;
        this.requestId = requestId;
        this.sceneKey = sceneKey;
        this.subject = subject;
        this.eventTime = eventTime;
        this.event = event;
        this.context = context;
        this.features = features;
        this.options = options;
    }

    /**
     * 返回租户标识。
     *
     * @return 租户标识
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回请求幂等标识。
     *
     * @return 请求幂等标识
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回场景业务键。
     *
     * @return 场景业务键
     */
    public String sceneKey() {
        return sceneKey;
    }

    /**
     * 返回subject 字段值。
     *
     * @return subject 字段值
     */
    public Map<String, Object> subject() {
        return subject;
    }

    /**
     * 返回eventTime 字段值。
     *
     * @return eventTime 字段值
     */
    public String eventTime() {
        return eventTime;
    }

    /**
     * 返回event 字段值。
     *
     * @return event 字段值
     */
    public Map<String, Object> event() {
        return event;
    }

    /**
     * 返回context 字段值。
     *
     * @return context 字段值
     */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * 返回features 字段值。
     *
     * @return features 字段值
     */
    public Map<String, Object> features() {
        return features;
    }

    /**
     * 返回options 字段值。
     *
     * @return options 字段值
     */
    public Options options() {
        return options;
    }

    /**
     * 判断两个 RiskDecisionEvaluateRequest 实例是否包含相同字段值。
     *
     * @param o 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionEvaluateRequest that)) {
            return false;
        }
        return java.util.Objects.equals(tenantId, that.tenantId) && java.util.Objects.equals(requestId, that.requestId) && java.util.Objects.equals(sceneKey, that.sceneKey) && java.util.Objects.equals(subject, that.subject) && java.util.Objects.equals(eventTime, that.eventTime) && java.util.Objects.equals(event, that.event) && java.util.Objects.equals(context, that.context) && java.util.Objects.equals(features, that.features) && java.util.Objects.equals(options, that.options);
    }

    /**
     * 根据全部字段生成哈希值。
     *
     * @return 字段哈希值
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tenantId, requestId, sceneKey, subject, eventTime, event, context, features, options);
    }

    /**
     * 返回与原记录形态一致的调试字符串。
     *
     * @return 字段调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionEvaluateRequest[" + "tenantId=" + tenantId + ", " + "requestId=" + requestId + ", " + "sceneKey=" + sceneKey + ", " + "subject=" + subject + ", " + "eventTime=" + eventTime + ", " + "event=" + event + ", " + "context=" + context + ", " + "features=" + features + ", " + "options=" + options + "]";
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
    public static final class Options {

        /**
         * 运行模式覆盖值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("modeOverride")
        private final String modeOverride;

        /**
         * 是否包含追踪信息。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("includeTrace")
        private final boolean includeTrace;

        /**
         * 请求超时时间。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("deadlineMs")
        private final Integer deadlineMs;

        /**
         * 创建 Options 实例。
         *
         * @param modeOverride 运行模式覆盖值
         * @param includeTrace 是否包含追踪信息
         * @param deadlineMs 请求超时时间
         */
        public Options(String modeOverride, boolean includeTrace, Integer deadlineMs) {
            this.modeOverride = modeOverride;
            this.includeTrace = includeTrace;
            this.deadlineMs = deadlineMs;
        }

        /**
         * 返回运行模式覆盖值。
         *
         * @return 运行模式覆盖值
         */
        public String modeOverride() {
            return modeOverride;
        }

        /**
         * 返回是否包含追踪信息。
         *
         * @return 是否包含追踪信息
         */
        public boolean includeTrace() {
            return includeTrace;
        }

        /**
         * 返回请求超时时间。
         *
         * @return 请求超时时间
         */
        public Integer deadlineMs() {
            return deadlineMs;
        }

        /**
         * 判断两个 Options 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Options that)) {
                return false;
            }
            return java.util.Objects.equals(modeOverride, that.modeOverride) && java.util.Objects.equals(includeTrace, that.includeTrace) && java.util.Objects.equals(deadlineMs, that.deadlineMs);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(modeOverride, includeTrace, deadlineMs);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "Options[" + "modeOverride=" + modeOverride + ", " + "includeTrace=" + includeTrace + ", " + "deadlineMs=" + deadlineMs + "]";
        }
    }
}
