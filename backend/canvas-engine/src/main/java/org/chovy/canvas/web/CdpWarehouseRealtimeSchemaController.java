package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/realtime/schemas")
public class CdpWarehouseRealtimeSchemaController {

    private final CdpWarehouseRealtimeSchemaService schemaService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimeSchemaController(CdpWarehouseRealtimeSchemaService schemaService) {
        this(schemaService, null);
    }

    @Autowired
    public CdpWarehouseRealtimeSchemaController(CdpWarehouseRealtimeSchemaService schemaService,
                                                TenantContextResolver tenantContextResolver) {
        this.schemaService = schemaService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<CdpWarehouseRealtimeSchemaService.SchemaVersionView>> register(
            @RequestBody SchemaVersionReq req) {
        SchemaVersionReq request = req == null ? new SchemaVersionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.register(
                                normalizeTenant(context), request.toCommand(), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseRealtimeSchemaService.SchemaVersionView>>> list(
            @RequestParam String pipelineKey,
            @RequestParam(required = false) String schemaRole,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.list(normalizeTenant(context), pipelineKey, schemaRole, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/latest")
    public Mono<R<CdpWarehouseRealtimeSchemaService.SchemaVersionView>> latest(
            @RequestParam String pipelineKey,
            @RequestParam String schemaRole) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(schemaService.latest(normalizeTenant(context), pipelineKey, schemaRole)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String operator(SchemaVersionReq request, TenantContext context) {
        if (request != null && request.getRegisteredBy() != null && !request.getRegisteredBy().isBlank()) {
            return request.getRegisteredBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class SchemaVersionReq {
        private String pipelineKey;
        private String schemaRole;
        private String schemaVersion;
        private String schemaJson;
        private String compatibilityPolicy;
        private Boolean active;
        private String registeredBy;

        CdpWarehouseRealtimeSchemaService.SchemaVersionCommand toCommand() {
            return new CdpWarehouseRealtimeSchemaService.SchemaVersionCommand(
                    pipelineKey,
                    schemaRole,
                    schemaVersion,
                    schemaJson,
                    compatibilityPolicy,
                    active);
        }
    }
}
