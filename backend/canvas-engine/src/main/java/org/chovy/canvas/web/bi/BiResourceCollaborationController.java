package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentView;
import org.chovy.canvas.domain.bi.resource.BiResourceLockCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceLockView;
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
public class BiResourceCollaborationController {

    private final TenantContextResolver tenantContextResolver;
    private final BiResourceCollaborationService collaborationService;

    public BiResourceCollaborationController(TenantContextResolver tenantContextResolver,
                                             BiResourceCollaborationService collaborationService) {
        this.tenantContextResolver = tenantContextResolver;
        this.collaborationService = collaborationService;
    }

    @PostMapping("/comments")
    public Mono<R<BiResourceCommentView>> addComment(@RequestBody BiResourceCommentCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.addComment(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/comments")
    public Mono<R<List<BiResourceCommentView>>> listComments(
            @RequestParam String resourceType,
            @RequestParam String resourceKey) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.listComments(
                                tenantId(context), resourceType, resourceKey)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/comments/{commentId}")
    public Mono<R<Void>> deleteComment(@PathVariable Long commentId) {
        return currentTenant().flatMap(context ->
                Mono.fromRunnable(() -> collaborationService.deleteComment(
                                tenantId(context), username(context), commentId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    @PostMapping("/locks/acquire")
    public Mono<R<BiResourceLockView>> acquireLock(@RequestBody BiResourceLockCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.acquireLock(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/locks")
    public Mono<R<BiResourceLockView>> currentLock(
            @RequestParam String resourceType,
            @RequestParam String resourceKey) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.currentLock(
                                tenantId(context), resourceType, resourceKey)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/locks/release")
    public Mono<R<Void>> releaseLock(@RequestBody BiResourceLockCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromRunnable(() -> collaborationService.releaseLock(
                                tenantId(context), username(context), command))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    private String username(TenantContext context) {
        return context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
