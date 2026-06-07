package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSecretView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitoringWebhookAdminController {

    private final MarketingMonitorWebhookIngestionService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingMonitoringWebhookAdminController(MarketingMonitorWebhookIngestionService service,
                                                    TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/sources/{sourceId}/webhook-secret/rotate")
    public Mono<R<MarketingMonitorWebhookSecretView>> rotateSecret(@PathVariable Long sourceId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.rotateSecret(tenantId(context), sourceId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
