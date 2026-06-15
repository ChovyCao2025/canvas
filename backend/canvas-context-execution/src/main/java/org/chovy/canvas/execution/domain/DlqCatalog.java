package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.execution.api.DlqFacade;

public class DlqCatalog {

    private final AtomicLong replaySequence = new AtomicLong(1);
    private final List<DeadLetter> deadLetters = new ArrayList<>();

    public DlqCatalog() {
        deadLetters.add(new DeadLetter(1001L, 42L, "user-1", "DIRECT_CALL", "DIRECT_CALL",
                "manual", ordered("couponCode", "A10"), "provider timeout",
                LocalDateTime.parse("2026-06-14T10:00:00")));
        deadLetters.add(new DeadLetter(1002L, 42L, "user-2", "MQ_EVENT", "MESSAGE",
                "signup", ordered("event", "signup"), "template missing",
                LocalDateTime.parse("2026-06-14T10:05:00")));
        deadLetters.add(new DeadLetter(1003L, 99L, "user-3", "WAIT_EVENT", "WAIT",
                "order-paid", ordered("orderId", "O-3"), "wait timeout",
                LocalDateTime.parse("2026-06-14T09:00:00")));
    }

    public DlqFacade.DlqPageView list(DlqFacade.DlqQuery query) {
        Long canvasId = query == null ? null : query.canvasId();
        int page = normalizePage(query == null ? 1 : query.page());
        int size = normalizeSize(query == null ? 20 : query.size());
        List<DlqFacade.DlqEntryView> filtered = deadLetters.stream()
                .filter(entry -> canvasId == null || Objects.equals(entry.canvasId(), canvasId))
                .sorted(Comparator.comparing(DeadLetter::failedAt).reversed())
                .map(DlqCatalog::toView)
                .toList();
        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new DlqFacade.DlqPageView(filtered.size(), page, size, filtered.subList(fromIndex, toIndex));
    }

    public DlqFacade.DlqReplayResult replay(Long id, boolean skipSuccessNodes) {
        DeadLetter entry = find(id);
        return new DlqFacade.DlqReplayResult(entry.id(), entry.canvasId(), entry.userId(), entry.triggerType(),
                entry.triggerNodeType(), entry.matchKey(), entry.payload(), skipSuccessNodes,
                "dlq-replay-" + entry.id() + "-" + replaySequence.getAndIncrement());
    }

    public DlqFacade.DeleteResult delete(Long id) {
        boolean removed = deadLetters.removeIf(entry -> Objects.equals(entry.id(), requireId(id)));
        return new DlqFacade.DeleteResult(id, removed);
    }

    public void register(DlqFacade.DlqEntryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("DLQ command is required");
        }
        deadLetters.add(new DeadLetter(
                requireId(command.id()),
                requireId(command.canvasId()),
                requireText(command.userId(), "userId is required"),
                defaultText(command.triggerType(), "DLQ_REPLAY"),
                defaultText(command.triggerNodeType(), "DIRECT_CALL"),
                defaultText(command.matchKey(), ""),
                copy(command.payload()),
                defaultText(command.errorMessage(), ""),
                LocalDateTime.parse("2026-06-14T11:00:00")));
    }

    private DeadLetter find(Long id) {
        Long requiredId = requireId(id);
        return deadLetters.stream()
                .filter(entry -> Objects.equals(entry.id(), requiredId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DLQ 记录不存在: " + requiredId));
    }

    private static DlqFacade.DlqEntryView toView(DeadLetter entry) {
        return new DlqFacade.DlqEntryView(entry.id(), entry.canvasId(), entry.userId(), entry.triggerType(),
                entry.triggerNodeType(), entry.matchKey(), entry.payload(), entry.errorMessage(),
                entry.failedAt().toString());
    }

    private static Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id is required");
        }
        return id;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    private static int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private static Map<String, Object> ordered(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> copy(Map<String, Object> values) {
        return Map.copyOf(values == null ? Map.of() : values);
    }

    private record DeadLetter(
            Long id,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String errorMessage,
            LocalDateTime failedAt) {

        private DeadLetter {
            payload = copy(payload);
        }
    }
}
