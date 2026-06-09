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
/**
 * DemoSandboxController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class DemoSandboxController {

    private final DemoSandboxService service;
    private final TenantContextResolver tenantContextResolver;
    private final ConversationAdapterHarness conversationAdapterHarness;

    /**
     * 初始化 DemoSandboxController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param conversationIngressService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationIngressService conversationIngressService) {
        this(service, tenantContextResolver, new ConversationAdapterHarness(
                conversationIngressService,
                new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()))));
    }

    @Autowired
    /**
     * 初始化 DemoSandboxController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param conversationAdapterHarness conversation adapter harness 参数，用于 DemoSandboxController 流程中的校验、计算或对象转换。
     */
    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationAdapterHarness conversationAdapterHarness) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
        this.conversationAdapterHarness = conversationAdapterHarness;
    }

    @PostMapping
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 install 流程生成的业务结果。
     */
    public Mono<R<DemoSandboxService.Sandbox>> install(@RequestBody InstallRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.install(request.tenantId(), request.demoName(), request.ttlDays())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/reset")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 reset 流程生成的业务结果。
     */
    public Mono<R<DemoSandboxService.ResetResult>> reset(@PathVariable Long tenantId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.reset(tenantId, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/expired")
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @return 返回 expired 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<DemoSandboxService.Sandbox>>> expired() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.expired()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/conversation-replies")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 reply 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * InstallRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record InstallRequest(Long tenantId, String demoName, int ttlDays) {
    }

    /**
     * ConversationReplyRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
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
