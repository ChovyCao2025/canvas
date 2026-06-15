package org.chovy.canvas.web.publicingress;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.PublicIngressFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class PublicIngressController {

    private final PublicIngressFacade facade;

    public PublicIngressController(PublicIngressFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/public/marketing-forms/{publicKey}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> publicMarketingForm(@PathVariable String publicKey) {
        return envelope(() -> facade.publicMarketingForm(publicKey));
    }

    @PostMapping("/public/marketing-forms/{publicKey}/submit")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> submitMarketingForm(
            ServerHttpRequest request,
            @PathVariable String publicKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.submitMarketingForm(publicKey, safePayload(payload),
                request.getHeaders().toSingleValueMap()));
    }

    @GetMapping({
            "/public/conversation-webhooks/{tenantId}/whatsapp",
            "/public/conversations/webhooks/{tenantId}/whatsapp"
    })
    public Mono<CompatibilityEnvelope<String>> verifyWhatsApp(
            @PathVariable Long tenantId,
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return envelope(() -> facade.verifyWhatsApp(tenantId, mode, verifyToken, challenge));
    }

    @PostMapping({
            "/public/conversation-webhooks/{tenantId}/whatsapp",
            "/public/conversations/webhooks/{tenantId}/whatsapp"
    })
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> receiveWhatsApp(
            @PathVariable Long tenantId,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        return envelope(() -> facade.receiveWhatsApp(tenantId, signature, rawBody));
    }

    @PostMapping("/public/marketing/content/assets/upload-callbacks/{tenantId}/{provider}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> receiveAssetUploadCallback(
            @PathVariable Long tenantId,
            @PathVariable String provider,
            @RequestHeader("X-Canvas-Asset-Timestamp") String timestamp,
            @RequestHeader("X-Canvas-Asset-Signature") String signature,
            @RequestBody String rawBody) {
        return envelope(() -> facade.receiveAssetUploadCallback(tenantId, provider, timestamp, signature, rawBody));
    }

    @PostMapping("/public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> receiveMonitoringWebhook(
            @PathVariable Long tenantId,
            @PathVariable String sourceKey,
            @RequestHeader("X-Canvas-Monitoring-Timestamp") String timestamp,
            @RequestHeader("X-Canvas-Monitoring-Signature") String signature,
            @RequestBody String rawBody) {
        return envelope(() -> facade.receiveMonitoringWebhook(tenantId, sourceKey, timestamp, signature, rawBody));
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

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
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
