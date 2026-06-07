package org.chovy.canvas.web;

import org.chovy.canvas.architecture.TechnicalMigrationCandidateService;
import org.chovy.canvas.architecture.TechnicalMigrationCandidateEvidenceRecord;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/architecture/migration-candidates")
public class TechnicalMigrationCandidateController {

    private final TechnicalMigrationCandidateService service;
    private final TenantContextResolver tenantContextResolver;

    public TechnicalMigrationCandidateController(TechnicalMigrationCandidateService service,
                                                 TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/evidence")
    public Mono<R<TechnicalMigrationCandidateEvidenceRecord>> register(
            @RequestBody TechnicalMigrationCandidateService.EvidenceRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.register(context, request)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
