package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.TestUserDO;
import org.chovy.canvas.dal.dataobject.TestUserSetDO;
import org.chovy.canvas.domain.canvas.TestUserRerunService;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserCreateReq;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserPreview;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserSetCreateReq;
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
@RequestMapping("/test-users")
public class TestUserController {

    private final TestUserRerunService service;
    private final TenantContextResolver tenantContextResolver;

    public TestUserController(TestUserRerunService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/sets")
    public Mono<R<List<TestUserSetDO>>> sets() {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.listSets(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/sets")
    public Mono<R<TestUserSetDO>> createSet(@RequestBody TestUserSetCreateReq req) {
        return current().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.createSet(tenantId(context), req, context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/sets/{setId}/users")
    public Mono<R<List<TestUserDO>>> users(@PathVariable Long setId) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.listUsers(tenantId(context), setId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/sets/{setId}/users")
    public Mono<R<TestUserDO>> createUser(@PathVariable Long setId, @RequestBody TestUserCreateReq req) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.createUser(tenantId(context), setId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    public Mono<R<TestUserDO>> detail(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.getUser(tenantId(context), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}/preview")
    public Mono<R<TestUserPreview>> preview(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.preview(tenantId(context), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> current() {
        return tenantContextResolver.current().defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }
}
