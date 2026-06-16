package org.chovy.canvas.bi.api;
/**
 * BiChartSubscriptionReferenceView 视图。
 */
public record BiChartSubscriptionReferenceView(
        /**
         * subscriptionKey 对应的业务键。
         */
        String subscriptionKey,
        /**
         * 展示名称。
         */
        String name,
        Boolean enabled) {
}
