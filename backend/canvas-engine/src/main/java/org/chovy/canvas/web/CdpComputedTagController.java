package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagRunDO;
import org.chovy.canvas.domain.cdp.CdpLineageService;
import org.chovy.canvas.domain.cdp.ComputedTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/cdp/computed-tags")
@RequiredArgsConstructor
/**
 * CdpComputedTagController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class CdpComputedTagController {
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * tag服务，用于承接对应业务能力和领域编排。
     */
    private final ComputedTagService tagService;
    /**
     * lineage服务，用于承接对应业务能力和领域编排。
     */
    private final CdpLineageService lineageService;

    /**
     * ComputedTagRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ComputedTagRequest {

        /**
         * tagCode 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("tagCode")
        private final String tagCode;

        /**
         * displayName 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("displayName")
        private final String displayName;

        /**
         * valueType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("valueType")
        private final String valueType;

        /**
         * computeType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("computeType")
        private final String computeType;

        /**
         * expressionJson 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("expressionJson")
        private final String expressionJson;

        /**
         * refreshMode 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("refreshMode")
        private final String refreshMode;

        /**
         * dependencies 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("dependencies")
        private final List<String> dependencies;

        /**
         * 创建 ComputedTagRequest 实例。
         *
         * @param tagCode tagCode 字段值
         * @param displayName displayName 字段值
         * @param valueType valueType 字段值
         * @param computeType computeType 字段值
         * @param expressionJson expressionJson 字段值
         * @param refreshMode refreshMode 字段值
         * @param dependencies dependencies 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ComputedTagRequest(@com.fasterxml.jackson.annotation.JsonProperty("tagCode") String tagCode, @com.fasterxml.jackson.annotation.JsonProperty("displayName") String displayName, @com.fasterxml.jackson.annotation.JsonProperty("valueType") String valueType, @com.fasterxml.jackson.annotation.JsonProperty("computeType") String computeType, @com.fasterxml.jackson.annotation.JsonProperty("expressionJson") String expressionJson, @com.fasterxml.jackson.annotation.JsonProperty("refreshMode") String refreshMode, @com.fasterxml.jackson.annotation.JsonProperty("dependencies") List<String> dependencies) {
            this.tagCode = tagCode;
            this.displayName = displayName;
            this.valueType = valueType;
            this.computeType = computeType;
            this.expressionJson = expressionJson;
            this.refreshMode = refreshMode;
            this.dependencies = dependencies;
        }

        /**
         * 返回tagCode 字段值。
         *
         * @return tagCode 字段值
         */
        public String tagCode() {
            return tagCode;
        }

        /**
         * 返回displayName 字段值。
         *
         * @return displayName 字段值
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回valueType 字段值。
         *
         * @return valueType 字段值
         */
        public String valueType() {
            return valueType;
        }

        /**
         * 返回computeType 字段值。
         *
         * @return computeType 字段值
         */
        public String computeType() {
            return computeType;
        }

        /**
         * 返回expressionJson 字段值。
         *
         * @return expressionJson 字段值
         */
        public String expressionJson() {
            return expressionJson;
        }

        /**
         * 返回refreshMode 字段值。
         *
         * @return refreshMode 字段值
         */
        public String refreshMode() {
            return refreshMode;
        }

        /**
         * 返回dependencies 字段值。
         *
         * @return dependencies 字段值
         */
        public List<String> dependencies() {
            return dependencies;
        }

        /**
         * 判断两个 ComputedTagRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ComputedTagRequest that)) {
                return false;
            }
            return java.util.Objects.equals(tagCode, that.tagCode) && java.util.Objects.equals(displayName, that.displayName) && java.util.Objects.equals(valueType, that.valueType) && java.util.Objects.equals(computeType, that.computeType) && java.util.Objects.equals(expressionJson, that.expressionJson) && java.util.Objects.equals(refreshMode, that.refreshMode) && java.util.Objects.equals(dependencies, that.dependencies);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tagCode, displayName, valueType, computeType, expressionJson, refreshMode, dependencies);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ComputedTagRequest[" + "tagCode=" + tagCode + ", " + "displayName=" + displayName + ", " + "valueType=" + valueType + ", " + "computeType=" + computeType + ", " + "expressionJson=" + expressionJson + ", " + "refreshMode=" + refreshMode + ", " + "dependencies=" + dependencies + "]";
        }
    }

    /**
     * ImpactCheckRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ImpactCheckRequest {

        /**
         * oldValueType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("oldValueType")
        private final String oldValueType;

        /**
         * newValueType 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("newValueType")
        private final String newValueType;

        /**
         * 创建 ImpactCheckRequest 实例。
         *
         * @param oldValueType oldValueType 字段值
         * @param newValueType newValueType 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ImpactCheckRequest(@com.fasterxml.jackson.annotation.JsonProperty("oldValueType") String oldValueType, @com.fasterxml.jackson.annotation.JsonProperty("newValueType") String newValueType) {
            this.oldValueType = oldValueType;
            this.newValueType = newValueType;
        }

        /**
         * 返回oldValueType 字段值。
         *
         * @return oldValueType 字段值
         */
        public String oldValueType() {
            return oldValueType;
        }

        /**
         * 返回newValueType 字段值。
         *
         * @return newValueType 字段值
         */
        public String newValueType() {
            return newValueType;
        }

        /**
         * 判断两个 ImpactCheckRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ImpactCheckRequest that)) {
                return false;
            }
            return java.util.Objects.equals(oldValueType, that.oldValueType) && java.util.Objects.equals(newValueType, that.newValueType);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(oldValueType, newValueType);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ImpactCheckRequest[" + "oldValueType=" + oldValueType + ", " + "newValueType=" + newValueType + "]";
        }
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<CdpComputedTagDefinitionDO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.list(tenantId(ctx))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<CdpComputedTagDefinitionDO>> create(@RequestBody ComputedTagRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.create(tenantId(ctx), toCommand(request), ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/preview")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 preview 流程生成的业务结果。
     */
    public Mono<R<ComputedTagService.PreviewResult>> preview(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.preview(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/activate")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 activate 流程生成的业务结果。
     */
    public Mono<R<Void>> activate(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.activate(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/pause")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 pause 流程生成的业务结果。
     */
    public Mono<R<Void>> pause(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    tagService.pause(tenantId(ctx), tagCode);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/run")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<ComputedTagService.RunResult>> run(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.runNow(tenantId(ctx), tagCode, ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/runs")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<List<CdpComputedTagRunDO>>> runs(@PathVariable String tagCode,
                                                   @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(tagService.listRuns(tenantId(ctx), tagCode, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{tagCode}/lineage")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 lineage 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<CdpLineageService.LineageImpact>>> lineage(@PathVariable String tagCode) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(lineageService.findTagLineage(tenantId(ctx), tagCode)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tagCode}/impact-check")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 impactCheck 流程生成的业务结果。
     */
    public Mono<R<CdpLineageService.ImpactCheck>> impactCheck(@PathVariable String tagCode,
                                                              @RequestBody ImpactCheckRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(lineageService.checkTypeChange(
                                tenantId(ctx),
                                tagCode,
                                request.oldValueType(),
                                request.newValueType())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private ComputedTagService.DefinitionCommand toCommand(ComputedTagRequest request) {
        return new ComputedTagService.DefinitionCommand(
                request.tagCode(),
                request.displayName(),
                request.valueType(),
                request.computeType(),
                request.expressionJson(),
                request.refreshMode(),
                request.dependencies());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }
}
