package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractAuditEventView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAutomationService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloEvaluationView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationContractController {

    private final MarketingIntegrationContractService service;
    private final MarketingIntegrationContractProbeService probeService;
    private final MarketingIntegrationContractProbeAutomationService probeAutomationService;
    private final MarketingIntegrationContractSloService sloService;
    private final TenantContextResolver tenantContextResolver;

    public MarketingIntegrationContractController(MarketingIntegrationContractService service,
                                                  MarketingIntegrationContractProbeService probeService,
                                                  MarketingIntegrationContractProbeAutomationService probeAutomationService,
                                                  MarketingIntegrationContractSloService sloService,
                                                  TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.probeService = probeService;
        this.probeAutomationService = probeAutomationService;
        this.sloService = sloService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/contracts")
    public Mono<R<MarketingIntegrationContractView>> upsertContract(
            @RequestBody MarketingIntegrationContractCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertContract(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contracts")
    public Mono<R<List<MarketingIntegrationContractView>>> listContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listContracts(tenantId(context), status, providerFamily, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contracts/{contractId}/audit-events")
    public Mono<R<List<MarketingIntegrationContractAuditEventView>>> listContractAuditEvents(
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listAuditEvents(tenantId(context), contractId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/contracts/{contractId}")
    public Mono<R<MarketingIntegrationContractView>> archiveContract(@PathVariable Long contractId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.archiveContract(tenantId(context), contractId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts/{contractId}/probe-runs")
    public Mono<R<MarketingIntegrationContractProbeRunView>> recordProbeRun(
            @PathVariable Long contractId,
            @RequestBody MarketingIntegrationContractProbeRunCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.recordProbeRun(tenantId(context), contractId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contract-probe-runs")
    public Mono<R<List<MarketingIntegrationContractProbeRunView>>> listProbeRuns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerFamily,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.listProbeRuns(tenantId(context), status, providerFamily, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contract-probe-runs/scan")
    public Mono<R<MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary>> scanProbeRuns(
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeAutomationService.scanProductionContracts(tenantId(context), limit, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contract-slo-evaluations")
    public Mono<R<List<MarketingIntegrationContractSloEvaluationView>>> listContractSloEvaluations(
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(sloService.listProductionSloEvaluations(tenantId(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
