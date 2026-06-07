package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.template.MessageTemplateService;
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
@RequestMapping("/message-templates")
public class MessageTemplateController {

    private final MessageTemplateService service;
    private final TenantContextResolver tenantContextResolver;

    public MessageTemplateController(MessageTemplateService service,
                                     TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<MessageTemplateService.Template>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.search(context, keyword, channel)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<MessageTemplateService.Template>> create(
            @RequestBody MessageTemplateService.TemplateDraft draft) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.create(context, draft)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{templateCode}/preview")
    public Mono<R<MessageTemplateService.PreviewResult>> preview(
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(service.preview(tenant, templateCode, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
