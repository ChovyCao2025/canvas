package org.chovy.canvas.web.risk;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.risk.api.RiskGovernanceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class RiskGovernanceController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final RiskGovernanceFacade facade;

    public RiskGovernanceController(RiskGovernanceFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/canvas/risk/decisions/traces")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> decisionTraces(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String sceneKey,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.decisionTraces(tenantIdOrDefault(tenantId), sceneKey, limit));
    }

    @PostMapping("/canvas/risk/lists")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createList(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createList(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/lists/{listKey}/entries")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> addListEntry(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String listKey,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.addListEntry(tenantIdOrDefault(tenantId), listKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/canvas/risk/lists/{listKey}/entries")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listEntries(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String listKey) {
        return envelope(() -> facade.listEntries(tenantIdOrDefault(tenantId), listKey));
    }

    @DeleteMapping("/canvas/risk/lists/{listKey}/entries/{entryId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> removeListEntry(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String listKey,
            @PathVariable Long entryId) {
        return envelope(() -> facade.removeListEntry(tenantIdOrDefault(tenantId), listKey, entryId,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/lists/{listKey}/entries/import")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> importListEntries(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String listKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.importListEntries(tenantIdOrDefault(tenantId), listKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createStrategyDraft(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createStrategyDraft(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/canvas/risk/strategies/{strategyKey}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> strategy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String strategyKey) {
        return envelope(() -> facade.getStrategy(tenantIdOrDefault(tenantId), strategyKey));
    }

    @GetMapping("/canvas/risk/strategies/{strategyKey}/versions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> strategyVersions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String strategyKey) {
        return envelope(() -> facade.listStrategyVersions(tenantIdOrDefault(tenantId), strategyKey));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/versions/{version}/validate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> validateStrategyVersion(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @PathVariable Integer version) {
        return envelope(() -> facade.validateStrategyVersion(tenantIdOrDefault(tenantId), strategyKey, version,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/versions/{version}/simulate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> simulateStrategyVersion(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @PathVariable Integer version) {
        return envelope(() -> facade.simulateStrategyVersion(tenantIdOrDefault(tenantId), strategyKey, version,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/versions/{version}/submit")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> submitStrategyVersion(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @PathVariable Integer version) {
        return envelope(() -> facade.submitStrategyVersion(tenantIdOrDefault(tenantId), strategyKey, version,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/versions/{version}/approve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> approveStrategyVersion(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @PathVariable Integer version) {
        return envelope(() -> facade.approveStrategyVersion(tenantIdOrDefault(tenantId), strategyKey, version,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/versions/{version}/activate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> activateStrategyVersion(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @PathVariable Integer version) {
        return envelope(() -> facade.activateStrategyVersion(tenantIdOrDefault(tenantId), strategyKey, version,
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/rollback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rollbackStrategy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.rollbackStrategy(tenantIdOrDefault(tenantId), strategyKey, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/canvas/risk/strategies/{strategyKey}/pause")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pauseStrategy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String strategyKey) {
        return envelope(() -> facade.pauseStrategy(tenantIdOrDefault(tenantId), strategyKey, actorOrDefault(actor)));
    }

    @GetMapping("/canvas/risk/strategies/{strategyKey}/versions/{left}/diff/{right}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> diffStrategyVersions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String strategyKey,
            @PathVariable Integer left,
            @PathVariable Integer right) {
        return envelope(() -> facade.diffStrategyVersions(tenantIdOrDefault(tenantId), strategyKey, left, right));
    }

    @PostMapping("/canvas/risk/lab/simulations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> startSimulation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.startSimulation(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/canvas/risk/lab/simulations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> simulations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String sceneKey,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listSimulations(tenantIdOrDefault(tenantId), sceneKey, limit));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
