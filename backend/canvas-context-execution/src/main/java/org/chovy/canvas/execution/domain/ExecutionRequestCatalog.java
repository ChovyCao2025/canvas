package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;

public class ExecutionRequestCatalog {

    private static final int DEFAULT_BATCH_LIMIT = 100;
    private static final int MAX_BATCH_LIMIT = 500;
    private static final Set<String> REPLAYABLE_STATUSES = Set.of("FAILED", "RETRY");

    private final List<ExecutionRequest> requests = new ArrayList<>();

    public ExecutionRequestCatalog() {
        requests.add(new ExecutionRequest("req-1", 7L, 42L, "FAILED", "user-1", "msg-1",
                ordered("couponCode", "A10"), LocalDateTime.parse("2026-06-14T10:00:00"),
                LocalDateTime.parse("2026-06-14T10:05:00")));
        requests.add(new ExecutionRequest("req-2", 7L, 42L, "RETRY", "user-2", "msg-2",
                ordered("couponCode", "B20"), LocalDateTime.parse("2026-06-14T10:01:00"),
                LocalDateTime.parse("2026-06-14T10:04:00")));
        requests.add(new ExecutionRequest("req-3", 7L, 42L, "SUCCESS", "user-3", "msg-3",
                ordered("couponCode", "C30"), LocalDateTime.parse("2026-06-14T10:02:00"),
                LocalDateTime.parse("2026-06-14T10:03:00")));
        requests.add(new ExecutionRequest("req-4", 8L, 42L, "FAILED", "user-4", "msg-4",
                ordered("couponCode", "D40"), LocalDateTime.parse("2026-06-14T10:03:00"),
                LocalDateTime.parse("2026-06-14T10:02:00")));
    }

    public ExecutionRequestFacade.RequestPageView list(ExecutionRequestFacade.RequestQuery query) {
        Long tenantId = query == null ? null : query.tenantId();
        Long canvasId = query == null ? null : query.canvasId();
        String status = normalizeBlank(query == null ? null : query.status());
        String userId = normalizeBlank(query == null ? null : query.userId());
        String sourceMsgId = normalizeBlank(query == null ? null : query.sourceMsgId());
        int page = normalizePage(query == null ? 1 : query.page());
        int size = normalizePageSize(query == null ? 20 : query.size());
        List<ExecutionRequestFacade.RequestView> filtered = requests.stream()
                .filter(request -> tenantId == null || Objects.equals(request.tenantId(), tenantId))
                .filter(request -> canvasId == null || Objects.equals(request.canvasId(), canvasId))
                .filter(request -> status == null || Objects.equals(request.status(), status))
                .filter(request -> userId == null || Objects.equals(request.userId(), userId))
                .filter(request -> sourceMsgId == null || Objects.equals(request.sourceMsgId(), sourceMsgId))
                .sorted(Comparator.comparing(ExecutionRequest::updatedAt).reversed())
                .map(ExecutionRequestCatalog::toView)
                .toList();
        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new ExecutionRequestFacade.RequestPageView(filtered.size(), page, size,
                filtered.subList(fromIndex, toIndex));
    }

    public ExecutionRequestFacade.ReplayResult replay(String id, ExecutionRequestFacade.ReplayCommand command) {
        ExecutionRequest request = find(id);
        requireTenantAccess(request, command == null ? null : command.tenantId());
        requireReplayable(request.status(), command != null && command.force());
        request.markPending(command == null ? null : command.operator(), command == null ? null : command.reason());
        return new ExecutionRequestFacade.ReplayResult(request.id(), "QUEUED", true);
    }

