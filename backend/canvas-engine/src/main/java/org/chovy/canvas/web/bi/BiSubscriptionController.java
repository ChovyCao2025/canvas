package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleCommand;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentDownload;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentService;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAuditSummary;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryLogView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRetryResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRunResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeService;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionAdminService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionCommand;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/canvas/bi")
public class BiSubscriptionController {

    private final TenantContextResolver tenantContextResolver;
    private final BiSubscriptionAdminService subscriptionAdminService;
    private final BiDeliveryRuntimeService deliveryRuntimeService;
    private final BiDeliverySchedulerService deliverySchedulerService;
    private final BiDeliveryAttachmentService deliveryAttachmentService;

    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService) {
        this(tenantContextResolver, subscriptionAdminService, deliveryRuntimeService,
                (BiDeliverySchedulerService) null, (BiDeliveryAttachmentService) null);
    }

    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService,
                                    ObjectProvider<BiDeliverySchedulerService> deliverySchedulerServiceProvider) {
        this(tenantContextResolver,
                subscriptionAdminService,
                deliveryRuntimeService,
                deliverySchedulerServiceProvider == null ? null : deliverySchedulerServiceProvider.getIfAvailable(),
                null);
    }

    @Autowired
    public BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                    BiSubscriptionAdminService subscriptionAdminService,
                                    BiDeliveryRuntimeService deliveryRuntimeService,
                                    ObjectProvider<BiDeliverySchedulerService> deliverySchedulerServiceProvider,
                                    ObjectProvider<BiDeliveryAttachmentService> deliveryAttachmentServiceProvider) {
        this(tenantContextResolver,
                subscriptionAdminService,
                deliveryRuntimeService,
                deliverySchedulerServiceProvider.getIfAvailable(),
                deliveryAttachmentServiceProvider.getIfAvailable());
    }

    private BiSubscriptionController(TenantContextResolver tenantContextResolver,
                                     BiSubscriptionAdminService subscriptionAdminService,
                                     BiDeliveryRuntimeService deliveryRuntimeService,
                                     BiDeliverySchedulerService deliverySchedulerService,
                                     BiDeliveryAttachmentService deliveryAttachmentService) {
        this.tenantContextResolver = tenantContextResolver;
        this.subscriptionAdminService = subscriptionAdminService;
        this.deliveryRuntimeService = deliveryRuntimeService;
        this.deliverySchedulerService = deliverySchedulerService;
        this.deliveryAttachmentService = deliveryAttachmentService;
    }

    @GetMapping("/subscriptions")
    public Mono<R<List<BiSubscriptionView>>> listSubscriptions(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.listSubscriptions(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/subscriptions")
    public Mono<R<BiSubscriptionView>> upsertSubscription(@RequestBody BiSubscriptionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.upsertSubscription(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/subscriptions/{id}")
    public Mono<R<Void>> deleteSubscription(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    subscriptionAdminService.deleteSubscription(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/subscriptions/{id}/run")
    public Mono<R<BiDeliveryRunResult>> runSubscription(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.runSubscription(
                                context.tenantId(),
                                id,
                                context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/alerts")
    public Mono<R<List<BiAlertRuleView>>> listAlerts(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.listAlerts(context.tenantId(), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alerts")
    public Mono<R<BiAlertRuleView>> upsertAlert(@RequestBody BiAlertRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(subscriptionAdminService.upsertAlert(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/alerts/{id}")
    public Mono<R<Void>> deleteAlert(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    subscriptionAdminService.deleteAlert(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/alerts/{id}/run")
    public Mono<R<BiDeliveryRunResult>> runAlert(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.runAlert(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/delivery-logs")
    public Mono<R<List<BiDeliveryLogView>>> listDeliveryLogs(@RequestParam(required = false) String jobType,
                                                             @RequestParam(required = false) Long jobId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.listLogs(context.tenantId(), jobType, jobId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/delivery-audit")
    public Mono<R<BiDeliveryAuditSummary>> auditDeliveryLogs(@RequestParam(required = false) String jobType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String channel,
                                                             @RequestParam(required = false) Long jobId,
                                                             @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.auditDeliveries(
                                context.tenantId(),
                                jobType,
                                status,
                                channel,
                                jobId,
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/delivery-logs/retry")
    public Mono<R<BiDeliveryRetryResult>> retryDeliveryLogs(@RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(deliveryRuntimeService.retryPendingDeliveries(
                                context.tenantId(),
                                context.username(),
                                limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/delivery-attachments")
    public Mono<R<List<BiDeliveryAttachmentView>>> listDeliveryAttachments(@RequestParam(required = false) String jobType,
                                                                           @RequestParam(required = false) Long jobId,
                                                                           @RequestParam(required = false) Long deliveryLogId,
                                                                           @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliveryAttachmentService == null) {
                        return R.ok(List.<BiDeliveryAttachmentView>of());
                    }
                    return R.ok(deliveryAttachmentService.listAttachments(
                            context.tenantId(),
                            jobType,
                            jobId,
                            deliveryLogId,
                            limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/delivery-attachments/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadDeliveryAttachment(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliveryAttachmentService == null) {
                        throw new IllegalStateException("BI delivery attachment service is not configured");
                    }
                    BiDeliveryAttachmentDownload file = deliveryAttachmentService.download(context.tenantId(), id);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(file.filename())
                                            .build()
                                            .toString())
                            .body(file.bytes());
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/delivery-attachments/cleanup")
    public Mono<R<BiDeliveryAttachmentCleanupResult>> cleanupDeliveryAttachments(@RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliveryAttachmentService == null) {
                        return R.ok(new BiDeliveryAttachmentCleanupResult(0, 0, 0, 0));
                    }
                    return R.ok(deliveryAttachmentService.cleanupExpiredAttachments(context.tenantId(), limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/delivery-scheduler/run")
    public Mono<R<BiDeliverySchedulerResult>> runDeliveryScheduler() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (deliverySchedulerService == null) {
                        return R.ok(new BiDeliverySchedulerResult(0, 0, 0, 0, 0, 0));
                    }
                    return R.ok(deliverySchedulerService.runDueOnce(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            null));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
