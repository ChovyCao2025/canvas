package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiPromptEvaluationService;
import org.chovy.canvas.domain.ai.AiPromptEvaluationService.EvaluationAuditEvent;
import org.chovy.canvas.domain.ai.AiPromptEvaluationService.EvaluationRequest;
import org.chovy.canvas.domain.ai.AiPromptEvaluationService.EvaluationResult;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiPromptTemplateService.RenderRequest;
import org.chovy.canvas.domain.ai.AiPromptTemplateService.RenderResult;
import org.chovy.canvas.domain.ai.AiPromptTemplateService.TemplateCreateRequest;
import org.chovy.canvas.domain.ai.AiPromptTemplateService.TemplateDetail;
import org.chovy.canvas.domain.ai.AiPromptTemplateService.TemplateSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/ai/prompt-templates")
/**
 * AiPromptTemplateController 提供对应业务域的 HTTP 接口入口。
 */
public class AiPromptTemplateController {

    /**
     * 模板服务，用于承接对应业务能力和领域编排。
     */
    private final AiPromptTemplateService templateService;
    /**
     * evaluation服务，用于承接对应业务能力和领域编排。
     */
    private final AiPromptEvaluationService evaluationService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    public AiPromptTemplateController(AiPromptTemplateService templateService,
                                      AiPromptEvaluationService evaluationService) {
        this(templateService, evaluationService, null);
    }

    @Autowired
    public AiPromptTemplateController(AiPromptTemplateService templateService,
                                      AiPromptEvaluationService evaluationService,
                                      TenantContextResolver tenantContextResolver) {
        this.templateService = templateService;
        this.evaluationService = evaluationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<TemplateSummary>>> list() {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(templateService.listTemplates(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    /**
     * 创建对应资源并返回处理结果。
     *
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<TemplateDetail>> create(@RequestBody TemplateCreateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(templateService.createTemplate(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    /**
     * 查询并返回对应资源或视图。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<TemplateDetail>> detail(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(templateService.getTemplate(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/{id}")
    /**
     * 更新对应资源并返回处理结果。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<TemplateDetail>> update(@PathVariable Long id, @RequestBody TemplateCreateRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(templateService.updateTemplate(tenantId, id, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/disable")
    /**
     * 变更或删除对应资源并返回处理结果。
     *
     * @param id 标识，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            templateService.disableTemplate(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/render")
    /**
     * 执行 render 对应的接口或辅助流程。
     *
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<RenderResult>> render(@RequestBody RenderRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(templateService.render(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/evaluate")
    /**
     * 执行 evaluate 对应的接口或辅助流程。
     *
     * @param req req，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    public Mono<R<EvaluationResult>> evaluate(@RequestBody EvaluationRequest req) {
        return currentManagerTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(evaluationService.evaluate(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/evaluation-audits")
    /**
     * 查询并返回对应资源或视图。
     * @return 返回处理后的业务结果
     */
    public Mono<R<List<EvaluationAuditEvent>>> audits() {
        return Mono.fromCallable(() -> R.ok(evaluationService.recentAudits()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 解析并校验当前请求上下文。
     * @return 返回处理后的业务结果
     */
    private Mono<Long> currentManagerTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限管理 AI 提示词模板")))
                .filter(this::canManageAi)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限管理 AI 提示词模板")))
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    /**
     * 判断当前上下文是否满足业务条件。
     *
     * @param context 上下文，由调用方或当前请求上下文提供
     * @return 返回处理后的业务结果
     */
    private boolean canManageAi(TenantContext context) {
        return context != null && (context.isSuperAdmin() || context.isTenantAdmin());
    }
}
