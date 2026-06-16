package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageDeliveryFacade;

/**
 * 定义 MessageDeliveryCatalog 的执行上下文数据结构或业务契约。
 */
public class MessageDeliveryCatalog {

    private final List<Delivery> deliveries = new ArrayList<>();
    private final List<Receipt> receipts = new ArrayList<>();

    /**
     * 执行 MessageDeliveryCatalog 对应的业务处理。
     */
    public MessageDeliveryCatalog() {
        deliveries.add(new Delivery(
                1001L,
                7L,
                42L,
                "exec-1",
                501L,
                "user-1",
                "message",
                "SMS",
                "twilio",
                "{\"body\":\"sent\"}",
                "idem-1001",
                "SENT",
                1,
                null,
                null,
                null,
                "pm-sent-1",
                "{\"ok\":true}",
                "",
                LocalDateTime.parse("2026-06-12T10:00:00"),
                LocalDateTime.parse("2026-06-12T10:04:00"),
                false));
        deliveries.add(new Delivery(
                1002L,
                7L,
                42L,
                "exec-1",
                502L,
                "user-1",
                "message",
                "SMS",
                "twilio",
                "{\"body\":\"dead\"}",
                "idem-1002",
                "DEAD",
                3,
                LocalDateTime.parse("2026-06-12T10:10:00"),
                null,
                "provider timeout",
                "pm-dead-1",
                "{\"error\":\"timeout\"}",
                "timeout",
                LocalDateTime.parse("2026-06-12T10:01:00"),
                LocalDateTime.parse("2026-06-12T10:05:00"),
                false));
        deliveries.add(new Delivery(
                1003L,
                8L,
                43L,
                "exec-reconcile",
                503L,
                "user-2",
                "message",
                "EMAIL",
                "sendgrid",
                "{\"body\":\"stale\"}",
                "idem-1003",
                "DEAD",
                2,
                LocalDateTime.parse("2026-06-12T10:11:00"),
                null,
                "stale pending timeout",
                "pm-stale-1",
                "{\"error\":\"stale\"}",
                "timeout",
                LocalDateTime.parse("2026-06-12T09:59:00"),
                LocalDateTime.parse("2026-06-12T10:05:00"),
                false));
        receipts.add(new Receipt(
                5001L,
                7L,
                1001L,
                "twilio",
                "pm-sent-1",
                "SENT",
                "{\"type\":\"SENT\"}",
                "receipt-5001",
                LocalDateTime.parse("2026-06-12T10:02:00"),
                LocalDateTime.parse("2026-06-12T10:02:00")));
        receipts.add(new Receipt(
                5002L,
                7L,
                1001L,
                "twilio",
                "pm-sent-1",
                "DELIVERED",
                "{\"type\":\"DELIVERED\"}",
                "receipt-5002",
                LocalDateTime.parse("2026-06-12T10:03:00"),
                LocalDateTime.parse("2026-06-12T10:03:00")));
        receipts.add(new Receipt(
                5003L,
                7L,
                1002L,
                "twilio",
                "pm-dead-1",
                "FAILED",
                "{\"type\":\"FAILED\"}",
                "receipt-5003",
                LocalDateTime.parse("2026-06-12T10:06:00"),
                LocalDateTime.parse("2026-06-12T10:06:00")));
    }

    public synchronized MessageDeliveryFacade.DeliveryPageView search(
            MessageDeliveryFacade.DeliverySearchQuery query) {
        List<Delivery> filtered = deliveries.stream()
                .filter(delivery -> matches(query.tenantId(), delivery.tenantId))
                .filter(delivery -> matches(query.canvasId(), delivery.canvasId))
                .filter(delivery -> matchesText(query.executionId(), delivery.executionId))
                .filter(delivery -> matchesText(query.userId(), delivery.userId))
                .filter(delivery -> matchesNormalized(query.channel(), delivery.channel))
                .filter(delivery -> matchesNormalized(query.provider(), delivery.provider))
                .filter(delivery -> matchesNormalized(query.status(), delivery.status))
                .filter(delivery -> matchesText(query.providerMessageId(), delivery.providerMessageId))
                .sorted(Comparator.comparing(Delivery::createdAt).reversed())
                .toList();
        int from = Math.min((query.page() - 1) * query.size(), filtered.size());
        int to = Math.min(from + query.size(), filtered.size());
        return new MessageDeliveryFacade.DeliveryPageView(filtered.size(), filtered.subList(from, to));
    }

