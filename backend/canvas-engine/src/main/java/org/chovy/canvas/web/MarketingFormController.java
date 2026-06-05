package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
public class MarketingFormController {

    private final MarketingFormService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingFormController(MarketingFormService service) {
        this(service, null);
    }

    @Autowired
    public MarketingFormController(MarketingFormService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/canvas/marketing-forms")
    public Mono<R<List<MarketingFormService.FormDefinitionView>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/{id}")
    public Mono<R<MarketingFormService.FormDefinitionView>> get(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.get(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/canvas/marketing-forms")
    public Mono<R<MarketingFormService.FormDefinitionView>> create(@RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.create(tenantId, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}")
    public Mono<R<MarketingFormService.FormDefinitionView>> update(
            @PathVariable Long id,
            @RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.update(tenantId, id, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}/status")
    public Mono<R<MarketingFormService.FormDefinitionView>> setStatus(
            @PathVariable Long id,
            @RequestBody StatusReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.setStatus(
                        tenantId, id, Boolean.TRUE.equals(req.active()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/submissions")
    public Mono<R<List<MarketingFormService.SubmissionView>>> submissions(
            @RequestParam(required = false) Long formId,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.submissions(tenantId, formId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/public/marketing-forms/{publicKey}")
    public Mono<R<MarketingFormService.PublicFormView>> publicForm(@PathVariable String publicKey) {
        return Mono.fromCallable(() -> R.ok(service.publicForm(publicKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/public/marketing-forms/{publicKey}/submit")
    public Mono<R<MarketingFormService.SubmitResult>> submit(
            ServerHttpRequest request,
            @PathVariable String publicKey,
            @RequestBody PublicSubmitReq req) {
        return Mono.fromCallable(() -> R.ok(service.submit(publicKey, toPublicCommand(req, request))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private MarketingFormService.FormDefinitionCommand toCommand(FormDefinitionReq req) {
        return new MarketingFormService.FormDefinitionCommand(
                req.publicKey(),
                req.name(),
                req.description(),
                req.fieldSchemaJson(),
                req.submitActionJson(),
                req.successMessage(),
                req.active(),
                req.createdBy());
    }

    private MarketingFormService.PublicSubmitCommand toPublicCommand(PublicSubmitReq req, ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String ip = request.getRemoteAddress() == null ? null : request.getRemoteAddress().getAddress().getHostAddress();
        return new MarketingFormService.PublicSubmitCommand(
                req.response(),
                req.utm(),
                req.anonymousId(),
                req.idempotencyKey(),
                req.consentChannel(),
                req.consentStatus(),
                userAgent,
                hashText(ip));
    }

    private String hashText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private Mono<Long> currentTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    public record FormDefinitionReq(
            String publicKey,
            String name,
            String description,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            Boolean active,
            String createdBy) {
    }

    public record StatusReq(Boolean active) {
    }

    public record PublicSubmitReq(
            Map<String, Object> response,
            Map<String, Object> utm,
            String anonymousId,
            String idempotencyKey,
            String consentChannel,
            String consentStatus) {
    }
}
