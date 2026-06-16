package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CdpWriteKeyDO;
import org.chovy.canvas.domain.cdp.CdpWriteKeyAuthService;
import org.chovy.canvas.dto.cdp.CdpWriteKeyCreateReq;
import org.chovy.canvas.dto.cdp.CdpWriteKeyCreateResp;
import org.chovy.canvas.dto.cdp.CdpWriteKeyRowDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * CdpWriteKeyController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/cdp/write-keys")
@RequiredArgsConstructor
public class CdpWriteKeyController {
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * write键服务，用于承接对应业务能力和领域编排。
     */
    private final CdpWriteKeyAuthService writeKeyService;
    /**
     * 查询 CDP 写入密钥列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError, writeKeyService.listTenantKeys 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CdpWriteKeyRowDTO>>> list() {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.currentOrError()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(writeKeyService.listTenantKeys(tenantId(ctx))
                                .stream()
                                .map(this::toRow)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 CDP 写入密钥接口，对应 POST 请求。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError, writeKeyService.generateRawKey, writeKeyService.create 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建 CDP 写入密钥后的业务数据。
     */
    @PostMapping
    public Mono<R<CdpWriteKeyCreateResp>> create(@RequestBody CdpWriteKeyCreateReq req) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    String raw = writeKeyService.generateRawKey();
                    CdpWriteKeyDO row = writeKeyService.create(tenantId(ctx), req, ctx.username(), raw);
                    return R.ok(new CdpWriteKeyCreateResp(
                            row.getId(),
                            row.getName(),
                            raw,
                            row.getKeyPrefix(),
                            row.getPlatform(),
                            row.getRateLimitQps(),
                            row.getDailyQuota()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 停用 CDP 写入密钥接口，对应 DELETE /{id}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError, writeKeyService.disable 完成业务处理。
     * 副作用：会停用资源。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    writeKeyService.disable(tenantId(ctx), id);
                    return R.ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CdpWriteKeyRowDTO toRow(CdpWriteKeyDO row) {
        return new CdpWriteKeyRowDTO(
                row.getId(),
                row.getName(),
                row.getKeyPrefix(),
                row.getPlatform(),
                row.getStatus(),
                row.getRateLimitQps(),
                row.getDailyQuota(),
                row.getDescription(),
                row.getCreatedAt(),
                row.getUpdatedAt());
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
