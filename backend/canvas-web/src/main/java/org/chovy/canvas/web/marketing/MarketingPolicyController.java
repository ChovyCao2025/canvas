package org.chovy.canvas.web.marketing;

import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/policies")
public class MarketingPolicyController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final MarketingPolicyFacade facade;

    public MarketingPolicyController(MarketingPolicyFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/state")
    public Mono<CompatibilityEnvelope<MarketingPolicyFacade.PolicyState>> policyState(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String userId,
            @RequestParam String channel) {
        return envelope(() -> facade.policyState(tenantIdOrDefault(tenantId), userId, channel));
    }

    @PostMapping("/consent")
    public Mono<CompatibilityEnvelope<MarketingPolicyFacade.ConsentView>> upsertConsent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) MarketingPolicyFacade.ConsentCommand command) {
        return envelope(() -> facade.upsertConsent(tenantIdOrDefault(tenantId), command));
    }

    @PostMapping("/suppression")
    public Mono<CompatibilityEnvelope<MarketingPolicyFacade.SuppressionView>> upsertSuppression(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) MarketingPolicyFacade.SuppressionCommand command) {
        return envelope(() -> facade.upsertSuppression(tenantIdOrDefault(tenantId), command));
    }

    @PostMapping("/channel")
    public Mono<CompatibilityEnvelope<MarketingPolicyFacade.ChannelView>> upsertChannel(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) MarketingPolicyFacade.ChannelCommand command) {
        return envelope(() -> facade.upsertChannel(tenantIdOrDefault(tenantId), command));
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

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
