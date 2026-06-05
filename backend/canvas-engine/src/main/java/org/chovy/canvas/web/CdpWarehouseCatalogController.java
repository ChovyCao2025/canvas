package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogService;
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
@RequestMapping("/warehouse/catalog")
public class CdpWarehouseCatalogController {

    private final CdpWarehouseCatalogService catalogService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseCatalogController(CdpWarehouseCatalogService catalogService) {
        this(catalogService, null);
    }

    @Autowired
    public CdpWarehouseCatalogController(CdpWarehouseCatalogService catalogService,
                                         TenantContextResolver tenantContextResolver) {
        this.catalogService = catalogService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/datasets")
    public Mono<R<List<CdpWarehouseCatalogService.DatasetView>>> listDatasets(
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.listDatasets(tenantId, layer, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/datasets")
    public Mono<R<CdpWarehouseCatalogService.DatasetView>> upsertDataset(@RequestBody DatasetReq req) {
        DatasetReq request = req == null ? new DatasetReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.upsertDataset(tenantId, request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/lineage")
    public Mono<R<CdpWarehouseCatalogService.LineageEdgeView>> createLineageEdge(@RequestBody LineageReq req) {
        LineageReq request = req == null ? new LineageReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.createLineageEdge(tenantId, request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/datasets/{datasetKey}/lineage")
    public Mono<R<CdpWarehouseCatalogService.LineageGraph>> lineage(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") CdpWarehouseCatalogService.Direction direction) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.lineage(tenantId, datasetKey, direction)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/datasets/{datasetKey}/lineage/transitive")
    public Mono<R<CdpWarehouseCatalogService.TransitiveLineageGraph>> transitiveLineage(
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") CdpWarehouseCatalogService.Direction direction,
            @RequestParam(required = false) Integer maxDepth) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(catalogService.transitiveLineage(tenantId, datasetKey, direction, maxDepth)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    @Data
    public static class DatasetReq {
        private String datasetKey;
        private String layer;
        private String physicalName;
        private String displayName;
        private String subjectArea;
        private String sourceSystem;
        private String ownerName;
        private String description;
        private Integer freshnessSlaMinutes;
        private String piiLevel;
        private String status;
        private String schemaJson;

        CdpWarehouseCatalogService.DatasetCommand toCommand() {
            return new CdpWarehouseCatalogService.DatasetCommand(
                    datasetKey,
                    layer,
                    physicalName,
                    displayName,
                    subjectArea,
                    sourceSystem,
                    ownerName,
                    description,
                    freshnessSlaMinutes,
                    piiLevel,
                    status,
                    schemaJson);
        }
    }

    @Data
    public static class LineageReq {
        private String upstreamDatasetKey;
        private String downstreamDatasetKey;
        private String transformType;
        private String transformRef;
        private String dependencyType;
        private String description;
        private Boolean active;

        CdpWarehouseCatalogService.LineageCommand toCommand() {
            return new CdpWarehouseCatalogService.LineageCommand(
                    upstreamDatasetKey,
                    downstreamDatasetKey,
                    transformType,
                    transformRef,
                    dependencyType,
                    description,
                    active);
        }
    }
}
