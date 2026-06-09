package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
/**
 * MarketingFormController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class MarketingFormController {

    private final MarketingFormService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 MarketingFormController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingFormController(MarketingFormService service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 MarketingFormController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingFormController(MarketingFormService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/canvas/marketing-forms")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<MarketingFormService.FormDefinitionView>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/{id}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> get(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.get(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/canvas/marketing-forms")
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> create(@RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.create(tenantId, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> update(
            @PathVariable Long id,
            @RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.update(tenantId, id, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}/status")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 setStatus 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> setStatus(
            @PathVariable Long id,
            @RequestBody StatusReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.setStatus(
                        tenantId, id, Boolean.TRUE.equals(req.active()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/submissions")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param formId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 submissions 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<MarketingFormService.SubmissionView>>> submissions(
            @RequestParam(required = false) Long formId,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.submissions(tenantId, formId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/public/marketing-forms/{publicKey}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @return 返回 publicForm 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.PublicFormView>> publicForm(@PathVariable String publicKey) {
        return Mono.fromCallable(() -> R.ok(service.publicForm(publicKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/public/marketing-forms/{publicKey}/submit")
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.SubmitResult>> submit(
            ServerHttpRequest request,
            @PathVariable String publicKey,
            @RequestBody PublicSubmitReq req) {
        return Mono.fromCallable(() -> R.ok(service.submit(publicKey, toPublicCommand(req, request))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingFormService.FormDefinitionCommand toCommand(FormDefinitionReq req) {
        return new MarketingFormService.FormDefinitionCommand(
                req.publicKey(),
                req.name(),
                req.description(),
                req.fieldSchemaJson(),
                req.submitActionJson(),
                req.successMessage(),
                req.active(),
                req.createdBy());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingFormService.PublicSubmitCommand toPublicCommand(PublicSubmitReq req, ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String ip = request.getRemoteAddress() == null ? null : request.getRemoteAddress().getAddress().getHostAddress();
        return new MarketingFormService.PublicSubmitCommand(
                req.response(),
                req.utm(),
                req.anonymousId(),
                req.idempotencyKey(),
                req.consentChannel(),
                req.consentStatus(),
                userAgent,
                hashText(ip));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private String hashText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    /**
     * FormDefinitionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record FormDefinitionReq(
            String publicKey,
            String name,
            String description,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            Boolean active,
            String createdBy) {
    }

    /**
     * StatusReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record StatusReq(Boolean active) {
    }

    /**
     * PublicSubmitReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record PublicSubmitReq(
            Map<String, Object> response,
            Map<String, Object> utm,
            String anonymousId,
            String idempotencyKey,
            String consentChannel,
            String consentStatus) {
    }
}