    public ExecutionRequestFacade.BatchReplayResult replayBatch(ExecutionRequestFacade.BatchReplayCommand command) {
        int limit = normalizeLimit(command == null ? DEFAULT_BATCH_LIMIT : command.limit());
        String status = normalizeBlank(command == null ? null : command.status());
        boolean force = command != null && command.force();
        if (status != null && !force && !REPLAYABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("批量重放默认只允许 FAILED/RETRY，其他状态请使用 force=true");
        }
        List<ExecutionRequest> candidates = requests.stream()
                .filter(request -> command == null || command.tenantId() == null
                        || Objects.equals(request.tenantId(), command.tenantId()))
                .filter(request -> command == null || command.canvasId() == null
                        || Objects.equals(request.canvasId(), command.canvasId()))
                .filter(request -> command == null || normalizeBlank(command.userId()) == null
                        || Objects.equals(request.userId(), normalizeBlank(command.userId())))
                .filter(request -> command == null || normalizeBlank(command.sourceMsgId()) == null
                        || Objects.equals(request.sourceMsgId(), normalizeBlank(command.sourceMsgId())))
                .filter(request -> status == null ? force || REPLAYABLE_STATUSES.contains(request.status())
                        : Objects.equals(request.status(), status))
                .sorted(Comparator.comparing(ExecutionRequest::updatedAt))
                .limit(limit)
                .toList();
        List<String> replayed = new ArrayList<>(candidates.size());
        for (ExecutionRequest request : candidates) {
            request.markPending("system", command == null ? null : command.reason());
            replayed.add(request.id());
        }
        return new ExecutionRequestFacade.BatchReplayResult(replayed.size(), limit, replayed, 0, List.of());
    }

    public void register(ExecutionRequestFacade.RequestCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("request command is required");
        }
        requests.add(new ExecutionRequest(
                requireText(command.id(), "request id is required"),
                requireId(command.tenantId(), "tenantId is required"),
                requireId(command.canvasId(), "canvasId is required"),
                defaultText(command.status(), "PENDING"),
                requireText(command.userId(), "userId is required"),
                defaultText(command.sourceMsgId(), ""),
                copy(command.payload()),
                LocalDateTime.parse("2026-06-14T11:00:00"),
                LocalDateTime.parse("2026-06-14T11:00:00")));
    }

    private ExecutionRequest find(String id) {
        String requestId = requireText(id, "request id is required");
        return requests.stream()
                .filter(request -> Objects.equals(request.id(), requestId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("执行请求不存在: " + requestId));
    }

    private static void requireTenantAccess(ExecutionRequest request, Long tenantId) {
        if (tenantId == null) {
            return;
        }
        if (!Objects.equals(request.tenantId(), tenantId)) {
            throw new IllegalArgumentException("跨租户执行请求访问被拒绝");
        }
    }

    private static void requireReplayable(String status, boolean force) {
        if (!force && !REPLAYABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("只能重放 FAILED/RETRY 状态的执行请求，其他状态请使用 force=true");
        }
    }

    private static ExecutionRequestFacade.RequestView toView(ExecutionRequest request) {
        return new ExecutionRequestFacade.RequestView(request.id(), request.tenantId(), request.canvasId(),
                request.status(), request.userId(), request.sourceMsgId(), request.payload(),
                request.createdAt().toString(), request.updatedAt().toString());
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BATCH_LIMIT;
        }
        return Math.min(limit, MAX_BATCH_LIMIT);
    }

    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    private static int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
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

    private static final class ExecutionRequest {
        private final String id;
        private final Long tenantId;
        private final Long canvasId;
        private String status;
        private final String userId;
        private final String sourceMsgId;
        private final Map<String, Object> payload;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String lastReplayOperator;
        private String lastReplayReason;

        private ExecutionRequest(String id, Long tenantId, Long canvasId, String status, String userId,
                                 String sourceMsgId, Map<String, Object> payload, LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.canvasId = canvasId;
            this.status = status;
            this.userId = userId;
            this.sourceMsgId = sourceMsgId;
            this.payload = copy(payload);
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        private void markPending(String operator, String reason) {
            this.status = "PENDING";
            this.lastReplayOperator = defaultText(operator, "system");
            this.lastReplayReason = defaultText(reason, "");
            this.updatedAt = LocalDateTime.parse("2026-06-14T12:00:00");
        }

        private String id() {
            return id;
        }

        private Long tenantId() {
            return tenantId;
        }

        private Long canvasId() {
            return canvasId;
        }

        private String status() {
            return status;
        }

        private String userId() {
            return userId;
        }

        private String sourceMsgId() {
            return sourceMsgId;
        }

        private Map<String, Object> payload() {
            return payload;
        }

        private LocalDateTime createdAt() {
            return createdAt;
        }

        private LocalDateTime updatedAt() {
            return updatedAt;
        }
    }
}
