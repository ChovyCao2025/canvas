package org.chovy.canvas.web.platform;

import java.util.function.Supplier;

import org.chovy.canvas.platform.api.PlatformActor;
import org.chovy.canvas.platform.api.TechnicalMigrationCandidateFacade;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceRequest;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/architecture/migration-candidates")
public class TechnicalMigrationCandidateController {

    private static final String DEFAULT_ACTOR = "system";

    private final TechnicalMigrationCandidateFacade facade;

    public TechnicalMigrationCandidateController(TechnicalMigrationCandidateFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/evidence")
    public Mono<CompatibilityEnvelope<TechnicalMigrationEvidenceView>> register(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody TechnicalMigrationEvidenceRequest request) {
        return envelope(() -> facade.register(new PlatformActor(tenantId, actorOrDefault(actor)), request));
    }

    @ExceptionHandler({IllegalArgumentException.class, SecurityException.class})
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
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
