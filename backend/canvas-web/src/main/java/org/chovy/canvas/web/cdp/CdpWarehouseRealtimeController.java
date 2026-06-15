package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseRealtimeFacade;
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
@RequestMapping("/warehouse/realtime")
public class CdpWarehouseRealtimeController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWarehouseRealtimeFacade facade;

    public CdpWarehouseRealtimeController(CdpWarehouseRealtimeFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> status(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.realtimeStatus(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/schemas")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> registerSchema(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.registerSchema(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/schemas")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> schemas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String pipelineKey,
            @RequestParam(required = false) String schemaRole,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSchemas(tenantIdOrDefault(tenantId), pipelineKey, schemaRole, limit));
    }

    @GetMapping("/schemas/latest")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> latestSchema(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String pipelineKey,
            @RequestParam String schemaRole) {
        return envelope(() -> facade.latestSchema(tenantIdOrDefault(tenantId), pipelineKey, schemaRole));
    }

    @GetMapping("/pipelines/contracts")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> pipelineContracts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String lifecycleStatus) {
        return envelope(() -> facade.listPipelineContracts(tenantIdOrDefault(tenantId), lifecycleStatus));
    }

    @PostMapping("/pipelines/contracts")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertPipelineContract(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertPipelineContract(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/pipelines/checkpoints")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reportCheckpoint(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.reportCheckpoint(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/pipelines/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pipelineStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer recentLimit) {
        return envelope(() -> facade.pipelineStatus(tenantIdOrDefault(tenantId), recentLimit));
    }

    @PostMapping("/jobs/incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanJobIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String pipelineKey,
            @RequestParam(required = false) Long maxHeartbeatAgeSeconds,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.scanJobIncidents(tenantIdOrDefault(tenantId), pipelineKey,
                maxHeartbeatAgeSeconds, limit));
    }

    @PostMapping("/jobs/heartbeats")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> heartbeat(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.heartbeat(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/jobs/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> jobStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String pipelineKey,
            @RequestParam(required = false) Long maxHeartbeatAgeSeconds,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.jobStatus(tenantIdOrDefault(tenantId), pipelineKey, maxHeartbeatAgeSeconds,
                limit));
    }

    @PostMapping("/jobs/actions")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> requestAction(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.requestAction(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/jobs/actions/pending")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> pendingActions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String pipelineKey,
            @RequestParam String jobKey,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.pendingActions(tenantIdOrDefault(tenantId), pipelineKey, jobKey, limit));
    }

    @PostMapping("/jobs/actions/{actionId}/ack")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> acknowledgeAction(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long actionId) {
        return envelope(() -> facade.acknowledgeAction(tenantIdOrDefault(tenantId), actionId));
    }

    @PostMapping("/jobs/actions/{actionId}/complete")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> completeAction(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long actionId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.completeAction(tenantIdOrDefault(tenantId), actionId,
                string(body.get("status")), string(body.get("resultMessage"))));
    }

    @PostMapping("/pipelines/incidents/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanPipelineIncidents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer recentLimit) {
        return envelope(() -> facade.scanPipelineIncidents(tenantIdOrDefault(tenantId), recentLimit));
    }

    @PostMapping("/job-probes/targets")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertProbeTarget(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertProbeTarget(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/job-probes/targets")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> probeTargets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Boolean includeDisabled,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listProbeTargets(tenantIdOrDefault(tenantId), includeDisabled, limit));
    }

    @PostMapping("/job-probes/targets/{targetId}/enabled")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> setProbeTargetEnabled(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long targetId,
            @RequestParam Boolean enabled) {
        return envelope(() -> facade.setProbeTargetEnabled(tenantIdOrDefault(tenantId), targetId, enabled));
    }

    @PostMapping("/job-probes/scan")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> scanProbeTargets(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.scanProbeTargets(tenantIdOrDefault(tenantId), targetId, limit));
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

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
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
