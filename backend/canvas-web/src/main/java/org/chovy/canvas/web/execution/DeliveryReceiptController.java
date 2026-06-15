package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.execution.api.DeliveryReceiptFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/delivery/receipts")
public class DeliveryReceiptController {

    private final DeliveryReceiptFacade facade;
    private final String receiptSecret;

    public DeliveryReceiptController(
            DeliveryReceiptFacade facade,
            @Value("${canvas.delivery.receipt.secret:}") String receiptSecret) {
        this.facade = facade;
        this.receiptSecret = receiptSecret;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<DeliveryReceiptFacade.ReceiptView>> receive(
            @RequestHeader(value = "X-Canvas-Receipt-Secret", required = false) String secret,
            @RequestBody ReceiptCallbackReq request) {
        return envelope(() -> {
            requireValidSecret(secret);
            request.validate();
            return facade.recordReceipt(new DeliveryReceiptFacade.ReceiptCommand(
                    request.provider(),
                    request.providerMessageId(),
                    request.receiptType(),
                    request.idempotencyKey(),
                    request.receivedAt(),
                    request.rawPayload()));
        });
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> forbidden(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CompatibilityEnvelope.forbidden(exception.getMessage()));
    }

    private void requireValidSecret(String candidate) {
        if (receiptSecret == null || receiptSecret.isBlank()) {
            throw new AccessDeniedException("receipt secret is not configured");
        }
        byte[] expected = receiptSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (candidate == null ? "" : candidate).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new AccessDeniedException("invalid receipt signature");
        }
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private record ReceiptCallbackReq(
            String provider,
            String providerMessageId,
            String receiptType,
            String idempotencyKey,
            LocalDateTime receivedAt,
            Map<String, Object> rawPayload) {
        private void validate() {
            if (provider == null || provider.isBlank()) {
                throw new IllegalArgumentException("provider is required");
            }
            if (providerMessageId == null || providerMessageId.isBlank()) {
                throw new IllegalArgumentException("providerMessageId is required");
            }
            if (receiptType == null || receiptType.isBlank()) {
                throw new IllegalArgumentException("receiptType is required");
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }

        private static CompatibilityEnvelope<Object> forbidden(String message) {
            return new CompatibilityEnvelope<>(403, message, "API_403", null, null);
        }
    }
}
