package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService.ModelView;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService.ProviderCreateRequest;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService.ProviderUpdateRequest;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService.ProviderView;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/ai/providers")
public class AiProviderController {

    private final AiProviderModelRegistryService service;
    private final TenantContextResolver tenantContextResolver;

    public AiProviderController(AiProviderModelRegistryService service) {
        this(service, null);
    }

    @Autowired
    public AiProviderController(AiProviderModelRegistryService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<ProviderView>>> list() {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listProviders(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<ProviderView>> create(@RequestBody ProviderCreateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.createProvider(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    public Mono<R<ProviderView>> detail(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.getProvider(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{id}")
    public Mono<R<ProviderView>> update(@PathVariable Long id, @RequestBody ProviderUpdateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.updateProvider(tenantId, id, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.disableProvider(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/models")
    public Mono<R<List<ModelView>>> models(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listModels(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentManagerTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限管理 AI 服务商")))
                .filter(this::canManageAi)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限管理 AI 服务商")))
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    private boolean canManageAi(TenantContext context) {
        return context != null && (context.isSuperAdmin() || context.isTenantAdmin());
    }
}
