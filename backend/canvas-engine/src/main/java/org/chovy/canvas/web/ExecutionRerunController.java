package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.ExecutionRerunAuditDO;
import org.chovy.canvas.domain.canvas.TestUserRerunService;
import org.chovy.canvas.domain.canvas.TestUserRerunService.RerunRequest;
import org.chovy.canvas.domain.canvas.TestUserRerunService.RerunResult;
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
@RequestMapping("/execution-reruns")
public class ExecutionRerunController {

    private final TestUserRerunService service;
    private final TenantContextResolver tenantContextResolver;

    public ExecutionRerunController(TestUserRerunService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/canvas/{canvasId}")
    public Mono<R<RerunResult>> rerun(@PathVariable Long canvasId, @RequestBody RerunRequest req) {
        return current().flatMap(context -> service.rerun(tenantId(context), context, canvasId, req).map(R::ok));
    }

    @GetMapping("/{id}")
    public Mono<R<ExecutionRerunAuditDO>> status(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.audit(tenantId(context), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<ExecutionRerunAuditDO>>> audits(@RequestParam(required = false) Long canvasId) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.audits(tenantId(context), canvasId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> current() {
        return tenantContextResolver.current().defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }
}
