package org.chovy.canvas.cdp.domain;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpWebhookCatalog {

    private final Clock clock;
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<Long, List<Delivery>> deliveries = new ConcurrentHashMap<>();

    public CdpWebhookCatalog(Clock clock) {
        this.clock = clock;
    }

    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = subscriptions.values().stream()
                .filter(row -> Objects.equals(row.tenantId, tenantId))
                .sorted(Comparator.comparing(Subscription::id))
                .map(this::view)
                .toList();
        return page(records);
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        String name = requiredText(payload, "name");
        String callbackUrl = callbackUrl(payload);
        long id = ids.incrementAndGet();
        Subscription subscription = new Subscription(
                id,
                tenantId,
                name,
                callbackUrl,
                eventTypes(payload),
                maxAttempts(payload),
                "ACTIVE",
                secretPrefix(id),
                actor,
                now());
        subscriptions.put(id, subscription);
        return view(subscription);
    }

    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Subscription existing = find(tenantId, id);
        Subscription updated = new Subscription(
                existing.id,
                tenantId,
                text(payload, "name", existing.name),
                payload.containsKey("callbackUrl") ? callbackUrl(payload) : existing.callbackUrl,
                payload.containsKey("eventTypes") ? eventTypes(payload) : existing.eventTypes,
                payload.containsKey("maxAttempts") ? maxAttempts(payload) : existing.maxAttempts,
                existing.status,
                existing.secretPrefix,
                actor,
                now());
        subscriptions.put(id, updated);
        return view(updated);
    }

    public Map<String, Object> transition(Long tenantId, Long id, String status, String actor) {
        Subscription existing = find(tenantId, id);
        Subscription updated = existing.withStatus(status, actor, now());
        subscriptions.put(id, updated);
        return view(updated);
    }

    public Map<String, Object> rotateSecret(Long tenantId, Long id, String actor) {
        Subscription existing = find(tenantId, id);
        Subscription updated = existing.withSecret(existing.secretPrefix + "_rotated", actor, now());
        subscriptions.put(id, updated);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenantId);
        view.put("subscriptionId", id);
        view.put("secret", "whsec_plain_" + id);
        view.put("secretPrefix", updated.secretPrefix);
        view.put("rotatedBy", actor);
        view.put("rotatedAt", updated.updatedAt);
        return view;
    }

    public Map<String, Object> testDelivery(Long tenantId, Long id, String actor) {
        Subscription subscription = find(tenantId, id);
        List<Delivery> rows = deliveries.computeIfAbsent(id, ignored -> new ArrayList<>());
        Delivery delivery = new Delivery(rows.size() + 1L, subscription, "webhook.test", "QUEUED", actor, now());
        rows.add(delivery);
        return deliveryView(delivery);
    }

    public Map<String, Object> deliveries(Long tenantId, Long id, Integer limit) {
        find(tenantId, id);
        List<Map<String, Object>> records = deliveries.getOrDefault(id, List.of()).stream()
                .limit(limit)
                .map(this::deliveryView)
                .toList();
        Map<String, Object> page = page(records);
        page.put("tenantId", tenantId);
        page.put("subscriptionId", id);
        page.put("limit", limit);
        return page;
    }

    private Subscription find(Long tenantId, Long id) {
        Subscription subscription = subscriptions.get(id);
        if (subscription == null || !Objects.equals(subscription.tenantId, tenantId)) {
            throw new IllegalArgumentException("Webhook subscription not found: " + id);
        }
        return subscription;
    }

    private Map<String, Object> view(Subscription row) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", row.id);
        view.put("subscriptionId", row.id);
        view.put("tenantId", row.tenantId);
        view.put("name", row.name);
        view.put("callbackUrl", row.callbackUrl);
        view.put("eventTypes", List.copyOf(row.eventTypes));
        view.put("maxAttempts", row.maxAttempts);
        view.put("status", row.status);
        view.put("secretPrefix", row.secretPrefix);
        view.put("createdBy", row.updatedBy);
        view.put("updatedBy", row.updatedBy);
        view.put("updatedAt", row.updatedAt);
        return view;
    }

    private Map<String, Object> deliveryView(Delivery delivery) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", delivery.id);
        view.put("tenantId", delivery.subscription.tenantId);
        view.put("subscriptionId", delivery.subscription.id);
        view.put("eventType", delivery.eventType);
        view.put("status", delivery.status);
        view.put("triggeredBy", delivery.actor);
        view.put("createdAt", delivery.createdAt);
        return view;
    }

    private static Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    private String now() {
        return OffsetDateTime.now(clock).toString();
    }

    private static String secretPrefix(long id) {
        return "whsec_" + id;
    }

    private static String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    private static String callbackUrl(Map<String, Object> payload) {
        String callbackUrl = requiredText(payload, "callbackUrl");
        if (!callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException("callbackUrl must start with https://");
        }
        return callbackUrl;
    }

    private static Integer maxAttempts(Map<String, Object> payload) {
        Object value = payload.get("maxAttempts");
        if (value instanceof Number number) {
            return Math.max(1, Math.min(10, number.intValue()));
        }
        return 3;
    }

    private static List<String> eventTypes(Map<String, Object> payload) {
        Object value = payload.get("eventTypes");
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of("webhook.test");
    }

    private record Subscription(long id,
                                Long tenantId,
                                String name,
                                String callbackUrl,
                                List<String> eventTypes,
                                Integer maxAttempts,
                                String status,
                                String secretPrefix,
                                String updatedBy,
                                String updatedAt) {

        Subscription withStatus(String nextStatus, String actor, String time) {
            return new Subscription(id, tenantId, name, callbackUrl, eventTypes, maxAttempts, nextStatus,
                    secretPrefix, actor, time);
        }

        Subscription withSecret(String nextPrefix, String actor, String time) {
            return new Subscription(id, tenantId, name, callbackUrl, eventTypes, maxAttempts, status,
                    nextPrefix, actor, time);
        }
    }

    private record Delivery(long id,
                            Subscription subscription,
                            String eventType,
                            String status,
                            String actor,
                            String createdAt) {
    }
}
