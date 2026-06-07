package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record GrowthLoyaltyResult(
        String status,
        Map<String, Object> payload) {
}
