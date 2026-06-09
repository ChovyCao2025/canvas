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

/**
 * MessageTemplateController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/message-templates")
public class MessageTemplateController {

    private final MessageTemplateService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 MessageTemplateController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MessageTemplateController(MessageTemplateService service,
                                     TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 消息 Template 请求接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param keyword 搜索关键字，可选。
     * @param channel 渠道过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<MessageTemplateService.Template>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.search(context, keyword, channel)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 消息 Template接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param draft 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含创建 消息 Template后的业务数据。
     */
    @PostMapping
    public Mono<R<MessageTemplateService.Template>> create(
            @RequestBody MessageTemplateService.TemplateDraft draft) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.create(context, draft)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 预览消息 Template结果接口，对应 POST /{templateCode}/preview。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param templateCode 路径参数。
     * @param context 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含预览消息 Template结果后的业务数据。
     */
    @PostMapping("/{templateCode}/preview")
    public Mono<R<MessageTemplateService.PreviewResult>> preview(
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> context) {
        return tenantContextResolver.currentOrError()
                .flatMap(tenant -> Mono.fromCallable(() -> R.ok(service.preview(tenant, templateCode, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
