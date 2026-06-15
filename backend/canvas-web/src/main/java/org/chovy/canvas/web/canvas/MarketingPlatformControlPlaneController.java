package org.chovy.canvas.web.canvas;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.MarketingPlatformControlPlaneFacade;
import org.chovy.canvas.canvas.application.MarketingPlatformControlPlaneApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/marketing-platform")
public class MarketingPlatformControlPlaneController {

    private final MarketingPlatformControlPlaneFacade facade;

    public MarketingPlatformControlPlaneController() {
        this(new MarketingPlatformControlPlaneApplicationService());
    }

    public MarketingPlatformControlPlaneController(MarketingPlatformControlPlaneFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/control-plane")
    public Mono<CompatibilityEnvelope<MarketingPlatformControlPlaneFacade.ControlPlaneSummaryView>> controlPlane(
            @RequestHeader(name = "X-Tenant-Id", required = false) Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(facade.summary(scopedTenantId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
