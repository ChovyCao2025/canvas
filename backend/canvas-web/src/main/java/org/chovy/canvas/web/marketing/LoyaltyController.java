package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.LoyaltyFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/loyalty")
public class LoyaltyController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final LoyaltyFacade facade;

    public LoyaltyController(LoyaltyFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/users/{userId}/account")
    public Mono<CompatibilityEnvelope<LoyaltyFacade.LoyaltyAccountView>> account(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> facade.account(tenantIdOrDefault(tenantId), userId));
    }

    @PostMapping("/users/{userId}/earn")
    public Mono<CompatibilityEnvelope<LoyaltyFacade.LoyaltyAccountView>> earn(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId,
            @RequestBody(required = false) LoyaltyFacade.EarnCommand command) {
        return envelope(() -> facade.earn(tenantIdOrDefault(tenantId), userId, command));
    }

    @PostMapping("/users/{userId}/redeem")
    public Mono<CompatibilityEnvelope<LoyaltyFacade.RedemptionView>> redeem(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId,
            @RequestBody(required = false) LoyaltyFacade.RedemptionCommand command) {
        return envelope(() -> facade.redeem(tenantIdOrDefault(tenantId), userId, command));
    }

    @GetMapping("/users/{userId}/benefits")
    public Mono<CompatibilityEnvelope<List<LoyaltyFacade.BenefitEligibilityView>>> benefits(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> facade.eligibleBenefits(tenantIdOrDefault(tenantId), userId));
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
