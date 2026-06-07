package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationPrivateDomainSyncService;
import org.chovy.canvas.domain.conversation.PrivateDomainContactQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainContactView;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupView;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncCommand;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncRunView;
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
@RequestMapping("/canvas/conversations/private-domain")
public class ConversationPrivateDomainController {

    private final ConversationPrivateDomainSyncService service;
    private final TenantContextResolver tenantContextResolver;

    public ConversationPrivateDomainController(ConversationPrivateDomainSyncService service,
                                               TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/sync-runs")
    public Mono<R<PrivateDomainSyncRunView>> ingest(@RequestBody PrivateDomainSyncCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.ingestSnapshot(
                                tenantId(context),
                                command,
                                actor(context))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contacts")
    public Mono<R<List<PrivateDomainContactView>>> contacts(
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.contacts(
                                tenantId(context),
                                new PrivateDomainContactQuery(provider, ownerUserId, keyword, boundedLimit(limit)))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/groups")
    public Mono<R<List<PrivateDomainGroupView>>> groups(
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.groups(
                                tenantId(context),
                                new PrivateDomainGroupQuery(provider, ownerUserId, boundedLimit(limit)))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/sync-runs")
    public Mono<R<List<PrivateDomainSyncRunView>>> syncRuns(
            @RequestParam String provider,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.syncRuns(
                                tenantId(context),
                                provider,
                                boundedLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
