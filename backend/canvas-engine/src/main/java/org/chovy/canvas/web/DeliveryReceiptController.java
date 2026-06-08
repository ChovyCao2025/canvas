package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/delivery/receipts")
public class DeliveryReceiptController {

    private final DeliveryOutboxService outboxService;
    private final String receiptSecret;

    public DeliveryReceiptController(DeliveryOutboxService outboxService,
                                     @Value("${canvas.delivery.receipt.secret:}") String receiptSecret) {
        this.outboxService = outboxService;
        this.receiptSecret = receiptSecret;
    }

    @PostMapping
    public Mono<R<DeliveryReceiptLog>> receive(@RequestHeader(value = "X-Canvas-Receipt-Secret", required = false) String secret,
                                               @RequestBody ReceiptCallbackReq req) {
        return Mono.fromCallable(() -> {
            requireValidSecret(secret);
            req.validate();
            DeliveryReceiptRequest request = new DeliveryReceiptRequest(
                    req.provider,
                    req.providerMessageId,
                    req.receiptType,
                    req.idempotencyKey,
                    req.receivedAt,
                    req.rawPayload == null ? Map.of() : req.rawPayload
            );
            return R.ok(outboxService.recordReceipt(request));
        }).subscribeOn(Schedulers.boundedElastic());
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

    @Data
    public static class ReceiptCallbackReq {
        private String provider;
        private String providerMessageId;
        private String receiptType;
        private String idempotencyKey;
        private LocalDateTime receivedAt;
        private Map<String, Object> rawPayload;

        void validate() {
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
}
