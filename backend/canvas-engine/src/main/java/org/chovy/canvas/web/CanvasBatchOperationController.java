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
/**
 * CanvasBatchOperationController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
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

    /**
     * 初始化 CanvasBatchOperationController 实例。
     *
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     * @param opsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
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
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvas canvas 参数，用于 pause 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 pause 流程生成的业务结果。
     */
    private BatchOperationItem pause(CanvasDO canvas, TenantContext context) {
        if (!Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "only published canvases can be paused");
        }
        canvasService.offline(canvas.getId(), operator(context));
        return new BatchOperationItem(canvas.getId(), null, SUCCESS, "paused");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvas canvas 参数，用于 resume 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 resume 流程生成的业务结果。
     */
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

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param canvas canvas 参数，用于 archive 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 archive 流程生成的业务结果。
     */
    private BatchOperationItem archive(CanvasDO canvas, TenantContext context) {
        if (Objects.equals(canvas.getStatus(), CanvasStatusEnum.ARCHIVED.getCode())) {
            return new BatchOperationItem(canvas.getId(), null, SKIPPED, "canvas already archived");
        }
        canvasService.archive(canvas.getId(), operator(context));
        return new BatchOperationItem(canvas.getId(), null, SUCCESS, "archived");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvas canvas 参数，用于 clone 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 clone 流程生成的业务结果。
     */
    private BatchOperationItem clone(CanvasDO canvas, BatchOperationRequest request, TenantContext context) {
        CanvasDO cloned = opsService.clone(canvas.getId(), operator(context));
        applyCloneReplacements(cloned, request == null ? null : request.replacements());
        return new BatchOperationItem(canvas.getId(), cloned.getId(), SUCCESS, "cloned");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param cloned cloned 参数，用于 applyCloneReplacements 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 applyCloneReplacements 流程中的校验、计算或对象转换。
     * @param replacements replacements 参数，用于 applyCloneReplacements 流程中的校验、计算或对象转换。
     */
    private void applyCloneReplacements(CanvasDO cloned, Map<String, String> replacements) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (cloned == null || replacements == null || replacements.isEmpty()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            canvasMapper.updateById(cloned);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @param MapString map string 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @param replacements replacements 参数，用于 replaceTokens 流程中的校验、计算或对象转换。
     * @return 返回 replace tokens 生成的文本或业务键。
     */
    private String replaceTokens(String input, Map<String, String> replacements) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (input == null || replacements == null || replacements.isEmpty()) {
            return input;
        }
        String result = input;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (!hasText(entry.getKey())) {
                continue;
            }
            result = result.replace("${" + entry.getKey().trim() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 resolve canvas ids 汇总后的集合、分页或映射视图。
     */
    private List<Long> resolveCanvasIds(BatchOperationRequest request, TenantContext context) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request != null && request.canvasIds() != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_FILTER_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_BATCH_SIZE));
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireAdmin(TenantContext context) {
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new AccessDeniedException("batch canvas operations require admin role");
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 current 流程生成的业务结果。
     */
    private Mono<TenantContext> current() {
        return tenantContextResolver.current().defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 operator 生成的文本或业务键。
     */
    private String operator(TenantContext context) {
        return hasText(context.username()) ? context.username().trim() : "system";
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * BatchOperationRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record BatchOperationRequest(List<Long> canvasIds,
                                        BatchCanvasFilters filters,
                                        Map<String, String> replacements,
                                        String reason) {
    }

    /**
     * BatchCanvasFilters 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record BatchCanvasFilters(Integer status,
                                     String name,
                                     String triggerType,
                                     Integer limit) {
    }

    /**
     * BatchOperationItem 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record BatchOperationItem(Long canvasId,
                                     Long targetCanvasId,
                                     String status,
                                     String message) {
    }

    /**
     * BatchOperationResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record BatchOperationResult(String operation,
                                       int totalCount,
                                       int successCount,
                                       int skippedCount,
                                       int failedCount,
                                       List<BatchOperationItem> items,
                                       Map<String, Integer> countsByStatus) {

        /**
         * 组装输出结构或完成对象转换。
         *
         * @param operation 待调度任务或操作名称，用于封装阻塞工作。
         * @param items items 参数，用于 from 流程中的校验、计算或对象转换。
         * @return 返回组装或转换后的结果对象。
         */
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
