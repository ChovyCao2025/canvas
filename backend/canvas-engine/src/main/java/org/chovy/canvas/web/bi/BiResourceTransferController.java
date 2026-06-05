package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceOwnershipView;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferService;
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
@RequestMapping("/canvas/bi/resources")
public class BiResourceTransferController {

    private final TenantContextResolver tenantContextResolver;
    private final BiResourceTransferService transferService;

    public BiResourceTransferController(TenantContextResolver tenantContextResolver,
                                        BiResourceTransferService transferService) {
        this.tenantContextResolver = tenantContextResolver;
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public Mono<R<BiResourceOwnershipView>> transfer(@RequestBody BiResourceTransferCommand command) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(transferService.transfer(
                                context.tenantId(),
                                context.username(),
                                command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/ownerships")
    public Mono<R<List<BiResourceOwnershipView>>> list(
            @RequestParam(required = false) String resourceType) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(transferService.list(
                                context.tenantId(),
                                resourceType)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
