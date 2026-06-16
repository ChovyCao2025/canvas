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

    /**
     * defaultfilter限制常量，用于保持控制器内部规则一致。
     */
    private static final int DEFAULT_FILTER_LIMIT = 100;
    /**
     * maxbatchsize常量，用于保持控制器内部规则一致。
     */
    private static final int MAX_BATCH_SIZE = 200;
    /**
     * success常量，用于保持控制器内部规则一致。
     */
    private static final String SUCCESS = "SUCCESS";
    /**
     * skipped常量，用于保持控制器内部规则一致。
     */
    private static final String SKIPPED = "SKIPPED";
    /**
     * failed常量，用于保持控制器内部规则一致。
     */
    private static final String FAILED = "FAILED";

    /**
     * 画布服务，用于承接对应业务能力和领域编排。
     */
    private final CanvasService canvasService;
    /**
     * ops服务，用于承接对应业务能力和领域编排。
     */
    private final CanvasOpsService opsService;
    /**
     * 画布数据访问组件，用于访问和持久化对应数据。
     */
    private final CanvasMapper canvasMapper;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
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
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param canvas" canvas"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalArgumentException("canvasIds or filters must resolve at least one canvas");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param MAX_BATCH_SIZE maxbatchsize，由调用方提供
             * @return 返回内部处理结果
             */
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
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalArgumentException("operation is required");
        }
        String normalized = operation.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAUSE", "RESUME", "ARCHIVE", "CLONE").contains(normalized)) {
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param operation operation，由调用方提供
             * @return 返回内部处理结果
             */
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
            /**
             * 执行 accessdeniedexception 对应的内部处理流程。
             *
             * @param role" role"，由调用方提供
             * @return 返回内部处理结果
             */
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
    public static final class BatchOperationRequest {

        /**
         * canvasIds 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("canvasIds")
        private final List<Long> canvasIds;

        /**
         * filters 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("filters")
        private final BatchCanvasFilters filters;

        /**
         * replacements 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("replacements")
        private final Map<String, String> replacements;

        /**
         * 原因。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("reason")
        private final String reason;

        /**
         * 创建 BatchOperationRequest 实例。
         *
         * @param canvasIds canvasIds 字段值
         * @param filters filters 字段值
         * @param replacements replacements 字段值
         * @param reason 原因
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public BatchOperationRequest(@com.fasterxml.jackson.annotation.JsonProperty("canvasIds") List<Long> canvasIds, @com.fasterxml.jackson.annotation.JsonProperty("filters") BatchCanvasFilters filters, @com.fasterxml.jackson.annotation.JsonProperty("replacements") Map<String, String> replacements, @com.fasterxml.jackson.annotation.JsonProperty("reason") String reason) {
            this.canvasIds = canvasIds;
            this.filters = filters;
            this.replacements = replacements;
            this.reason = reason;
        }

        /**
         * 返回canvasIds 字段值。
         *
         * @return canvasIds 字段值
         */
        public List<Long> canvasIds() {
            return canvasIds;
        }

        /**
         * 返回filters 字段值。
         *
         * @return filters 字段值
         */
        public BatchCanvasFilters filters() {
            return filters;
        }

        /**
         * 返回replacements 字段值。
         *
         * @return replacements 字段值
         */
        public Map<String, String> replacements() {
            return replacements;
        }

        /**
         * 返回原因。
         *
         * @return 原因
         */
        public String reason() {
            return reason;
        }

        /**
         * 判断两个 BatchOperationRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BatchOperationRequest that)) {
                return false;
            }
            return java.util.Objects.equals(canvasIds, that.canvasIds) && java.util.Objects.equals(filters, that.filters) && java.util.Objects.equals(replacements, that.replacements) && java.util.Objects.equals(reason, that.reason);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(canvasIds, filters, replacements, reason);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "BatchOperationRequest[" + "canvasIds=" + canvasIds + ", " + "filters=" + filters + ", " + "replacements=" + replacements + ", " + "reason=" + reason + "]";
        }
    }

    /**
     * BatchCanvasFilters 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class BatchCanvasFilters {

        /**
         * 状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("status")
        private final Integer status;

        /**
         * 名称。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private final String name;

        /**
         * triggerType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("triggerType")
        private final String triggerType;

        /**
         * 数量限制。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("limit")
        private final Integer limit;

        /**
         * 创建 BatchCanvasFilters 实例。
         *
         * @param status 状态
         * @param name 名称
         * @param triggerType triggerType 字段值
         * @param limit 数量限制
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public BatchCanvasFilters(@com.fasterxml.jackson.annotation.JsonProperty("status") Integer status, @com.fasterxml.jackson.annotation.JsonProperty("name") String name, @com.fasterxml.jackson.annotation.JsonProperty("triggerType") String triggerType, @com.fasterxml.jackson.annotation.JsonProperty("limit") Integer limit) {
            this.status = status;
            this.name = name;
            this.triggerType = triggerType;
            this.limit = limit;
        }

        /**
         * 返回状态。
         *
         * @return 状态
         */
        public Integer status() {
            return status;
        }

        /**
         * 返回名称。
         *
         * @return 名称
         */
        public String name() {
            return name;
        }

        /**
         * 返回triggerType 字段值。
         *
         * @return triggerType 字段值
         */
        public String triggerType() {
            return triggerType;
        }

        /**
         * 返回数量限制。
         *
         * @return 数量限制
         */
        public Integer limit() {
            return limit;
        }

        /**
         * 判断两个 BatchCanvasFilters 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BatchCanvasFilters that)) {
                return false;
            }
            return java.util.Objects.equals(status, that.status) && java.util.Objects.equals(name, that.name) && java.util.Objects.equals(triggerType, that.triggerType) && java.util.Objects.equals(limit, that.limit);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(status, name, triggerType, limit);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "BatchCanvasFilters[" + "status=" + status + ", " + "name=" + name + ", " + "triggerType=" + triggerType + ", " + "limit=" + limit + "]";
        }
    }

    /**
     * BatchOperationItem 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class BatchOperationItem {

        /**
         * 画布标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("canvasId")
        private final Long canvasId;

        /**
         * targetCanvasId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("targetCanvasId")
        private final Long targetCanvasId;

        /**
         * 状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("status")
        private final String status;

        /**
         * 消息。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("message")
        private final String message;

        /**
         * 创建 BatchOperationItem 实例。
         *
         * @param canvasId 画布标识
         * @param targetCanvasId targetCanvasId 字段值
         * @param status 状态
         * @param message 消息
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public BatchOperationItem(@com.fasterxml.jackson.annotation.JsonProperty("canvasId") Long canvasId, @com.fasterxml.jackson.annotation.JsonProperty("targetCanvasId") Long targetCanvasId, @com.fasterxml.jackson.annotation.JsonProperty("status") String status, @com.fasterxml.jackson.annotation.JsonProperty("message") String message) {
            this.canvasId = canvasId;
            this.targetCanvasId = targetCanvasId;
            this.status = status;
            this.message = message;
        }

        /**
         * 返回画布标识。
         *
         * @return 画布标识
         */
        public Long canvasId() {
            return canvasId;
        }

        /**
         * 返回targetCanvasId 字段值。
         *
         * @return targetCanvasId 字段值
         */
        public Long targetCanvasId() {
            return targetCanvasId;
        }

        /**
         * 返回状态。
         *
         * @return 状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回消息。
         *
         * @return 消息
         */
        public String message() {
            return message;
        }

        /**
         * 判断两个 BatchOperationItem 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BatchOperationItem that)) {
                return false;
            }
            return java.util.Objects.equals(canvasId, that.canvasId) && java.util.Objects.equals(targetCanvasId, that.targetCanvasId) && java.util.Objects.equals(status, that.status) && java.util.Objects.equals(message, that.message);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(canvasId, targetCanvasId, status, message);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "BatchOperationItem[" + "canvasId=" + canvasId + ", " + "targetCanvasId=" + targetCanvasId + ", " + "status=" + status + ", " + "message=" + message + "]";
        }
    }

    /**
     * BatchOperationResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class BatchOperationResult {

        /**
         * operation 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("operation")
        private final String operation;

        /**
         * totalCount 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("totalCount")
        private final int totalCount;

        /**
         * successCount 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("successCount")
        private final int successCount;

        /**
         * skippedCount 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("skippedCount")
        private final int skippedCount;

        /**
         * failedCount 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("failedCount")
        private final int failedCount;

        /**
         * items 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("items")
        private final List<BatchOperationItem> items;

        /**
         * countsByStatus 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("countsByStatus")
        private final Map<String, Integer> countsByStatus;

        /**
         * 创建 BatchOperationResult 实例。
         *
         * @param operation operation 字段值
         * @param totalCount totalCount 字段值
         * @param successCount successCount 字段值
         * @param skippedCount skippedCount 字段值
         * @param failedCount failedCount 字段值
         * @param items items 字段值
         * @param countsByStatus countsByStatus 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public BatchOperationResult(@com.fasterxml.jackson.annotation.JsonProperty("operation") String operation, @com.fasterxml.jackson.annotation.JsonProperty("totalCount") int totalCount, @com.fasterxml.jackson.annotation.JsonProperty("successCount") int successCount, @com.fasterxml.jackson.annotation.JsonProperty("skippedCount") int skippedCount, @com.fasterxml.jackson.annotation.JsonProperty("failedCount") int failedCount, @com.fasterxml.jackson.annotation.JsonProperty("items") List<BatchOperationItem> items, @com.fasterxml.jackson.annotation.JsonProperty("countsByStatus") Map<String, Integer> countsByStatus) {
            this.operation = operation;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.skippedCount = skippedCount;
            this.failedCount = failedCount;
            this.items = items;
            this.countsByStatus = countsByStatus;
        }

        /**
         * 返回operation 字段值。
         *
         * @return operation 字段值
         */
        public String operation() {
            return operation;
        }

        /**
         * 返回totalCount 字段值。
         *
         * @return totalCount 字段值
         */
        public int totalCount() {
            return totalCount;
        }

        /**
         * 返回successCount 字段值。
         *
         * @return successCount 字段值
         */
        public int successCount() {
            return successCount;
        }

        /**
         * 返回skippedCount 字段值。
         *
         * @return skippedCount 字段值
         */
        public int skippedCount() {
            return skippedCount;
        }

        /**
         * 返回failedCount 字段值。
         *
         * @return failedCount 字段值
         */
        public int failedCount() {
            return failedCount;
        }

        /**
         * 返回items 字段值。
         *
         * @return items 字段值
         */
        public List<BatchOperationItem> items() {
            return items;
        }

        /**
         * 返回countsByStatus 字段值。
         *
         * @return countsByStatus 字段值
         */
        public Map<String, Integer> countsByStatus() {
            return countsByStatus;
        }

        /**
         * 判断两个 BatchOperationResult 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BatchOperationResult that)) {
                return false;
            }
            return java.util.Objects.equals(operation, that.operation) && java.util.Objects.equals(totalCount, that.totalCount) && java.util.Objects.equals(successCount, that.successCount) && java.util.Objects.equals(skippedCount, that.skippedCount) && java.util.Objects.equals(failedCount, that.failedCount) && java.util.Objects.equals(items, that.items) && java.util.Objects.equals(countsByStatus, that.countsByStatus);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(operation, totalCount, successCount, skippedCount, failedCount, items, countsByStatus);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "BatchOperationResult[" + "operation=" + operation + ", " + "totalCount=" + totalCount + ", " + "successCount=" + successCount + ", " + "skippedCount=" + skippedCount + ", " + "failedCount=" + failedCount + ", " + "items=" + items + ", " + "countsByStatus=" + countsByStatus + "]";
        }

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
