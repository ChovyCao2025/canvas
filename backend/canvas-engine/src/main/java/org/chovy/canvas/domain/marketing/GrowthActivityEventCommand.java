package org.chovy.canvas.domain.marketing;

import java.util.Map;

public record GrowthActivityEventCommand(
        Long participantId,
        String eventType,
        String eventKey,
        String sourceType,
        Long sourceId,
        Map<String, Object> payload) {
}
