package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionView;
import org.chovy.canvas.domain.bi.permission.BiPermissionAdminService;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionView;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionView;
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
@RequestMapping("/canvas/bi/permissions")
public class BiPermissionController {

    private final TenantContextResolver tenantContextResolver;
    private final BiPermissionAdminService permissionAdminService;

    public BiPermissionController(TenantContextResolver tenantContextResolver,
                                  BiPermissionAdminService permissionAdminService) {
        this.tenantContextResolver = tenantContextResolver;
        this.permissionAdminService = permissionAdminService;
    }

    @GetMapping("/resources")
    public Mono<R<List<BiResourcePermissionView>>> listResourcePermissions(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) Long resourceId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listResourcePermissions(
                                context.tenantId(),
                                resourceType,
                                resourceKey,
                                resourceId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/resources")
    public Mono<R<BiResourcePermissionView>> upsertResourcePermission(
            @RequestBody BiResourcePermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertResourcePermission(
                                context.tenantId(),
                                context.username(),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/resources/{id}")
    public Mono<R<Void>> deleteResourcePermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteResourcePermission(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/rows")
    public Mono<R<List<BiRowPermissionView>>> listRowPermissions(
            @RequestParam(required = false) String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listRowPermissions(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/rows")
    public Mono<R<BiRowPermissionView>> upsertRowPermission(
            @RequestBody BiRowPermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertRowPermission(context.tenantId(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/rows/{id}")
    public Mono<R<Void>> deleteRowPermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteRowPermission(context.tenantId(), id);
                    return R.<Void>ok(null);
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/columns")
    public Mono<R<List<BiColumnPermissionView>>> listColumnPermissions(
            @RequestParam(required = false) String datasetKey) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.listColumnPermissions(context.tenantId(), datasetKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/columns")
    public Mono<R<BiColumnPermissionView>> upsertColumnPermission(
            @RequestBody BiColumnPermissionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(permissionAdminService.upsertColumnPermission(context.tenantId(), command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/columns/{id}")
    public Mono<R<Void>> deleteColumnPermission(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    permissionAdminService.deleteColumnPermission(context.tenantId(), id);
                    return R.<Void>ok(null);
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
