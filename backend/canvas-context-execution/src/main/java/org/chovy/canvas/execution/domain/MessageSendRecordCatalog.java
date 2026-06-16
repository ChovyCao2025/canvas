package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageSendRecordFacade;

public class MessageSendRecordCatalog {

    private final List<MessageSendRecord> records = new ArrayList<>();

    public MessageSendRecordCatalog() {
        records.add(new MessageSendRecord(
                501L,
                7L,
                "exec-1",
                42L,
                "user-1",
                "node-message",
                "SMS",
                "tpl-1",
                "idem-501",
                "{\"body\":\"old\"}",
                "SENT",
                "pm-501",
                null,
                LocalDateTime.parse("2026-06-12T10:00:00"),
                LocalDateTime.parse("2026-06-12T10:04:00")));
        records.add(new MessageSendRecord(
                502L,
                7L,
                "exec-1",
                42L,
                "user-1",
                "node-message",
                "SMS",
                "tpl-2",
                "idem-502",
                "{\"body\":\"new\"}",
                "SENT",
                "pm-502",
                null,
                LocalDateTime.parse("2026-06-12T10:05:00"),
                LocalDateTime.parse("2026-06-12T10:06:00")));
        records.add(new MessageSendRecord(
                503L,
                8L,
                "exec-2",
                43L,
                "user-2",
                "node-email",
                "EMAIL",
                "tpl-3",
                "idem-503",
                "{\"body\":\"failed\"}",
                "FAILED",
                null,
                "provider timeout",
                LocalDateTime.parse("2026-06-12T11:00:00"),
                LocalDateTime.parse("2026-06-12T11:01:00")));
    }

    public synchronized MessageSendRecordFacade.MessageSendRecordPageView search(
            MessageSendRecordFacade.MessageSendRecordQuery query) {
        List<MessageSendRecord> filtered = records.stream()
                .filter(record -> matches(query.canvasId(), record.canvasId))
                .filter(record -> matchesText(query.executionId(), record.executionId))
                .filter(record -> matchesText(query.userId(), record.userId))
                .filter(record -> matchesNormalized(query.channel(), record.channel))
                .filter(record -> matchesNormalized(query.status(), record.status))
                .filter(record -> query.startAt() == null || !record.createdAt.isBefore(query.startAt()))
                .filter(record -> query.endAt() == null || !record.createdAt.isAfter(query.endAt()))
                .sorted(Comparator.comparing(MessageSendRecord::createdAt).reversed())
                .toList();
        int from = Math.min((query.page() - 1) * query.size(), filtered.size());
        int to = Math.min(from + query.size(), filtered.size());
        return new MessageSendRecordFacade.MessageSendRecordPageView(filtered.size(), filtered.subList(from, to));
    }

    public synchronized Optional<MessageSendRecord> findById(Long id) {
        return records.stream()
                .filter(record -> Objects.equals(record.id, id))
                .findFirst();
    }

    private static boolean matches(Long expected, Long actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    private static boolean matchesText(String expected, String actual) {
        return expected == null || expected.isBlank() || Objects.equals(expected, actual);
    }

    private static boolean matchesNormalized(String expected, String actual) {
        return expected == null || expected.isBlank()
                || normalize(expected).equals(normalize(actual));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record MessageSendRecord(
            Long id,
            Long tenantId,
            String executionId,
            Long canvasId,
            String userId,
            String nodeId,
            String channel,
            String templateId,
            String idempotencyKey,
            String requestPayload,
            String status,
            String externalMessageId,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
