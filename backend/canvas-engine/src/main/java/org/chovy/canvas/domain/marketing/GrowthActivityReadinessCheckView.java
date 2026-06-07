package org.chovy.canvas.domain.marketing;

public record GrowthActivityReadinessCheckView(
        String severity,
        String itemType,
        String itemKey,
        String title,
        String reason,
        String route) {
}
