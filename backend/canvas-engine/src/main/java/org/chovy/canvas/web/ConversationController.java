package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressReq;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.conversation.ConversationMessageView;
import org.chovy.canvas.domain.conversation.ConversationSessionView;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;

@RestController
@RequestMapping("/canvas/conversations")
public class ConversationController {

    private final ConversationIngressService service;
    private final TenantContextResolver tenantContextResolver;
    private final ConversationAdapterHarness adapterHarness;

    public ConversationController(ConversationIngressService service,
                                  TenantContextResolver tenantContextResolver) {
        this(service, tenantContextResolver, null);
    }

    @Autowired
    public ConversationController(ConversationIngressService service,
                                  TenantContextResolver tenantContextResolver,
                                  ConversationAdapterHarness adapterHarness) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
        this.adapterHarness = adapterHarness;
    }

    @PostMapping("/ingress")
    public Mono<R<ConversationIngressResp>> ingest(@RequestBody ConversationIngressReq req) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.ingest(tenantId(context), req)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/adapters/{adapterKey}/ingress")
    public Mono<R<ConversationIngressResp>> ingestAdapter(@PathVariable String adapterKey,
                                                          @RequestBody Map<String, Object> payload) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(requireAdapterHarness().ingestRaw(
                                tenantId(context),
                                adapterKey,
                                payload,
                                context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<ConversationSessionView>>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.listRecentSessions(
                                tenantId(context),
                                userId,
                                channel,
                                boundedLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{sessionId}/messages")
    public Mono<R<List<ConversationMessageView>>> messages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.listMessages(
                                tenantId(context),
                                sessionId,
                                boundedLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private ConversationAdapterHarness requireAdapterHarness() {
        if (adapterHarness == null) {
            throw new IllegalStateException("conversation adapter harness is required");
        }
        return adapterHarness;
    }
}
