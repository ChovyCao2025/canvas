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

@RestController
@RequestMapping("/cdp/write-keys")
@RequiredArgsConstructor
public class CdpWriteKeyController {
    private final TenantContextResolver tenantContextResolver;
    private final CdpWriteKeyAuthService writeKeyService;

    @GetMapping
    public Mono<R<List<CdpWriteKeyRowDTO>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(writeKeyService.listTenantKeys(tenantId(ctx))
                                .stream()
                                .map(this::toRow)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

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

    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    writeKeyService.disable(tenantId(ctx), id);
                    return R.ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

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

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }
}
