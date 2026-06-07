package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/public/marketing-monitoring/webhooks")
public class PublicMarketingMonitoringWebhookController {

    private final MarketingMonitorWebhookIngestionService service;

    public PublicMarketingMonitoringWebhookController(MarketingMonitorWebhookIngestionService service) {
        this.service = service;
    }

    @PostMapping("/{tenantId}/{sourceKey}")
    public Mono<R<MarketingMonitorWebhookIngestView>> ingestWebhook(
            @PathVariable Long tenantId,
            @PathVariable String sourceKey,
            @RequestHeader("X-Canvas-Monitoring-Timestamp") String timestamp,
            @RequestHeader("X-Canvas-Monitoring-Signature") String signature,
            @RequestBody String rawBody) {
        return Mono.fromCallable(() -> R.ok(service.ingestWebhook(
                        tenantId,
                        sourceKey,
                        timestamp,
                        signature,
                        rawBody)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
