package org.chovy.canvas.web.bi;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chovy.canvas.bi.api.BiCatalogFacade;
import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiPermissionGrantView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.api.BiWorkspaceView;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi")
public class BiCatalogController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "analyst";

    private final BiCatalogFacade facade;

    public BiCatalogController(BiCatalogFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/workspaces")
    public Mono<CompatibilityEnvelope<BiWorkspaceView>> upsertWorkspace(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody WorkspaceRequest request) {
        return envelope(() -> facade.upsertWorkspace(
                tenantIdOrDefault(tenantId),
                new BiWorkspaceCommand(
                        request.workspaceKey(),
                        request.name(),
                        request.description(),
                        request.status()),
                actorOrDefault(actor)));
    }

    @PostMapping("/datasets/resources/{datasetKey}/draft")
    public Mono<CompatibilityEnvelope<BiDatasetView>> saveDatasetDraft(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DatasetDraftRequest request) {
        return envelope(() -> facade.upsertDataset(
                tenantIdOrDefault(tenantId),
                request.toCommand(datasetKey),
                actorOrDefault(actor)));
    }

    @GetMapping("/datasets/resources")
    public Mono<CompatibilityEnvelope<List<BiDatasetView>>> listDatasetResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDatasetResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasets/resources/{datasetKey}")
    public Mono<CompatibilityEnvelope<BiDatasetView>> getDatasetResource(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDatasetResource(tenantIdOrDefault(tenantId), datasetKey));
    }

    @GetMapping("/datasets")
    public Mono<CompatibilityEnvelope<List<BiQueryDatasetView>>> listQueryDatasets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listQueryDatasets(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/datasets/{datasetKey}")
    public Mono<CompatibilityEnvelope<BiQueryDatasetView>> getQueryDataset(
            @PathVariable String datasetKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getQueryDataset(tenantIdOrDefault(tenantId), datasetKey));
    }

    @GetMapping("/dashboards/presets")
    public Mono<CompatibilityEnvelope<List<BiDashboardPresetView>>> listDashboardPresets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDashboardPresets(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/dashboards/presets/{dashboardKey}")
    public Mono<CompatibilityEnvelope<BiDashboardPresetView>> getDashboardPreset(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDashboardPreset(tenantIdOrDefault(tenantId), dashboardKey));
    }

    @GetMapping("/capacity/quick-engine")
    public Mono<CompatibilityEnvelope<BiQuickEngineCapacitySummaryView>> quickEngineCapacity(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.quickEngineCapacity(tenantIdOrDefault(tenantId), limit));
    }

    @GetMapping("/capacity/quick-engine/queue")
    public Mono<CompatibilityEnvelope<BiQuickEngineQueueSnapshotView>> quickEngineQueue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String poolKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.quickEngineQueue(tenantIdOrDefault(tenantId), poolKey, status, limit));
    }

    @PostMapping("/resources/favorites")
    public Mono<CompatibilityEnvelope<BiResourceFavoriteView>> favoriteResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiResourceFavoriteCommand command) {
        return envelope(() -> facade.favoriteResource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/resources/favorites")
    public Mono<CompatibilityEnvelope<List<BiResourceFavoriteView>>> listFavoriteResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestParam(required = false) String resourceType) {
        return envelope(() -> facade.listFavoriteResources(
                tenantIdOrDefault(tenantId),
                actorOrDefault(actor),
                resourceType));
    }

    @DeleteMapping("/resources/favorites/{resourceType}/{resourceKey}")
    public Mono<CompatibilityEnvelope<Void>> unfavoriteResource(
            @PathVariable String resourceType,
            @PathVariable String resourceKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> {
            facade.unfavoriteResource(tenantIdOrDefault(tenantId), actorOrDefault(actor), resourceType, resourceKey);
            return null;
        });
    }

    @PostMapping("/capacity/quick-engine/alert-policy")
    public Mono<CompatibilityEnvelope<BiQuickEngineCapacityAlertPolicyView>> updateQuickEngineCapacityAlertPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQuickEngineCapacityAlertPolicyCommand command) {
        return envelope(() -> facade.updateQuickEngineCapacityAlertPolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/capacity/quick-engine/tenant-pool-policy")
    public Mono<CompatibilityEnvelope<BiQuickEngineTenantPoolPolicyView>> updateQuickEngineTenantPoolPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody BiQuickEngineTenantPoolPolicyCommand command) {
        return envelope(() -> facade.updateQuickEngineTenantPoolPolicy(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @PostMapping("/charts/resources/{chartKey}/draft")
    public Mono<CompatibilityEnvelope<BiChartView>> saveChartDraft(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody ChartDraftRequest request) {
        return envelope(() -> facade.upsertChart(
                tenantIdOrDefault(tenantId),
                request.toCommand(chartKey),
                actorOrDefault(actor)));
    }

    @GetMapping("/charts/resources")
    public Mono<CompatibilityEnvelope<List<BiChartView>>> listChartResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listChartResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/charts/resources/{chartKey}")
    public Mono<CompatibilityEnvelope<BiChartView>> getChartResource(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getChartResource(tenantIdOrDefault(tenantId), chartKey));
    }

    @GetMapping("/charts/resources/{chartKey}/impact")
    public Mono<CompatibilityEnvelope<BiChartReferenceImpactView>> chartReferenceImpact(
            @PathVariable String chartKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.chartReferenceImpact(tenantIdOrDefault(tenantId), chartKey));
    }

    @PostMapping("/dashboards/resources/{dashboardKey}/draft")
    public Mono<CompatibilityEnvelope<BiDashboardView>> saveDashboardDraft(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody DashboardDraftRequest request) {
        return envelope(() -> facade.upsertDashboard(
                tenantIdOrDefault(tenantId),
                request.toCommand(dashboardKey),
                actorOrDefault(actor)));
    }

    @GetMapping(value = "/dashboards/resources", params = "!workspaceId")
    public Mono<CompatibilityEnvelope<List<BiDashboardView>>> listDashboardResources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listDashboardResources(tenantIdOrDefault(tenantId)));
    }

    @GetMapping(value = "/dashboards/resources/{dashboardKey}", params = "!workspaceId")
    public Mono<CompatibilityEnvelope<BiDashboardView>> getDashboardResource(
            @PathVariable String dashboardKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.getDashboardResource(tenantIdOrDefault(tenantId), dashboardKey));
    }

    @GetMapping(value = "/dashboards/resources/{dashboardKey}", params = "workspaceId")
    public Mono<CompatibilityEnvelope<BiDashboardReadModelView>> dashboardReadModel(
            @PathVariable String dashboardKey,
            @RequestParam Long workspaceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.dashboardReadModel(tenantIdOrDefault(tenantId), workspaceId, dashboardKey));
    }

    @PostMapping("/permissions/resources")
    public Mono<CompatibilityEnvelope<BiPermissionGrantView>> grantPermission(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody PermissionGrantRequest request) {
        return envelope(() -> facade.grantPermission(
                tenantIdOrDefault(tenantId),
                request.toCommand(),
                actorOrDefault(actor)));
    }

    @GetMapping("/permissions/effective-access")
    public Mono<CompatibilityEnvelope<BiPermissionDecisionView>> effectiveAccess(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam Long workspaceId,
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam String actor,
            @RequestParam(value = "roles", required = false) List<String> roles,
            @RequestParam("action") String action) {
        return envelope(() -> facade.effectiveAccess(new BiAccessRequest(
                tenantIdOrDefault(tenantId),
                workspaceId,
                resourceType,
                resourceId,
                actor,
                roles == null ? Set.of() : Set.copyOf(roles),
                action)));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private record WorkspaceRequest(
            String workspaceKey,
            String name,
            String description,
            String status) {
    }

    private record DatasetDraftRequest(
            Long workspaceId,
            String datasetKey,
            String name,
            String datasetType,
            Long sourceRefId,
            String tableExpression,
            String tenantColumn,
            Map<String, Object> model,
            List<DatasetFieldRequest> fields,
            List<MetricRequest> metrics,
            String status) {

        private BiDatasetCommand toCommand(String pathDatasetKey) {
            return new BiDatasetCommand(
                    workspaceId,
                    pathDatasetKey,
                    name,
                    datasetType,
                    sourceRefId,
                    tableExpression,
                    tenantColumn,
                    model,
                    fields == null ? List.of() : fields.stream().map(DatasetFieldRequest::toCommand).toList(),
                    metrics == null ? List.of() : metrics.stream().map(MetricRequest::toCommand).toList(),
                    status);
        }
    }

    private record DatasetFieldRequest(
            String fieldKey,
            String displayName,
            String columnExpression,
            String roleKey,
            String dataType,
            String defaultAggregation,
            Boolean visible,
            Integer sortOrder) {

        private BiDatasetFieldCommand toCommand() {
            return new BiDatasetFieldCommand(
                    fieldKey,
                    displayName,
                    columnExpression,
                    roleKey,
                    dataType,
                    defaultAggregation,
                    visible,
                    sortOrder);
        }
    }

    private record MetricRequest(
            String metricKey,
            String displayName,
            String expression,
            String aggregation,
            String dataType,
            String unit) {

        private BiMetricCommand toCommand() {
            return new BiMetricCommand(metricKey, displayName, expression, aggregation, dataType, unit);
        }
    }

    private record ChartDraftRequest(
            Long workspaceId,
            String chartKey,
            String name,
            String chartType,
            String datasetKey,
            Map<String, Object> query,
            Map<String, Object> style,
            Map<String, Object> interaction,
            String status) {

        private BiChartCommand toCommand(String pathChartKey) {
            return new BiChartCommand(
                    workspaceId,
                    pathChartKey,
                    name,
                    chartType,
                    datasetKey,
                    query,
                    style,
                    interaction,
                    status);
        }
    }

    private record DashboardDraftRequest(
            Long workspaceId,
            String dashboardKey,
            String name,
            String description,
            Map<String, Object> theme,
            Map<String, Object> filters,
            List<String> chartKeys,
            String status) {

        private BiDashboardCommand toCommand(String pathDashboardKey) {
            return new BiDashboardCommand(
                    workspaceId,
                    pathDashboardKey,
                    name,
                    description,
                    theme,
                    filters,
                    chartKeys,
                    status);
        }
    }

    private record PermissionGrantRequest(
            Long workspaceId,
            String resourceType,
            Long resourceId,
            String subjectType,
            String subjectId,
            String actionKey,
            String effect) {

        private BiPermissionGrantCommand toCommand() {
            return new BiPermissionGrantCommand(
                    workspaceId,
                    resourceType,
                    resourceId,
                    subjectType,
                    subjectId,
                    actionKey,
                    effect);
        }
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
