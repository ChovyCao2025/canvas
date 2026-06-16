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

/**
 * 定义 ExecutionRequestCatalog 的执行上下文数据结构或业务契约。
 */
public class ExecutionRequestCatalog {

    /**
     * 保存 DEFAULT_BATCH_LIMIT 对应的状态或配置。
     */
    private static final int DEFAULT_BATCH_LIMIT = 100;

    /**
     * 保存 MAX_BATCH_LIMIT 对应的状态或配置。
     */
    private static final int MAX_BATCH_LIMIT = 500;
    private static final Set<String> REPLAYABLE_STATUSES = Set.of("FAILED", "RETRY");

    private final List<ExecutionRequest> requests = new ArrayList<>();

    /**
     * 执行 ExecutionRequestCatalog 对应的业务处理。
     */
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

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
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

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param command command 参数
     * @return 处理后的结果
     */
    public ExecutionRequestFacade.ReplayResult replay(String id, ExecutionRequestFacade.ReplayCommand command) {
        ExecutionRequest request = find(id);
        requireTenantAccess(request, command == null ? null : command.tenantId());
        requireReplayable(request.status(), command != null && command.force());
        request.markPending(command == null ? null : command.operator(), command == null ? null : command.reason());
        return new ExecutionRequestFacade.ReplayResult(request.id(), "QUEUED", true);
    }

    /**
     * 执行 replayBatch 对应的业务处理。
     * @param command command 参数
     * @return 处理后的结果
     */
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
            // 批量重放只把候选请求恢复为 PENDING，实际派发由上层应用服务控制。
            request.markPending("system", command == null ? null : command.reason());
            replayed.add(request.id());
        }
        return new ExecutionRequestFacade.BatchReplayResult(replayed.size(), limit, replayed, 0, List.of());
    }

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
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

    /**
     * 执行 find 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    private ExecutionRequest find(String id) {
        String requestId = requireText(id, "request id is required");
        return requests.stream()
                .filter(request -> Objects.equals(request.id(), requestId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("执行请求不存在: " + requestId));
    }

    /**
     * 执行 requireTenantAccess 对应的业务处理。
     * @param request request 参数
     * @param tenantId tenantId 参数
     */
    private static void requireTenantAccess(ExecutionRequest request, Long tenantId) {
        if (tenantId == null) {
            return;
        }
        if (!Objects.equals(request.tenantId(), tenantId)) {
            throw new IllegalArgumentException("跨租户执行请求访问被拒绝");
        }
    }

    /**
     * 执行 requireReplayable 对应的业务处理。
     * @param status status 参数
     * @param force force 参数
     */
    private static void requireReplayable(String status, boolean force) {
        if (!force && !REPLAYABLE_STATUSES.contains(status)) {
            // force=false 时只允许重放已失败或等待重试的请求，防止重复执行成功请求。
            throw new IllegalArgumentException("只能重放 FAILED/RETRY 状态的执行请求，其他状态请使用 force=true");
        }
    }

    /**
     * 执行 toView 对应的业务处理。
     * @param request request 参数
     * @return 处理后的结果
     */
    private static ExecutionRequestFacade.RequestView toView(ExecutionRequest request) {
        return new ExecutionRequestFacade.RequestView(request.id(), request.tenantId(), request.canvasId(),
                request.status(), request.userId(), request.sourceMsgId(), request.payload(),
                request.createdAt().toString(), request.updatedAt().toString());
    }

    /**
     * 执行 normalizeLimit 对应的业务处理。
     * @param limit limit 参数
     */
    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BATCH_LIMIT;
        }
        return Math.min(limit, MAX_BATCH_LIMIT);
    }

    /**
     * 执行 normalizePage 对应的业务处理。
     * @param page page 参数
     */
    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    /**
     * 执行 normalizePageSize 对应的业务处理。
     * @param size size 参数
     */
    private static int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    /**
     * 执行 normalizeBlank 对应的业务处理。
     * @param value value 参数
     */
    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
     * 执行 requireId 对应的业务处理。
     * @param id id 参数
     * @param message message 参数
     */
    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
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
     * 定义 ExecutionRequest 的执行上下文数据结构或业务契约。
     */
    private static final class ExecutionRequest {
        /**
         * 保存 id 对应的状态或配置。
         */
        private final String id;

        /**
         * 保存 tenantId 对应的状态或配置。
         */
        private final Long tenantId;

        /**
         * 保存 canvasId 对应的状态或配置。
         */
        private final Long canvasId;

        /**
         * 保存 status 对应的状态或配置。
         */
        private String status;

        /**
         * 保存 userId 对应的状态或配置。
         */
        private final String userId;

        /**
         * 保存 sourceMsgId 对应的状态或配置。
         */
        private final String sourceMsgId;

        /**
         * 保存 Map<String 对应的状态或配置。
         */
        private final Map<String, Object> payload;

        /**
         * 保存 createdAt 对应的状态或配置。
         */
        private final LocalDateTime createdAt;

        /**
         * 保存 updatedAt 对应的状态或配置。
         */
        private LocalDateTime updatedAt;

        /**
         * 保存 lastReplayOperator 对应的状态或配置。
         */
        private String lastReplayOperator;

        /**
         * 保存 lastReplayReason 对应的状态或配置。
         */
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

        /**
         * 执行 markPending 对应的业务处理。
         * @param operator operator 参数
         * @param reason reason 参数
         */
        private void markPending(String operator, String reason) {
            this.status = "PENDING";
            this.lastReplayOperator = defaultText(operator, "system");
            this.lastReplayReason = defaultText(reason, "");
            this.updatedAt = LocalDateTime.parse("2026-06-14T12:00:00");
        }

        /**
         * 执行 id 对应的业务处理。
         * @return 处理后的结果
         */
        private String id() {
            return id;
        }

        /**
         * 执行 tenantId 对应的业务处理。
         */
        private Long tenantId() {
            return tenantId;
        }

        /**
         * 执行 canvasId 对应的业务处理。
         */
        private Long canvasId() {
            return canvasId;
        }

        /**
         * 执行 status 对应的业务处理。
         * @return 处理后的结果
         */
        private String status() {
            return status;
        }

        /**
         * 执行 userId 对应的业务处理。
         */
        private String userId() {
            return userId;
        }

        /**
         * 执行 sourceMsgId 对应的业务处理。
         */
        private String sourceMsgId() {
            return sourceMsgId;
        }

        /**
         * 执行 payload 对应的业务处理。
         * @return 处理后的结果
         */
        private Map<String, Object> payload() {
            return payload;
        }

        /**
         * 执行 createdAt 对应的业务处理。
         */
        private LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 执行 updatedAt 对应的业务处理。
         */
        private LocalDateTime updatedAt() {
            return updatedAt;
        }
    }
}
