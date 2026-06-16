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
/**
 * AiProviderController 提供对应业务域的 HTTP 接口入口。
 */
public class AiProviderController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final AiProviderModelRegistryService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 使用模型注册服务创建控制器，兼容未接入租户解析器的旧测试场景。
     *
     * @param service AI 提供方模型注册服务
     */
    public AiProviderController(AiProviderModelRegistryService service) {
        this(service, null);
    }

    @Autowired
    /**
     * 使用模型注册服务和租户解析器创建控制器。
     *
     * @param service AI 提供方模型注册服务
     * @param tenantContextResolver 租户上下文解析器
     */
    public AiProviderController(AiProviderModelRegistryService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<ProviderView>>> list() {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listProviders(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    /**
     * 创建对应资源并返回处理结果。
     *
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<ProviderView>> create(@RequestBody ProviderCreateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.createProvider(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    /**
     * 查询并返回对应资源或视图。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<ProviderView>> detail(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.getProvider(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{id}")
    /**
     * 更新对应资源并返回处理结果。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<ProviderView>> update(@PathVariable Long id, @RequestBody ProviderUpdateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.updateProvider(tenantId, id, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/disable")
    /**
     * 变更或删除对应资源并返回处理结果。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.disableProvider(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/models")
    /**
     * 查询并返回对应资源或视图。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<ModelView>>> models(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listModels(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 解析并校验当前请求上下文。
     * @return 返回处理后的业务结果
     */
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

    /**
     * 判断当前上下文是否满足业务条件。
     *
     * @param context 上下文，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    private boolean canManageAi(TenantContext context) {
        return context != null && (context.isSuperAdmin() || context.isTenantAdmin());
    }
}
