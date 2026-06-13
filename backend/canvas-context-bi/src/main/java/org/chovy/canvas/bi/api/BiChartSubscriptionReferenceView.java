package org.chovy.canvas.bi.api;

public record BiChartSubscriptionReferenceView(
        String subscriptionKey,
        String name,
        Boolean enabled) {
}
