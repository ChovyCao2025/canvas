package org.chovy.canvas.domain.bi.chart;

/**
 * 描述依赖 BI 图表的定时订阅。
 *
 * @param subscriptionKey 定时订阅的稳定标识
 * @param name 影响提示和清理页面中展示的订阅名称
 * @param enabled 订阅当前是否启用，启用后可能继续执行
 */
public record BiChartSubscriptionReference(
        String subscriptionKey,
        String name,
        Boolean enabled
) {
}
