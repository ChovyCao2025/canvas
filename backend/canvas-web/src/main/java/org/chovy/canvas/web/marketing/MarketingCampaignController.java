package org.chovy.canvas.web.marketing;

import java.util.List;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkView;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessView;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/marketing-campaigns")
public class MarketingCampaignController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingCampaignFacade facade;

    public MarketingCampaignController(MarketingCampaignFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<MarketingCampaignView>> upsertCampaign(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody MarketingCampaignCommand command) {
        return envelope(() -> facade.upsertCampaign(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<MarketingCampaignView>>> listCampaigns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listCampaigns(tenantIdOrDefault(tenantId), status, limit));
    }

    @PostMapping("/links")
    public Mono<CompatibilityEnvelope<MarketingCampaignLinkView>> linkResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody MarketingCampaignLinkCommand command) {
        return envelope(() -> facade.linkResource(
                tenantIdOrDefault(tenantId),
                command,
                actorOrDefault(actor)));
    }

    @GetMapping("/{campaignId}/links")
    public Mono<CompatibilityEnvelope<List<MarketingCampaignLinkView>>> listLinks(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long campaignId) {
        return envelope(() -> facade.listLinks(tenantIdOrDefault(tenantId), campaignId));
    }

    @GetMapping("/{campaignId}/readiness")
    public Mono<CompatibilityEnvelope<MarketingCampaignReadinessView>> readiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long campaignId) {
        return envelope(() -> facade.readiness(tenantIdOrDefault(tenantId), campaignId));
    }

    @DeleteMapping("/links/{linkId}")
    public Mono<CompatibilityEnvelope<Void>> unlinkResource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long linkId) {
        return envelope(() -> {
            facade.unlinkResource(tenantIdOrDefault(tenantId), linkId);
            return null;
        });
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
