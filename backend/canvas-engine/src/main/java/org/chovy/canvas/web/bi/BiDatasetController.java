package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerResult;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCleanupResultView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceMultiTableCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewCommand;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewResult;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/canvas/bi/datasets/resources")
public class BiDatasetController {

    private final TenantContextResolver tenantContextResolver;
    private final BiDatasetResourceService datasetResourceService;
    private final BiDatasetFromDatasourceService datasourceService;
    private final BiDatasetAccelerationService accelerationService;
    private final BiDatasetAccelerationSchedulerService accelerationSchedulerService;
    private final BiSqlDatasetPreviewService sqlDatasetPreviewService;

    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService) {
        this(tenantContextResolver, datasetResourceService, null, null, null);
    }

    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, null, null);
    }

    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, accelerationService, null);
    }

    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService,
                               BiDatasetAccelerationSchedulerService accelerationSchedulerService) {
        this(tenantContextResolver, datasetResourceService, datasourceService, accelerationService,
                accelerationSchedulerService, null);
    }

    @Autowired
    public BiDatasetController(TenantContextResolver tenantContextResolver,
                               BiDatasetResourceService datasetResourceService,
                               BiDatasetFromDatasourceService datasourceService,
                               BiDatasetAccelerationService accelerationService,
                               BiDatasetAccelerationSchedulerService accelerationSchedulerService,
                               BiSqlDatasetPreviewService sqlDatasetPreviewService) {
        this.tenantContextResolver = tenantContextResolver;
        this.datasetResourceService = datasetResourceService;
        this.datasourceService = datasourceService;
        this.accelerationService = accelerationService;
        this.accelerationSchedulerService = accelerationSchedulerService;
        this.sqlDatasetPreviewService = sqlDatasetPreviewService;
    }

    @GetMapping
    public Mono<R<List<BiDatasetResource>>> list() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listResources(context.tenantId())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> get(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.getResource(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/draft")
    public Mono<R<BiDatasetResource>> saveDraft(@PathVariable String datasetKey,
                                                @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                @RequestBody BiDatasetResource resource) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (!datasetKey.equals(resource.datasetKey())) {
                        throw new IllegalArgumentException("dataset key does not match request path");
                    }
                    return R.ok(datasetResourceService.saveDraft(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            resource,
                            lockToken));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDatasetResource>> saveDraft(String datasetKey, BiDatasetResource resource) {
        return saveDraft(datasetKey, null, resource);
    }

    @PostMapping("/{datasetKey}/publish")
    public Mono<R<BiDatasetResource>> publish(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.publish(context.tenantId(), context.username(), context.role(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{datasetKey}")
    public Mono<R<BiDatasetResource>> archive(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.archive(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}/versions")
    public Mono<R<List<BiDatasetVersionView>>> listVersions(@PathVariable String datasetKey,
                                                            @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.listVersions(context.tenantId(), datasetKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/versions/{version}/restore")
    public Mono<R<BiDatasetResource>> restoreVersion(@PathVariable String datasetKey,
                                                     @RequestHeader(value = "X-BI-LOCK-TOKEN", required = false) String lockToken,
                                                     @PathVariable int version) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(datasetResourceService.restoreVersion(
                                context.tenantId(),
                                context.username(),
                                context.role(),
                                datasetKey,
                                version,
                                lockToken)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<R<BiDatasetResource>> restoreVersion(String datasetKey, int version) {
        return restoreVersion(datasetKey, null, version);
    }

    @GetMapping("/{datasetKey}/acceleration-policy")
    public Mono<R<BiDatasetAccelerationPolicyView>> accelerationPolicy(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.policyView(context.tenantId(), datasetKey));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/acceleration-policy")
    public Mono<R<BiDatasetAccelerationPolicyView>> upsertAccelerationPolicy(
            @PathVariable String datasetKey,
            @RequestBody BiDatasetAccelerationPolicyCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.upsertPolicy(
                            context.tenantId(),
                            datasetKey,
                            command,
                            context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/acceleration-refresh")
    public Mono<R<BiDatasetExtractRefreshRunView>> refreshAcceleration(@PathVariable String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.refreshNow(context.tenantId(), datasetKey, context.username()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/acceleration-scheduler/run")
    public Mono<R<BiDatasetAccelerationSchedulerResult>> runAccelerationScheduler() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationSchedulerService();
                    return R.ok(accelerationSchedulerService.runDueOnce(
                            context.tenantId(),
                            context.username(),
                            LocalDateTime.now()));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/sql-preview")
    public Mono<R<BiSqlDatasetPreviewResult>> previewSqlDataset(@RequestBody BiSqlDatasetPreviewCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireSqlDatasetPreviewService();
                    return R.ok(sqlDatasetPreviewService.preview(context.tenantId(), command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}/acceleration-runs")
    public Mono<R<List<BiDatasetExtractRefreshRunView>>> accelerationRuns(@PathVariable String datasetKey,
                                                                          @RequestParam(defaultValue = "10") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.recentRuns(context.tenantId(), datasetKey, limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{datasetKey}/acceleration-capacity")
    public Mono<R<BiDatasetExtractCapacitySummaryView>> accelerationCapacity(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.capacitySummary(context.tenantId(), datasetKey, limit));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{datasetKey}/acceleration-cleanup")
    public Mono<R<BiDatasetExtractCleanupResultView>> cleanupAcceleration(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "2") int retainTables) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireAccelerationService();
                    return R.ok(accelerationService.cleanupRetainedExtracts(context.tenantId(), datasetKey, retainTables));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/from-datasource-schema")
    public Mono<R<BiDatasetResource>> createFromDatasourceSchema(@RequestBody BiDatasetFromDatasourceCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (datasourceService == null) {
                        throw new IllegalStateException("BI datasource dataset service is required");
                    }
                    return R.ok(datasourceService.createTableDataset(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/from-datasource-schema/multi-table")
    public Mono<R<BiDatasetResource>> createMultiTableFromDatasourceSchema(
            @RequestBody BiDatasetFromDatasourceMultiTableCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    if (datasourceService == null) {
                        throw new IllegalStateException("BI datasource dataset service is required");
                    }
                    return R.ok(datasourceService.createMultiTableDataset(
                            context.tenantId(),
                            context.username(),
                            context.role(),
                            command));
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

    private void requireAccelerationService() {
        if (accelerationService == null) {
            throw new IllegalStateException("BI dataset acceleration service is required");
        }
    }

    private void requireAccelerationSchedulerService() {
        if (accelerationSchedulerService == null) {
            throw new IllegalStateException("BI dataset acceleration scheduler service is required");
        }
    }

    private void requireSqlDatasetPreviewService() {
        if (sqlDatasetPreviewService == null) {
            throw new IllegalStateException("BI SQL dataset preview service is required");
        }
    }
}
