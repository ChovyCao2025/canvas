package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.platform.MarketingPlatformControlPlaneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/marketing-platform")
public class MarketingPlatformControlPlaneController {

    private final MarketingPlatformControlPlaneService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingPlatformControlPlaneController(MarketingPlatformControlPlaneService service,
                                                   TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/control-plane")
    public Mono<R<MarketingPlatformControlPlaneService.ControlPlaneSummary>> controlPlane() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.summary(tenantId(context))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
