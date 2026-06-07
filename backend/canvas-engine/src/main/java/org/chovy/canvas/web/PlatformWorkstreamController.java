package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.platform.PlatformWorkstreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/platform/workstreams")
public class PlatformWorkstreamController {

    private final PlatformWorkstreamService service;
    private final TenantContextResolver tenantContextResolver;

    public PlatformWorkstreamController(PlatformWorkstreamService service,
                                        TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<PlatformWorkstreamService.WorkstreamStatus>>> list() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.statuses()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
