package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * CdpWarehouseCatalogController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/warehouse/catalog")
public class CdpWarehouseCatalogController {

    /** 承接数仓数据集目录与血缘配置的治理逻辑。 */
    private final CdpWarehouseCatalogService catalogService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CdpWarehouseCatalogController 实例并注入 web 场景依赖。
     * @param catalogService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseCatalogController(CdpWarehouseCatalogService catalogService) {
        this(catalogService, null);
    }

    /**
     * 创建 CdpWarehouseCatalogController 实例并注入 web 场景依赖。
     * @param catalogService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public CdpWarehouseCatalogController(CdpWarehouseCatalogService catalogService,
                                         TenantContextResolver tenantContextResolver) {
        this.catalogService = catalogService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 查询 CDP 数仓 Catalog列表接口，对应 GET /datasets。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 catalogService.listDatasets 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param layer 请求参数，可选。
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/datasets")
    public Mono<R<List<CdpWarehouseCatalogService.DatasetView>>> listDatasets(
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.listDatasets(tenantId, layer, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 CDP 数仓 Catalog接口，对应 POST /datasets。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 catalogService.upsertDataset 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建或更新 CDP 数仓 Catalog后的业务数据。
     */
    @PostMapping("/datasets")
    public Mono<R<CdpWarehouseCatalogService.DatasetView>> upsertDataset(@RequestBody DatasetReq req) {
        DatasetReq request = req == null ? new DatasetReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.upsertDataset(tenantId, request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 CDP 数仓 Catalog接口，对应 POST /lineage。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 catalogService.createLineageEdge 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建 CDP 数仓 Catalog后的业务数据。
     */
    @PostMapping("/lineage")
    public Mono<R<CdpWarehouseCatalogService.LineageEdgeView>> createLineageEdge(@RequestBody LineageReq req) {
        LineageReq request = req == null ? new LineageReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.createLineageEdge(tenantId, request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Catalog血缘接口，对应 GET /datasets/{datasetKey}/lineage。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 catalogService.lineage 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param direction 请求参数，默认值为 BOTH。
     * @return 异步返回统一响应，包含查询 CDP 数仓 Catalog血缘后的业务数据。
     */
    @GetMapping("/datasets/{datasetKey}/lineage")
    public Mono<R<CdpWarehouseCatalogService.LineageGraph>> lineage(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") CdpWarehouseCatalogService.Direction direction) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.lineage(tenantId, datasetKey, direction)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 CDP 数仓 Catalog血缘接口，对应 GET /datasets/{datasetKey}/lineage/transitive。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 catalogService.transitiveLineage 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param datasetKey 数据集唯一键。
     * @param direction 请求参数，默认值为 BOTH。
     * @param maxDepth 请求参数，可选。
     * @return 异步返回统一响应，包含查询 CDP 数仓 Catalog血缘后的业务数据。
     */
    @GetMapping("/datasets/{datasetKey}/lineage/transitive")
    public Mono<R<CdpWarehouseCatalogService.TransitiveLineageGraph>> transitiveLineage(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") CdpWarehouseCatalogService.Direction direction,
            @RequestParam(required = false) Integer maxDepth) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.transitiveLineage(tenantId, datasetKey, direction, maxDepth)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    @Data
    /**
     * DatasetReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class DatasetReq {
        /** 数据集唯一键，用于关联目录、指标、字段治理和表契约。 */
        private String datasetKey;
        /** 数仓分层标识，用于区分 ODS、DWD、DWS、ADS 等治理边界。 */
        private String layer;
        /** 物理表或字段所在对象名称，用于对接实际存储引擎。 */
        private String physicalName;
        /** 面向运营和治理人员展示的可读名称。 */
        private String displayName;
        /** 主题域，用于按业务域组织数据集目录。 */
        private String subjectArea;
        /** 来源系统，用于追踪数据集的上游业务系统。 */
        private String sourceSystem;
        /** 责任人名称，用于治理归属、告警通知和问题追踪。 */
        private String ownerName;
        /** 补充说明，用于记录配置目的、血缘关系或治理背景。 */
        private String description;
        /** 新鲜度 SLA 分钟数，用于目录展示和可用性告警。 */
        private Integer freshnessSlaMinutes;
        /** 隐私敏感级别，用于决定访问控制和脱敏策略。 */
        private String piiLevel;
        /** 业务状态，用于控制配置启用、检查结果或回执处理分支。 */
        private String status;
        /** Schema 结构 JSON，用于注册实时数据格式或目录数据集字段。 */
        private String schemaJson;

        /**
         * 执行 目标command 对应的内部处理流程。
         * @return 返回内部处理结果
         */
        CdpWarehouseCatalogService.DatasetCommand toCommand() {
            return new CdpWarehouseCatalogService.DatasetCommand(
                    datasetKey,
                    layer,
                    physicalName,
                    displayName,
                    subjectArea,
                    sourceSystem,
                    ownerName,
                    description,
                    freshnessSlaMinutes,
                    piiLevel,
                    status,
                    schemaJson);
        }
    }

    @Data
    /**
     * LineageReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class LineageReq {
        /** 上游数据集唯一键，用于声明血缘输入来源。 */
        private String upstreamDatasetKey;
        /** 下游数据集唯一键，用于声明血缘输出目标。 */
        private String downstreamDatasetKey;
        /** 转换类型，用于描述血缘边上的加工方式。 */
        private String transformType;
        /** 转换引用，用于关联 SQL、任务或代码资产。 */
        private String transformRef;
        /** 依赖类型，用于区分强依赖、弱依赖或派生关系。 */
        private String dependencyType;
        /** 补充说明，用于记录配置目的、血缘关系或治理背景。 */
        private String description;
        /** 是否作为当前有效配置参与目录、血缘或 Schema 治理。 */
        private Boolean active;

        /**
         * 执行 目标command 对应的内部处理流程。
         * @return 返回内部处理结果
         */
        CdpWarehouseCatalogService.LineageCommand toCommand() {
            return new CdpWarehouseCatalogService.LineageCommand(
                    upstreamDatasetKey,
                    downstreamDatasetKey,
                    transformType,
                    transformRef,
                    dependencyType,
                    description,
                    active);
        }
    }
}
