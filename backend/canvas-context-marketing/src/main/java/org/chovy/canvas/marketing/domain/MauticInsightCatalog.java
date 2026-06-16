package org.chovy.canvas.marketing.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护MauticInsight相关的内存业务目录。
 */
public class MauticInsightCatalog {

    /**
     * 执行audienceMembership业务操作。
     */
    public Map<String, Object> audienceMembership(Long audienceId, String userId) {
        return ordered(
                "audienceId", audienceId,
                "userId", userId,
                "member", true,
                "score", 87,
                "matchedSegments", List.of("high-intent", "email-engaged"),
                "evaluatedAt", Instant.EPOCH.toString());
    }

    /**
     * 执行journeyPath业务操作。
     */
    public Map<String, Object> journeyPath(String executionId) {
        return ordered(
                "executionId", executionId,
                "currentNode", "send-email",
                "path", List.of("trigger", "segment-check", "send-email"),
                "nextBestAction", "wait-for-click",
                "evaluatedAt", Instant.EPOCH.toString());
    }

    /**
     * 执行channelPreference业务操作。
     */
    public Map<String, Object> channelPreference(String userId, String preferredChannel) {
        String channel = preferredChannel == null || preferredChannel.isBlank() ? "email" : preferredChannel.trim();
        return ordered(
                "userId", userId,
                "preferredChannel", channel,
                "confidence", 0.91,
                "fallbackChannels", List.of("sms", "push"),
                "reason", "recent engagement favors " + channel);
    }

    /**
     * 执行suppressionTimeline业务操作。
     */
    public Map<String, Object> suppressionTimeline(String userId) {
        return ordered(
                "userId", userId,
                "suppressed", false,
                "events", List.of(
                        ordered("type", "consent_granted", "at", Instant.EPOCH.toString()),
                        ordered("type", "bounce_check_passed", "at", Instant.EPOCH.toString())));
    }

    /**
     * 执行publishHealth业务操作。
     */
    public Map<String, Object> publishHealth(Long canvasId) {
        return ordered(
                "canvasId", canvasId,
                "healthy", true,
                "checks", List.of(
                        ordered("name", "audience", "status", "PASS"),
                        ordered("name", "content", "status", "PASS"),
                        ordered("name", "frequency", "status", "WARN")),
                "evaluatedAt", Instant.EPOCH.toString());
    }

    /**
     * 执行frequencyTemplates业务操作。
     */
    public List<Map<String, Object>> frequencyTemplates() {
        return List.of(
                ordered("key", "conservative", "maxPerDay", 1, "maxPerWeek", 3),
                ordered("key", "balanced", "maxPerDay", 2, "maxPerWeek", 5),
                ordered("key", "aggressive", "maxPerDay", 3, "maxPerWeek", 7));
    }

    /**
     * 执行ordered业务操作。
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
