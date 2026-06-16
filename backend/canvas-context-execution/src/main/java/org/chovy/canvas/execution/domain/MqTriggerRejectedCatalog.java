package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.chovy.canvas.execution.api.MqTriggerRejectedFacade;

public class MqTriggerRejectedCatalog {

    private final List<RejectedMessage> rejectedMessages = new ArrayList<>();

    public MqTriggerRejectedCatalog() {
        rejectedMessages.add(new RejectedMessage(1001L, "signup-topic", "msg-1", "NO_ROUTE",
                ordered("userId", "user-1", "messageCode", "signup.created",
                        "payload", ordered("source", "landing")),
                List.of(), LocalDateTime.parse("2026-06-14T10:00:00")));
        rejectedMessages.add(new RejectedMessage(1002L, "payment-topic", "msg-2", "ROUTE_RETRY",
                ordered("userId", "user-2", "messageCode", "payment.created",
                        "payload", ordered("orderId", "O-2")),
                List.of("42", "bad-route", "43"), LocalDateTime.parse("2026-06-14T10:05:00")));
        rejectedMessages.add(new RejectedMessage(1003L, "broken-topic", "msg-3", "INVALID_JSON",
                ordered("raw", "{"), List.of("42"), LocalDateTime.parse("2026-06-14T09:00:00")));
    }

    public MqTriggerRejectedFacade.RejectedPageView list(MqTriggerRejectedFacade.RejectedQuery query) {
        String tag = normalizeBlank(query == null ? null : query.tag());
        String reason = normalizeBlank(query == null ? null : query.reason());
        int page = Math.max(1, query == null ? 1 : query.page());
        int size = normalizeSize(query == null ? 20 : query.size());
        List<MqTriggerRejectedFacade.RejectedView> filtered = rejectedMessages.stream()
                .filter(row -> tag == null || Objects.equals(row.tag(), tag))
                .filter(row -> reason == null || Objects.equals(row.reason(), reason))
                .sorted(Comparator.comparing(RejectedMessage::createdAt).reversed())
                .map(MqTriggerRejectedCatalog::toView)
                .toList();
        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new MqTriggerRejectedFacade.RejectedPageView(filtered.size(), page, size,
                filtered.subList(fromIndex, toIndex));
    }

    public MqTriggerRejectedFacade.RejectedView detail(Long id) {
        return toView(find(id));
    }

    public MqTriggerRejectedFacade.ReplayResult replay(Long id) {
        RejectedMessage rejected = find(id);
        validateBody(rejected.body());
        List<String> requestIds = new ArrayList<>();
        List<String> dispatchFailed = new ArrayList<>();
        for (String rawRoute : rejected.routes()) {
            Long canvasId = parseCanvasId(rawRoute);
            if (canvasId == null) {
                continue;
            }
            String requestId = "mq-replay-" + canvasId + "-" + rejected.msgId();
            requestIds.add(requestId);
            if (canvasId % 2 != 0) {
                dispatchFailed.add(requestId);
            }
        }
        return new MqTriggerRejectedFacade.ReplayResult(requestIds.size(), requestIds,
                dispatchFailed.size(), dispatchFailed);
    }

    public void register(MqTriggerRejectedFacade.RejectedCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("rejected command is required");
        }
        rejectedMessages.add(new RejectedMessage(
                requireId(command.id()),
                requireText(command.tag(), "tag is required"),
                requireText(command.msgId(), "msgId is required"),
                defaultText(command.reason(), ""),
                copy(command.body()),
                List.copyOf(command.routes() == null ? List.of() : command.routes()),
                LocalDateTime.parse("2026-06-14T11:00:00")));
    }

    private RejectedMessage find(Long id) {
        Long requiredId = requireId(id);
        return rejectedMessages.stream()
                .filter(row -> Objects.equals(row.id(), requiredId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("rejected 消息不存在: " + requiredId));
    }

    private static void validateBody(Map<String, Object> body) {
        if (body == null || body.containsKey("raw")) {
            throw new IllegalArgumentException("无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON");
        }
        if (isBlank(asString(body.get("userId")))
                || isBlank(asString(body.get("messageCode")))
                || !(body.get("payload") instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("无法重放 rejected 消息，缺少 userId/messageCode/payload");
        }
    }

    private static Long parseCanvasId(String raw) {
        try {
            long canvasId = Long.parseLong(raw);
            return canvasId > 0 ? canvasId : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static MqTriggerRejectedFacade.RejectedView toView(RejectedMessage row) {
        return new MqTriggerRejectedFacade.RejectedView(row.id(), row.tag(), row.msgId(), row.reason(),
                row.body(), row.createdAt().toString());
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

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private record RejectedMessage(
            Long id,
            String tag,
            String msgId,
            String reason,
            Map<String, Object> body,
            List<String> routes,
            LocalDateTime createdAt) {

        private RejectedMessage {
            body = copy(body);
            routes = List.copyOf(routes == null ? List.of() : routes);
        }
    }
}
