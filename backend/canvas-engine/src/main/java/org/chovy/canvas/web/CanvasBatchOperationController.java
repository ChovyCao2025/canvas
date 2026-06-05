package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/canvas/batch")
public class CanvasBatchOperationController {

    private static final int DEFAULT_FILTER_LIMIT = 100;
    private static final int MAX_BATCH_SIZE = 200;
    private static final String SUCCESS = "SUCCESS";
    private static final String SKIPPED = "SKIPPED";
    private static final String FAILED = "FAILED";

    private final CanvasService canvasService;
    private final CanvasOpsService opsService;
    private final CanvasMapper canvasMapper;
    private final TenantContextResolver tenantContextResolver;

    public CanvasBatchOperationController(CanvasService canvasService,
                                          CanvasOpsService opsService,
                                          CanvasMapper canvasMapper,
                                          TenantContextResolver tenantContextResolver) {
        this.canvasService = canvasService;
        this.opsService = opsService;
        this.canvasMapper = canvasMapper;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/{operation}")
    public Mono<R<BatchOperationResult>> run(@PathVariable String operation,
                                             @RequestBody BatchOperationRequest request) {
        String normalized = normalizeOperation(operation);
        return current().flatMap(context -> Mono.fromCallable(() -> {
                    requireAdmin(context);
                    List<Long> canvasIds = resolveCanvasIds(request, context);
                    List<BatchOperationItem> items = new ArrayList<>(canvasIds.size());
                    for (Long canvasId : canvasIds) {
                        items.add(runOne(normalized, canvasId, request, context));
                    }
                    return R.ok(BatchOperationResult.from(normalized, items));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private BatchOperationItem runOne(String operation,
                                      Long canvasId,
                                      BatchOperationRequest request,
                                      TenantContext context) {
        try {
            CanvasDO canvas = canvasService.requireTenantAccess(canvasId, tenantId(context), context.isSuperAdmin());
            return switch (operation) {
                case "PAUSE" -> pause(canvas, context);
                case "RESUME" -> resume(canvas, context);
                case "ARCHIVE" -> archive(canvas, context);
                case "CLONE" -> clone(canvas, request, context);
                default -> throw new IllegalArgumentException("unsupported batch operation: " + operation);
            };
        } catch (Exception e) {
            return new BatchOperationItem(canvasId, null, FAILED, e.getMessage());
        }
    }

    private BatchOperationItem pause(CanvasDO canvas, TenantContext context) {
        if (!Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "only published canvases can be paused");
        }
        canvasService.offline(canvas.getId(), operator(context));
        return new BatchOperationItem(canvas.getId(), null, SUCCESS, "paused");
    }

    private BatchOperationItem resume(CanvasDO canvas, TenantContext context) {
        if (Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "canvas already published");
        }
        if (Objects.equals(canvas.getStatus(), CanvasStatusEnum.ARCHIVED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "archived canvas cannot be resumed");
        }
        canvasService.publish(canvas.getId(), operator(context));
        return new BatchOperationItem(canvas.getId(), null, SUCCESS, "resumed");
    }

    private BatchOperationItem archive(CanvasDO canvas, TenantContext context) {
        if (Objects.equals(canvas.getStatus(), CanvasStatusEnum.ARCHIVED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "canvas already archived");
        }
        canvasService.archive(canvas.getId(), operator(context));
        return new BatchOperationItem(canvas.getId(), null, SUCCESS, "archived");
    }

    private BatchOperationItem clone(CanvasDO canvas, BatchOperationRequest request, TenantContext context) {
        CanvasDO cloned = opsService.clone(canvas.getId(), operator(context));
        applyCloneReplacements(cloned, request == null ? null : request.replacements());
        return new BatchOperationItem(canvas.getId(), cloned.getId(), SUCCESS, "cloned");
    }

    private void applyCloneReplacements(CanvasDO cloned, Map<String, String> replacements) {
        if (cloned == null || replacements == null || replacements.isEmpty()) {
            return;
        }
        boolean changed = false;
        if (hasText(replacements.get("name"))) {
            cloned.setName(replacements.get("name").trim());
            changed = true;
        } else {
            String replaced = replaceTokens(cloned.getName(), replacements);
            if (!Objects.equals(replaced, cloned.getName())) {
                cloned.setName(replaced);
                changed = true;
            }
        }
        if (hasText(replacements.get("description"))) {
            cloned.setDescription(replacements.get("description").trim());
            changed = true;
        } else {
            String replaced = replaceTokens(cloned.getDescription(), replacements);
            if (!Objects.equals(replaced, cloned.getDescription())) {
                cloned.setDescription(replaced);
                changed = true;
            }
        }
        if (changed) {
            canvasMapper.updateById(cloned);
        }
    }

    private String replaceTokens(String input, Map<String, String> replacements) {
        if (input == null || replacements == null || replacements.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (!hasText(entry.getKey())) {
                continue;
            }
            result = result.replace("${" + entry.getKey().trim() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private List<Long> resolveCanvasIds(BatchOperationRequest request, TenantContext context) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request != null && request.canvasIds() != null) {
            for (Long id : request.canvasIds()) {
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            BatchCanvasFilters filters = request == null ? null : request.filters();
            String nameFilter = filters != null && hasText(filters.name()) ? filters.name().trim() : null;
            String triggerTypeFilter = filters != null && hasText(filters.triggerType()) ? filters.triggerType().trim() : null;
            LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<CanvasDO>()
                    .eq(!context.isSuperAdmin(), CanvasDO::getTenantId, tenantId(context))
                    .eq(filters != null && filters.status() != null, CanvasDO::getStatus, filters == null ? null : filters.status())
                    .like(nameFilter != null, CanvasDO::getName, nameFilter)
                    .eq(triggerTypeFilter != null, CanvasDO::getTriggerType, triggerTypeFilter)
                    .orderByDesc(CanvasDO::getUpdatedAt)
                    .last("LIMIT " + normalizeLimit(filters == null ? null : filters.limit()));
            canvasMapper.selectList(wrapper).forEach(row -> {
                if (row != null && row.getId() != null) {
                    ids.add(row.getId());
                }
            });
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("canvasIds or filters must resolve at least one canvas");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batch size exceeds " + MAX_BATCH_SIZE);
        }
        return new ArrayList<>(ids);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_FILTER_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_BATCH_SIZE));
    }

    private String normalizeOperation(String operation) {
        if (!hasText(operation)) {
            throw new IllegalArgumentException("operation is required");
        }
        String normalized = operation.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAUSE", "RESUME", "ARCHIVE", "CLONE").contains(normalized)) {
            throw new IllegalArgumentException("unsupported batch operation: " + operation);
        }
        return normalized;
    }

    private void requireAdmin(TenantContext context) {
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new AccessDeniedException("batch canvas operations require admin role");
        }
    }

    private Mono<TenantContext> current() {
        return tenantContextResolver.current().defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    private String operator(TenantContext context) {
        return hasText(context.username()) ? context.username().trim() : "system";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record BatchOperationRequest(List<Long> canvasIds,
                                        BatchCanvasFilters filters,
                                        Map<String, String> replacements,
                                        String reason) {
    }

    public record BatchCanvasFilters(Integer status,
                                     String name,
                                     String triggerType,
                                     Integer limit) {
    }

    public record BatchOperationItem(Long canvasId,
                                     Long targetCanvasId,
                                     String status,
                                     String message) {
    }

    public record BatchOperationResult(String operation,
                                       int totalCount,
                                       int successCount,
                                       int skippedCount,
                                       int failedCount,
                                       List<BatchOperationItem> items,
                                       Map<String, Integer> countsByStatus) {

        static BatchOperationResult from(String operation, List<BatchOperationItem> items) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put(SUCCESS, 0);
            counts.put(SKIPPED, 0);
            counts.put(FAILED, 0);
            for (BatchOperationItem item : items) {
                counts.computeIfPresent(item.status(), (key, count) -> count + 1);
            }
            return new BatchOperationResult(
                    operation,
                    items.size(),
                    counts.get(SUCCESS),
                    counts.get(SKIPPED),
                    counts.get(FAILED),
                    items,
                    counts
            );
        }
    }
}
