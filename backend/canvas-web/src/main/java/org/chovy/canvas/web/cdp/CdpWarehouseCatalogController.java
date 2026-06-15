package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.DatasetCommand;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.Direction;
import org.chovy.canvas.cdp.api.CdpWarehouseCatalogFacade.LineageCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

@RestController
@RequestMapping("/warehouse/catalog")
public class CdpWarehouseCatalogController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWarehouseCatalogFacade facade;

    public CdpWarehouseCatalogController(CdpWarehouseCatalogFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/datasets")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listDatasets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.listDatasets(tenantIdOrDefault(tenantId), layer, status));
    }

    @PostMapping("/datasets")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertDataset(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) DatasetReq request) {
        DatasetReq body = request == null ? new DatasetReq() : request;
        return envelope(() -> facade.upsertDataset(tenantIdOrDefault(tenantId), body.toCommand()));
    }

    @PostMapping("/lineage")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createLineageEdge(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) LineageReq request) {
        LineageReq body = request == null ? new LineageReq() : request;
        return envelope(() -> facade.createLineageEdge(tenantIdOrDefault(tenantId), body.toCommand()));
    }

    @GetMapping("/datasets/{datasetKey}/lineage")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> lineage(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") Direction direction) {
        return envelope(() -> facade.lineage(tenantIdOrDefault(tenantId), datasetKey, direction));
    }

    @GetMapping("/datasets/{datasetKey}/lineage/transitive")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> transitiveLineage(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String datasetKey,
            @RequestParam(defaultValue = "BOTH") Direction direction,
            @RequestParam(required = false) Integer maxDepth) {
        return envelope(() -> facade.transitiveLineage(tenantIdOrDefault(tenantId), datasetKey, direction, maxDepth));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    public static class DatasetReq {
        public String datasetKey;
        public String layer;
        public String physicalName;
        public String displayName;
        public String subjectArea;
        public String sourceSystem;
        public String ownerName;
        public String description;
        public Integer freshnessSlaMinutes;
        public String piiLevel;
        public String status;
        public String schemaJson;

        DatasetCommand toCommand() {
            return new DatasetCommand(datasetKey, layer, physicalName, displayName, subjectArea, sourceSystem,
                    ownerName, description, freshnessSlaMinutes, piiLevel, status, schemaJson);
        }
    }

    public static class LineageReq {
        public String upstreamDatasetKey;
        public String downstreamDatasetKey;
        public String transformType;
        public String transformRef;
        public String dependencyType;
        public String description;
        public Boolean active;

        LineageCommand toCommand() {
            return new LineageCommand(upstreamDatasetKey, downstreamDatasetKey, transformType, transformRef,
                    dependencyType, description, active);
        }
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
