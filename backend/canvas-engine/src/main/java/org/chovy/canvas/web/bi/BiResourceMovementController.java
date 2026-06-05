package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceLocationView;
import org.chovy.canvas.domain.bi.resource.BiResourceMoveCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceMovementService;
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
public class BiResourceMovementController {

    private final TenantContextResolver tenantContextResolver;
    private final BiResourceMovementService movementService;

    public BiResourceMovementController(TenantContextResolver tenantContextResolver,
                                        BiResourceMovementService movementService) {
        this.tenantContextResolver = tenantContextResolver;
        this.movementService = movementService;
    }

    @PostMapping({"/locations", "/move"})
    public Mono<R<BiResourceLocationView>> move(@RequestBody BiResourceMoveCommand command) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(movementService.move(
                                context.tenantId(),
                                context.username(),
                                command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/locations")
    public Mono<R<List<BiResourceLocationView>>> list(
            @RequestParam(required = false) String resourceType) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(movementService.list(
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
