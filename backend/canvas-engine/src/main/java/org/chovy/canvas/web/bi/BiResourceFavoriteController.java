package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteService;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteView;
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
@RequestMapping("/canvas/bi/resources")
public class BiResourceFavoriteController {

    private final TenantContextResolver tenantContextResolver;
    private final BiResourceFavoriteService favoriteService;

    public BiResourceFavoriteController(TenantContextResolver tenantContextResolver,
                                        BiResourceFavoriteService favoriteService) {
        this.tenantContextResolver = tenantContextResolver;
        this.favoriteService = favoriteService;
    }

    @PostMapping("/favorites")
    public Mono<R<BiResourceFavoriteView>> favorite(@RequestBody BiResourceFavoriteCommand command) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(favoriteService.favorite(
                                context.tenantId(),
                                context.username(),
                                command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/favorites")
    public Mono<R<List<BiResourceFavoriteView>>> list(
            @RequestParam(required = false) String resourceType) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(favoriteService.list(
                                context.tenantId(),
                                context.username(),
                                resourceType)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/favorites/{resourceType}/{resourceKey}")
    public Mono<R<Void>> unfavorite(
            @PathVariable String resourceType,
            @PathVariable String resourceKey) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> {
                            favoriteService.unfavorite(
                                    context.tenantId(),
                                    context.username(),
                                    resourceType,
                                    resourceKey);
                            return R.ok();
                        })
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
