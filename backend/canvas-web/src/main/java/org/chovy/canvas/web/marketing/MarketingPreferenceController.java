package org.chovy.canvas.web.marketing;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/marketing-preferences")
public class MarketingPreferenceController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final MarketingPreferenceFacade facade;

    public MarketingPreferenceController(MarketingPreferenceFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/users/{userId}")
    public Mono<CompatibilityEnvelope<MarketingPreferenceFacade.PreferenceReport>> report(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> facade.report(tenantIdOrDefault(tenantId), userId));
    }

    @PutMapping("/users/{userId}/consents/{channel}")
    public Mono<CompatibilityEnvelope<MarketingPreferenceFacade.ConsentRow>> updateConsent(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ConsentUpdateReq req) {
        return envelope(() -> facade.updateConsent(tenantIdOrDefault(tenantId), userId,
                new MarketingPreferenceFacade.ConsentUpdateCommand(channel, req.consentStatus(), req.source())));
    }

    @PutMapping("/users/{userId}/channels/{channel}")
    public Mono<CompatibilityEnvelope<MarketingPreferenceFacade.ChannelRow>> updateChannel(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ChannelUpdateReq req) {
        return envelope(() -> facade.updateChannel(tenantIdOrDefault(tenantId), userId,
                new MarketingPreferenceFacade.ChannelUpdateCommand(channel, req.address(), req.enabled(),
                        req.verified(), req.metadata())));
    }

    @PostMapping("/users/{userId}/suppressions")
    public Mono<CompatibilityEnvelope<MarketingPreferenceFacade.SuppressionRow>> addSuppression(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId,
            @RequestBody SuppressionCreateReq req) {
        return envelope(() -> facade.addSuppression(tenantIdOrDefault(tenantId), userId,
                new MarketingPreferenceFacade.SuppressionCreateCommand(req.channel(), req.reason(), req.active(),
                        req.expiresAt())));
    }

    @PutMapping("/suppressions/{id}/deactivate")
    public Mono<CompatibilityEnvelope<Void>> deactivateSuppression(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> {
            facade.deactivateSuppression(tenantIdOrDefault(tenantId), id);
            return null;
        });
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

    public record ConsentUpdateReq(String consentStatus, String source) {
    }

    public record ChannelUpdateReq(String address, Boolean enabled, Boolean verified, String metadata) {
    }

    public record SuppressionCreateReq(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
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
