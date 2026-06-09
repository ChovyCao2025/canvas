package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO;
import org.chovy.canvas.domain.cdp.EventAttributeDiscoveryService;
import org.chovy.canvas.dto.cdp.CdpDiscoveredAttributeDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * EventAttributeDiscoveryController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/event-attributes")
@RequiredArgsConstructor
public class EventAttributeDiscoveryController {
    private final TenantContextResolver tenantContextResolver;
    private final EventAttributeDiscoveryService discoveryService;
    /**
     * 查询Event Attribute Discovery列表接口，对应 GET /discovered。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError, discoveryService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/discovered")
    public Mono<R<List<CdpDiscoveredAttributeDTO>>> listDiscovered(
            @RequestParam(required = false) String status) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.currentOrError()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(discoveryService.list(tenantId(ctx), status)
                                .stream()
                                .map(this::toDto)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CdpDiscoveredAttributeDTO toDto(EventAttrDefinitionDO row) {
        return new CdpDiscoveredAttributeDTO(
                row.getId(),
                row.getEventCode(),
                row.getAttrName(),
                row.getAttrType(),
                row.getStatus(),
                row.getSampleValue(),
                row.getFirstSeenAt(),
                row.getLastSeenAt());
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }
}
