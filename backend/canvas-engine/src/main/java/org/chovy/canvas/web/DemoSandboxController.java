package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterCatalog;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.conversation.SandboxConversationReplyAdapter;
import org.chovy.canvas.domain.conversation.SandboxConversationReplyPayload;
import org.chovy.canvas.domain.demo.DemoSandboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo-sandboxes")
public class DemoSandboxController {

    private final DemoSandboxService service;
    private final TenantContextResolver tenantContextResolver;
    private final ConversationAdapterHarness conversationAdapterHarness;

    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationIngressService conversationIngressService) {
        this(service, tenantContextResolver, new ConversationAdapterHarness(
                conversationIngressService,
                new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()))));
    }

    @Autowired
    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationAdapterHarness conversationAdapterHarness) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
        this.conversationAdapterHarness = conversationAdapterHarness;
    }

    @PostMapping
    public Mono<R<DemoSandboxService.Sandbox>> install(@RequestBody InstallRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.install(request.tenantId(), request.demoName(), request.ttlDays())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/reset")
    public Mono<R<DemoSandboxService.ResetResult>> reset(@PathVariable Long tenantId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.reset(tenantId, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/expired")
    public Mono<R<List<DemoSandboxService.Sandbox>>> expired() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.expired()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/conversation-replies")
    public Mono<R<ConversationIngressResp>> reply(@PathVariable Long tenantId,
                                                  @RequestBody ConversationReplyRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(conversationAdapterHarness.ingest(
                                tenantId,
                                "SANDBOX",
                                toPayload(request),
                                context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private SandboxConversationReplyPayload toPayload(ConversationReplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("conversation reply request is required");
        }
        return new SandboxConversationReplyPayload(
                request.canvasId(),
                request.versionId(),
                request.executionId(),
                request.userId(),
                request.externalMessageId(),
                request.eventId(),
                request.text(),
                request.intent(),
                request.attributes());
    }

    public record InstallRequest(Long tenantId, String demoName, int ttlDays) {
    }

    public record ConversationReplyRequest(
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String externalMessageId,
            String eventId,
            String text,
            String intent,
            Map<String, Object> attributes) {
    }
}
