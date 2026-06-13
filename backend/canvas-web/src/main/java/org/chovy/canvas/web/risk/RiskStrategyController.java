package org.chovy.canvas.web.risk;

import java.util.List;

import org.chovy.canvas.risk.api.RiskStrategyFacade;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class RiskStrategyController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final RiskStrategyFacade facade;

    public RiskStrategyController(RiskStrategyFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/canvas/risk/strategies")
    public Mono<CompatibilityEnvelope<List<RiskStrategyView>>> listStrategies(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String sceneKey) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(
                        facade.listStrategies(tenantIdOrDefault(tenantId), sceneKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }
    }
}
