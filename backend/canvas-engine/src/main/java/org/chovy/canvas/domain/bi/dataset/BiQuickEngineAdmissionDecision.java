package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineAdmissionDecision record.
 * @param allowed 是否允许请求立即进入 Quick Engine 执行.
 * @param status 准入状态码，区分放行、排队、拒绝和容量告警等决策.
 * @param message 面向调用方展示的准入说明，通常包含排队或拒绝原因.
 * @param tenantPoolPolicy 命中的租户池容量策略，用于解释租户级并发和配额口径.
 * @param concurrencyQueue 当前并发队列视图，用于展示排队深度、等待位置和队列状态.
 */
public record BiQuickEngineAdmissionDecision(
        boolean allowed,
        String status,
        String message,
        BiQuickEngineTenantPoolPolicyView tenantPoolPolicy,
        BiQuickEngineConcurrencyQueueView concurrencyQueue) {

    /**
     * 判断准入结果是否代表请求已进入等待队列。
     *
     * <p>该判断同时兼容状态码和提示文案中的 QUEUE/QUEUED 标记，供 API 层决定返回排队视图，
     * 不改变原始准入状态。</p>
     *
     * @return {@code true} 表示请求未被拒绝但需要等待调度
     */
    public boolean queued() {
        return normalize(status).contains("QUEUE") || normalize(message).contains("QUEUED");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
