package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDeliveryView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDispatchView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorIngestResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemIngestCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollRunView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollingService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRevokeCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthCallbackCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotView;
import org.chovy.canvas.domain.monitoring.MarketingMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitoringController {

    private final MarketingMonitoringService service;
    private final MarketingMonitorAlertFanoutService alertFanoutService;
    private final MarketingMonitorPollingService pollingService;
    private final MarketingMonitorInferenceService inferenceService;
    private final MarketingMonitorProviderCredentialService providerCredentialService;
    private final MarketingMonitorProviderOAuthAuthorizationService providerOAuthAuthorizationService;
    private final TenantContextResolver tenantContextResolver;

    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, null, null, null, null, tenantContextResolver);
    }

    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, null, null, null, tenantContextResolver);
    }

    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, inferenceService, null, null, tenantContextResolver);
    }

    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         MarketingMonitorProviderCredentialService providerCredentialService,
                                         TenantContextResolver tenantContextResolver) {
        this(service, alertFanoutService, pollingService, inferenceService, providerCredentialService, null,
                tenantContextResolver);
    }

    @Autowired
    public MarketingMonitoringController(MarketingMonitoringService service,
                                         MarketingMonitorAlertFanoutService alertFanoutService,
                                         MarketingMonitorPollingService pollingService,
                                         MarketingMonitorInferenceService inferenceService,
                                         MarketingMonitorProviderCredentialService providerCredentialService,
                                         MarketingMonitorProviderOAuthAuthorizationService providerOAuthAuthorizationService,
                                         TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.alertFanoutService = alertFanoutService;
        this.pollingService = pollingService;
        this.inferenceService = inferenceService;
        this.providerCredentialService = providerCredentialService;
        this.providerOAuthAuthorizationService = providerOAuthAuthorizationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/sources")
    public Mono<R<MarketingMonitorSourceView>> upsertSource(
            @RequestBody MarketingMonitorSourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/items")
    public Mono<R<MarketingMonitorIngestResult>> ingestItem(
            @RequestBody MarketingMonitorItemIngestCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.ingestItem(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/items")
    public Mono<R<List<MarketingMonitorItemView>>> items(
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.items(tenantId(context),
                                new MarketingMonitorItemQuery(sentimentLabel, competitorKey, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/alerts")
    public Mono<R<List<MarketingMonitorAlertView>>> alerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.alerts(tenantId(context),
                                new MarketingMonitorAlertQuery(status, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public Mono<R<MarketingMonitorAlertView>> resolveAlert(@PathVariable Long alertId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.resolveAlert(tenantId(context), alertId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alert-channels")
    public Mono<R<MarketingMonitorAlertChannelView>> upsertAlertChannel(
            @RequestBody MarketingMonitorAlertChannelCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.upsertChannel(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alerts/{alertId}/dispatch")
    public Mono<R<MarketingMonitorAlertDispatchView>> dispatchAlert(@PathVariable Long alertId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.dispatchAlert(tenantId(context), alertId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/alert-deliveries")
    public Mono<R<List<MarketingMonitorAlertDeliveryView>>> alertDeliveries(
            @RequestParam(required = false) Long alertId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(alertFanoutService.deliveries(tenantId(context), alertId, status,
                                boundedLimit(limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/sources/{sourceId}/polling")
    public Mono<R<MarketingMonitorSourcePollingView>> configureSourcePolling(
            @PathVariable Long sourceId,
            @RequestBody MarketingMonitorSourcePollingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .configurePolling(tenantId(context), sourceId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/sources/{sourceId}/poll")
    public Mono<R<MarketingMonitorPollRunView>> pollSource(
            @PathVariable Long sourceId,
            @RequestBody(required = false) MarketingMonitorPollCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .pollSource(tenantId(context), sourceId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/trends/snapshots/build")
    public Mono<R<MarketingMonitorTrendSnapshotView>> buildTrendSnapshot(
            @RequestBody MarketingMonitorTrendSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService()
                                .buildTrendSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/trends/snapshots")
    public Mono<R<List<MarketingMonitorTrendSnapshotView>>> trendSnapshots(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String brandKey,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredPollingService().trendSnapshots(tenantId(context),
                                new MarketingMonitorTrendSnapshotQuery(
                                        sourceId,
                                        brandKey,
                                        competitorKey,
                                boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/items/{itemId}/inferences")
    public Mono<R<MarketingMonitorInferenceView>> analyzeInference(
            @PathVariable Long itemId,
            @RequestBody(required = false) MarketingMonitorInferenceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredInferenceService()
                                .analyze(tenantId(context), inferenceCommand(itemId, command), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/inferences")
    public Mono<R<List<MarketingMonitorInferenceView>>> inferences(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String modelKey,
            @RequestParam(required = false) String providerStatus,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredInferenceService().list(tenantId(context),
                                new MarketingMonitorInferenceQuery(
                                        itemId,
                                        sentimentLabel,
                                        modelKey,
                                        providerStatus,
                                        fallbackUsed,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials")
    public Mono<R<MarketingMonitorProviderCredentialView>> upsertProviderCredential(
            @RequestBody MarketingMonitorProviderCredentialCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .upsert(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/provider-credentials")
    public Mono<R<List<MarketingMonitorProviderCredentialView>>> providerCredentials(
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String authType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService().list(tenantId(context),
                                new MarketingMonitorProviderCredentialQuery(
                                        providerType,
                                        authType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/{credentialKey}/refresh")
    public Mono<R<MarketingMonitorProviderCredentialView>> refreshProviderCredential(
            @PathVariable String credentialKey,
            @RequestBody(required = false) MarketingMonitorProviderCredentialRefreshCommand command) {
        MarketingMonitorProviderCredentialRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRefreshCommand(null)
                : command;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .refresh(tenantId(context), credentialKey, effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/refresh-due")
    public Mono<R<MarketingMonitorProviderCredentialDueRefreshResult>> refreshDueProviderCredentials(
            @RequestBody(required = false) MarketingMonitorProviderCredentialDueRefreshCommand command) {
        MarketingMonitorProviderCredentialDueRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialDueRefreshCommand(null, null)
                : new MarketingMonitorProviderCredentialDueRefreshCommand(
                        command.windowMinutes(),
                        boundedLimit(command.limit() == null ? 50 : command.limit()));
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .refreshDue(tenantId(context), effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/{credentialKey}/revoke")
    public Mono<R<MarketingMonitorProviderCredentialView>> revokeProviderCredential(
            @PathVariable String credentialKey,
            @RequestBody(required = false) MarketingMonitorProviderCredentialRevokeCommand command) {
        MarketingMonitorProviderCredentialRevokeCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRevokeCommand(null, null, null, null, null)
                : command;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .revoke(tenantId(context), credentialKey, effectiveCommand, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/{credentialKey}/disable")
    public Mono<R<MarketingMonitorProviderCredentialView>> disableProviderCredential(
            @PathVariable String credentialKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService()
                                .disable(tenantId(context), credentialKey, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/provider-credentials/events")
    public Mono<R<List<MarketingMonitorProviderCredentialEventView>>> providerCredentialEvents(
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderCredentialService().events(tenantId(context),
                                new MarketingMonitorProviderCredentialEventQuery(
                                        credentialKey,
                                        eventType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/oauth/authorizations")
    public Mono<R<MarketingMonitorProviderOAuthAuthorizationView>> startProviderOAuthAuthorization(
            @RequestBody MarketingMonitorProviderOAuthAuthorizationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService()
                                .startAuthorization(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/provider-credentials/oauth/callback")
    public Mono<R<MarketingMonitorProviderOAuthAuthorizationView>> completeProviderOAuthAuthorization(
            @RequestBody MarketingMonitorProviderOAuthCallbackCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService()
                                .completeAuthorization(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/provider-credentials/oauth/authorizations")
    public Mono<R<List<MarketingMonitorProviderOAuthAuthorizationView>>> providerOAuthAuthorizations(
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService().list(tenantId(context),
                                new MarketingMonitorProviderOAuthAuthorizationQuery(
                                        credentialKey,
                                        providerType,
                                        status,
                                        boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/provider-credentials/oauth/events")
    public Mono<R<List<MarketingMonitorProviderOAuthAuthorizationEventView>>> providerOAuthAuthorizationEvents(
            @RequestParam(required = false) String authState,
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requiredProviderOAuthAuthorizationService().events(tenantId(context),
                                new MarketingMonitorProviderOAuthAuthorizationEventQuery(
                                        authState,
                                        credentialKey,
                                        eventType,
                                        status,
                                        boundedLimit(limit)))))
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

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private MarketingMonitorPollingService requiredPollingService() {
        if (pollingService == null) {
            throw new IllegalStateException("marketing monitoring polling service is not configured");
        }
        return pollingService;
    }

    private MarketingMonitorInferenceService requiredInferenceService() {
        if (inferenceService == null) {
            throw new IllegalStateException("marketing monitoring inference service is not configured");
        }
        return inferenceService;
    }

    private MarketingMonitorProviderCredentialService requiredProviderCredentialService() {
        if (providerCredentialService == null) {
            throw new IllegalStateException("marketing monitoring provider credential service is not configured");
        }
        return providerCredentialService;
    }

    private MarketingMonitorProviderOAuthAuthorizationService requiredProviderOAuthAuthorizationService() {
        if (providerOAuthAuthorizationService == null) {
            throw new IllegalStateException("marketing monitoring provider OAuth authorization service is not configured");
        }
        return providerOAuthAuthorizationService;
    }

    private MarketingMonitorInferenceCommand inferenceCommand(Long itemId,
                                                              MarketingMonitorInferenceCommand command) {
        return new MarketingMonitorInferenceCommand(
                itemId,
                command == null ? null : command.providerId(),
                command == null ? null : command.templateId(),
                command == null ? null : command.modelKey(),
                command == null ? null : command.modelVersion(),
                command == null ? null : command.forceFallback(),
                command == null ? null : command.params(),
                command == null ? null : command.timeoutMs(),
                command == null ? null : command.metadata());
    }
}
