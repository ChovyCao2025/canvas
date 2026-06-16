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

/**
 * 定义 DlqCatalog 的执行上下文数据结构或业务契约。
 */
public class DlqCatalog {

    private final AtomicLong replaySequence = new AtomicLong(1);
    private final List<DeadLetter> deadLetters = new ArrayList<>();

    /**
     * 执行 DlqCatalog 对应的业务处理。
     */
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

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
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

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param skipSuccessNodes skipSuccessNodes 参数
     * @return 处理后的结果
     */
    public DlqFacade.DlqReplayResult replay(Long id, boolean skipSuccessNodes) {
        DeadLetter entry = find(id);
        return new DlqFacade.DlqReplayResult(entry.id(), entry.canvasId(), entry.userId(), entry.triggerType(),
                entry.triggerNodeType(), entry.matchKey(), entry.payload(), skipSuccessNodes,
                "dlq-replay-" + entry.id() + "-" + replaySequence.getAndIncrement());
    }

    /**
     * 执行 delete 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    public DlqFacade.DeleteResult delete(Long id) {
        boolean removed = deadLetters.removeIf(entry -> Objects.equals(entry.id(), requireId(id)));
        return new DlqFacade.DeleteResult(id, removed);
    }

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
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

    /**
     * 执行 find 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    private DeadLetter find(Long id) {
        Long requiredId = requireId(id);
        return deadLetters.stream()
                .filter(entry -> Objects.equals(entry.id(), requiredId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DLQ 记录不存在: " + requiredId));
    }

    /**
     * 执行 toView 对应的业务处理。
     * @param entry entry 参数
     * @return 处理后的结果
     */
    private static DlqFacade.DlqEntryView toView(DeadLetter entry) {
        return new DlqFacade.DlqEntryView(entry.id(), entry.canvasId(), entry.userId(), entry.triggerType(),
                entry.triggerNodeType(), entry.matchKey(), entry.payload(), entry.errorMessage(),
                entry.failedAt().toString());
    }

    /**
     * 执行 requireId 对应的业务处理。
     * @param id id 参数
     */
    private static Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id is required");
        }
        return id;
    }

    /**
     * 执行 requireText 对应的业务处理。
     * @param value value 参数
     * @param message message 参数
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 执行 defaultText 对应的业务处理。
     * @param value value 参数
     * @param fallback fallback 参数
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 执行 normalizePage 对应的业务处理。
     * @param page page 参数
     */
    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    /**
     * 执行 normalizeSize 对应的业务处理。
     * @param size size 参数
     */
    private static int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    /**
     * 执行 ordered 对应的业务处理。
     * @param values values 参数
     * @return 处理后的结果
     */
    private static Map<String, Object> ordered(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    /**
     * 执行 copy 对应的业务处理。
     * @param values values 参数
     * @return 处理后的结果
     */
    private static Map<String, Object> copy(Map<String, Object> values) {
        return Map.copyOf(values == null ? Map.of() : values);
    }

    /**
     * 定义 DeadLetter 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param triggerNodeType triggerNodeType 对应的数据字段
     * @param matchKey matchKey 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param errorMessage errorMessage 对应的数据字段
     * @param failedAt failedAt 对应的数据字段
     */
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
