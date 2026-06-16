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

/**
 * 维护 CdpWebhook 的内存目录和查询视图。
 */
public class CdpWebhookCatalog {

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<Long, List<Delivery>> deliveries = new ConcurrentHashMap<>();

    /**
     * 创建当前组件实例。
     */
    public CdpWebhookCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 查询list列表。
     */
    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = subscriptions.values().stream()
                .filter(row -> Objects.equals(row.tenantId, tenantId))
                .sorted(Comparator.comparing(Subscription::id))
                .map(this::view)
                .toList();
        return page(records);
    }

    /**
     * 创建create。
     */
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

    /**
     * 更新update。
     */
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

    /**
     * 执行 transition 对应的 CDP 业务操作。
     */
    public Map<String, Object> transition(Long tenantId, Long id, String status, String actor) {
        Subscription existing = find(tenantId, id);
        Subscription updated = existing.withStatus(status, actor, now());
        subscriptions.put(id, updated);
        return view(updated);
    }

    /**
     * 执行 rotateSecret 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 testDelivery 对应的 CDP 业务操作。
     */
    public Map<String, Object> testDelivery(Long tenantId, Long id, String actor) {
        Subscription subscription = find(tenantId, id);
        List<Delivery> rows = deliveries.computeIfAbsent(id, ignored -> new ArrayList<>());
        Delivery delivery = new Delivery(rows.size() + 1L, subscription, "webhook.test", "QUEUED", actor, now());
        rows.add(delivery);
        return deliveryView(delivery);
    }

    /**
     * 执行 deliveries 对应的 CDP 业务操作。
     */
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

    /**
     * 查找find。
     */
    private Subscription find(Long tenantId, Long id) {
        Subscription subscription = subscriptions.get(id);
        if (subscription == null || !Objects.equals(subscription.tenantId, tenantId)) {
            throw new IllegalArgumentException("Webhook subscription not found: " + id);
        }
        return subscription;
    }

    /**
     * 执行 view 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 deliveryView 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 page 对应的 CDP 业务操作。
     */
    private static Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private String now() {
        return OffsetDateTime.now(clock).toString();
    }

    /**
     * 执行 secretPrefix 对应的 CDP 业务操作。
     */
    private static String secretPrefix(long id) {
        return "whsec_" + id;
    }

    /**
     * 读取并校验必填的d Text。
     */
    private static String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    /**
     * 执行 text 对应的 CDP 业务操作。
     */
    private static String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    /**
     * 执行 callbackUrl 对应的 CDP 业务操作。
     */
    private static String callbackUrl(Map<String, Object> payload) {
        String callbackUrl = requiredText(payload, "callbackUrl");
        if (!callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException("callbackUrl must start with https://");
        }
        return callbackUrl;
    }

    /**
     * 执行 maxAttempts 对应的 CDP 业务操作。
     */
    private static Integer maxAttempts(Map<String, Object> payload) {
        Object value = payload.get("maxAttempts");
        if (value instanceof Number number) {
            return Math.max(1, Math.min(10, number.intValue()));
        }
        return 3;
    }

    /**
     * 执行 eventTypes 对应的 CDP 业务操作。
     */
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

    /**
     * 表示 Subscription 的业务数据或处理组件。
     */
    private static final class Subscription {

        /**
         * 唯一标识。
         */
        private final long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 名称。
         */
        private final String name;

        /**
         * callback Url。
         */
        private final String callbackUrl;

        /**
         * event Types。
         */
        private final List<String> eventTypes;

        /**
         * max Attempts。
         */
        private final Integer maxAttempts;

        /**
         * 状态。
         */
        private final String status;

        /**
         * secret Prefix。
         */
        private final String secretPrefix;

        /**
         * updated By。
         */
        private final String updatedBy;

        /**
         * 更新时间。
         */
        private final String updatedAt;

        /**
         * 使用记录字段创建 Subscription。
         */
        private Subscription(
                long id,
                Long tenantId,
                String name,
                String callbackUrl,
                List<String> eventTypes,
                Integer maxAttempts,
                String status,
                String secretPrefix,
                String updatedBy,
                String updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.callbackUrl = callbackUrl;
            this.eventTypes = eventTypes;
            this.maxAttempts = maxAttempts;
            this.status = status;
            this.secretPrefix = secretPrefix;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }

/**
 * 返回替换Status后的副本。
 */
Subscription withStatus(String nextStatus, String actor, String time) {
            return new Subscription(id, tenantId, name, callbackUrl, eventTypes, maxAttempts, nextStatus,
                    secretPrefix, actor, time);
        }

        /**
         * 返回替换Secret后的副本。
         */
        Subscription withSecret(String nextPrefix, String actor, String time) {
            return new Subscription(id, tenantId, name, callbackUrl, eventTypes, maxAttempts, status,
                    nextPrefix, actor, time);
        }

        /**
         * 返回唯一标识。
         */
        public long id() {
            return id;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回名称。
         */
        public String name() {
            return name;
        }

        /**
         * 返回callback Url。
         */
        public String callbackUrl() {
            return callbackUrl;
        }

        /**
         * 返回event Types。
         */
        public List<String> eventTypes() {
            return eventTypes;
        }

        /**
         * 返回max Attempts。
         */
        public Integer maxAttempts() {
            return maxAttempts;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回secret Prefix。
         */
        public String secretPrefix() {
            return secretPrefix;
        }

        /**
         * 返回updated By。
         */
        public String updatedBy() {
            return updatedBy;
        }

        /**
         * 返回更新时间。
         */
        public String updatedAt() {
            return updatedAt;
        }

        /**
         * 按所有字段比较 Subscription。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Subscription that = (Subscription) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(callbackUrl, that.callbackUrl)
                    && java.util.Objects.equals(eventTypes, that.eventTypes)
                    && java.util.Objects.equals(maxAttempts, that.maxAttempts)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(secretPrefix, that.secretPrefix)
                    && java.util.Objects.equals(updatedBy, that.updatedBy)
                    && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据所有字段计算 Subscription 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, name, callbackUrl, eventTypes, maxAttempts, status, secretPrefix, updatedBy, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "Subscription[" + "id=" + id + ", tenantId=" + tenantId + ", name=" + name + ", callbackUrl=" + callbackUrl + ", eventTypes=" + eventTypes + ", maxAttempts=" + maxAttempts + ", status=" + status + ", secretPrefix=" + secretPrefix + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 表示 Delivery 的业务数据或处理组件。
     */
    private static final class Delivery {

        /**
         * 唯一标识。
         */
        private final long id;

        /**
         * subscription。
         */
        private final Subscription subscription;

        /**
         * 事件类型。
         */
        private final String eventType;

        /**
         * 状态。
         */
        private final String status;

        /**
         * 操作人。
         */
        private final String actor;

        /**
         * 创建时间。
         */
        private final String createdAt;

        /**
         * 使用记录字段创建 Delivery。
         */
        private Delivery(
                long id,
                Subscription subscription,
                String eventType,
                String status,
                String actor,
                String createdAt) {
            this.id = id;
            this.subscription = subscription;
            this.eventType = eventType;
            this.status = status;
            this.actor = actor;
            this.createdAt = createdAt;
        }

        /**
         * 返回唯一标识。
         */
        public long id() {
            return id;
        }

        /**
         * 返回subscription。
         */
        public Subscription subscription() {
            return subscription;
        }

        /**
         * 返回事件类型。
         */
        public String eventType() {
            return eventType;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回操作人。
         */
        public String actor() {
            return actor;
        }

        /**
         * 返回创建时间。
         */
        public String createdAt() {
            return createdAt;
        }

        /**
         * 按所有字段比较 Delivery。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Delivery that = (Delivery) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(subscription, that.subscription)
                    && java.util.Objects.equals(eventType, that.eventType)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(actor, that.actor)
                    && java.util.Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据所有字段计算 Delivery 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, subscription, eventType, status, actor, createdAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "Delivery[" + "id=" + id + ", subscription=" + subscription + ", eventType=" + eventType + ", status=" + status + ", actor=" + actor + ", createdAt=" + createdAt + "]";
        }
    }
}
