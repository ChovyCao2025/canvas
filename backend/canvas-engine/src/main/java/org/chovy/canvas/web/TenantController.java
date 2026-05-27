package org.chovy.canvas.web;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.TenantDO;
import org.chovy.canvas.domain.tenant.TenantService;
import org.chovy.canvas.dto.tenant.TenantUsageDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping
    public Mono<R<List<TenantDO>>> list() {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(tenantService::list)
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @PostMapping
    public Mono<R<TenantDO>> create(@RequestBody TenantCreateReq req) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(() -> tenantService.create(
                                req.getName(), req.getTenantKey(), req.getPlanCode(),
                                req.getQuotaJson(), context.username()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    @PutMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() ->
                                tenantService.disable(id, context.username()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    @PutMapping("/{id}/activate")
    public Mono<R<Void>> activate(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() ->
                                tenantService.activate(id, context.username()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    @GetMapping("/{id}/usage")
    public Mono<R<TenantUsageDTO>> usage(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(() -> tenantService.usage(id))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    private Mono<TenantContext> requireSuperAdmin() {
        return tenantContextResolver.current()
                .filter(TenantContext::isSuperAdmin)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "需要超级管理员权限")));
    }

    @Data
    public static class TenantCreateReq {
        private String name;
        private String tenantKey;
        private String planCode;
        private String quotaJson;
    }
}