    /**
     * 执行 findById 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    public synchronized Optional<Delivery> findById(Long id) {
        return deliveries.stream()
                .filter(delivery -> Objects.equals(delivery.id, id))
                .findFirst();
    }

    /**
     * 执行 receipts 对应的业务处理。
     * @param outboxId outboxId 参数
     * @return 处理后的结果
     */
    public synchronized List<Receipt> receipts(Long outboxId) {
        return receipts.stream()
                .filter(receipt -> Objects.equals(receipt.outboxId, outboxId))
                .sorted(Comparator.comparing(Receipt::receivedAt).reversed())
                .toList();
    }

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    public synchronized MessageDeliveryFacade.ReplayResultView replay(Long id) {
        Optional<Delivery> delivery = findById(id);
        if (delivery.isEmpty() || !"DEAD".equalsIgnoreCase(delivery.get().status)) {
            return new MessageDeliveryFacade.ReplayResultView(id, null, false);
        }
        Delivery replayed = delivery.get().withStatus("PENDING");
        deliveries.remove(delivery.get());
        deliveries.add(replayed);
        return new MessageDeliveryFacade.ReplayResultView(id, "PENDING", true);
    }

    /**
     * 执行 reconcile 对应的业务处理。
     * @return 处理后的结果
     */
    public synchronized MessageDeliveryFacade.ReconcileResultView reconcile() {
        int requeued = 0;
        for (int index = 0; index < deliveries.size(); index++) {
            Delivery delivery = deliveries.get(index);
            if ("DEAD".equalsIgnoreCase(delivery.status)) {
                deliveries.set(index, delivery.withStatus("PENDING"));
                requeued++;
            }
        }
        return new MessageDeliveryFacade.ReconcileResultView(requeued);
    }

    /**
     * 执行 matches 对应的业务处理。
     * @param expected expected 参数
     * @param actual actual 参数
     * @return 处理后的结果
     */
    private static boolean matches(Long expected, Long actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    /**
     * 执行 matchesText 对应的业务处理。
     * @param expected expected 参数
     * @param actual actual 参数
     */
    private static boolean matchesText(String expected, String actual) {
        return expected == null || expected.isBlank() || Objects.equals(expected, actual);
    }

    /**
     * 执行 matchesNormalized 对应的业务处理。
     * @param expected expected 参数
     * @param actual actual 参数
     */
    private static boolean matchesNormalized(String expected, String actual) {
        return expected == null || expected.isBlank()
                || normalize(expected).equals(normalize(actual));
    }

    /**
     * 执行 normalize 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 定义 Delivery 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     * @param messageSendRecordId messageSendRecordId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param nodeType nodeType 对应的数据字段
     * @param channel channel 对应的数据字段
     * @param provider provider 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param idempotencyKey idempotencyKey 对应的数据字段
     * @param status status 对应的数据字段
     */
    public record Delivery(
            Long id,
            Long tenantId,
            Long canvasId,
            String executionId,
            Long messageSendRecordId,
            String userId,
            String nodeType,
            String channel,
            String provider,
            String payload,
            String idempotencyKey,
            String status,
            Integer attempt,
            LocalDateTime nextRetryAt,
            LocalDateTime claimedAt,
            String errorMessage,
            String providerMessageId,
            String providerResponse,
            String currentReceiptStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Boolean duplicate) {
        private Delivery withStatus(String status) {
            return new Delivery(id, tenantId, canvasId, executionId, messageSendRecordId, userId, nodeType, channel,
                    provider, payload, idempotencyKey, status, attempt, nextRetryAt, claimedAt, errorMessage,
                    providerMessageId, providerResponse, currentReceiptStatus, createdAt, LocalDateTime.now(),
                    duplicate);
        }
    }

    /**
     * 定义 Receipt 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param outboxId outboxId 对应的数据字段
     * @param provider provider 对应的数据字段
     * @param providerMessageId providerMessageId 对应的数据字段
     * @param receiptType receiptType 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param idempotencyKey idempotencyKey 对应的数据字段
     * @param receivedAt receivedAt 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
    public record Receipt(
            Long id,
            Long tenantId,
            Long outboxId,
            String provider,
            String providerMessageId,
            String receiptType,
            String payload,
            String idempotencyKey,
            LocalDateTime receivedAt,
            LocalDateTime createdAt) {
    }
}
