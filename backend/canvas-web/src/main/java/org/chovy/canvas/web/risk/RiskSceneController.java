package org.chovy.canvas.web.risk;

import java.util.List;

import org.chovy.canvas.risk.api.RiskSceneFacade;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class RiskSceneController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final RiskSceneFacade facade;

    public RiskSceneController(RiskSceneFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/canvas/risk/scenes")
    public Mono<CompatibilityEnvelope<List<RiskSceneView>>> listScenes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.listScenes(tenantIdOrDefault(tenantId))))
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
