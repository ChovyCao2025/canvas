package org.chovy.canvas.web.cdp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpBatchTrackCommand;
import org.chovy.canvas.cdp.api.CdpEventIngestionFacade;
import org.chovy.canvas.cdp.api.CdpIngestionResult;
import org.chovy.canvas.cdp.api.CdpWriteKeyAuthenticationFacade;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/cdp/events")
public class CdpEventIngestionController {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final CdpWriteKeyAuthenticationFacade writeKeyAuthenticationFacade;
    private final CdpEventIngestionFacade facade;

    public CdpEventIngestionController(CdpEventIngestionFacade facade) {
        this(null, facade);
    }

    @Autowired
    public CdpEventIngestionController(CdpWriteKeyAuthenticationFacade writeKeyAuthenticationFacade,
                                       CdpEventIngestionFacade facade) {
        this.writeKeyAuthenticationFacade = writeKeyAuthenticationFacade;
        this.facade = facade;
    }

    @PostMapping("/track")
    public Mono<CompatibilityEnvelope<CdpIngestionResult>> track(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Cdp-Write-Key", required = false) String writeKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) CdpBatchTrackCommand command) {
        return envelope(() -> facade.ingestBatch(
                authenticateWriteKey(authorizationHeader, writeKey, tenantId),
                command == null ? new CdpBatchTrackCommand(List.of(), null) : command));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private CdpWriteKeyView authenticateWriteKey(String authorizationHeader, String writeKey, Long tenantId) {
        if (writeKeyAuthenticationFacade != null && authorizationHeader != null && !authorizationHeader.isBlank()) {
            return writeKeyAuthenticationFacade.authenticate(authorizationHeader);
        }
        return writeKeyView(writeKey, tenantId);
    }

    private static CdpWriteKeyView writeKeyView(String writeKey, Long tenantId) {
        if (writeKey == null || writeKey.isBlank()) {
            throw new IllegalArgumentException("X-Cdp-Write-Key is required");
        }
        return new CdpWriteKeyView(0L, tenantId == null ? DEFAULT_TENANT_ID : tenantId,
                writeKey.trim(), "WEB", null, (LocalDateTime) null);
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
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
