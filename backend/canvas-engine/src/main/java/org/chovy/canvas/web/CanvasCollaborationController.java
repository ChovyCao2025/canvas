package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService;
import org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * CanvasCollaborationController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasCollaborationController {

    /**
     * summary服务，用于承接对应业务能力和领域编排。
     */
    private final CanvasCollaborationSummaryService summaryService;
    /**
     * preference服务，用于承接对应业务能力和领域编排。
     */
    private final UserWorkspacePreferenceService preferenceService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 查询画布协作汇总接口，对应 GET /{canvasId}/collaboration/summary。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError, summaryService.summary 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param canvasId 画布 ID。
     * @return 异步返回统一响应，包含查询画布协作汇总后的业务数据。
     */
    @GetMapping("/{canvasId}/collaboration/summary")
    public Mono<R<CanvasCollaborationSummaryService.Summary>> summary(@PathVariable Long canvasId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> summaryService.summary(context.tenantId(), canvasId))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }
    /**
     * 处理 画布协作 请求接口，对应 GET /preferences/editor。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError, preferenceService.getEditorPreference 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含处理 画布协作 请求后的业务数据。
     */
    @GetMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> editorPreference() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> preferenceService.getEditorPreference(
                                context.tenantId(), context.username()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }
    /**
     * 创建或更新 画布协作接口，对应 PUT /preferences/editor。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param patch 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含创建或更新 画布协作后的业务数据。
     */
    @PutMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> upsertEditorPreference(
            @RequestBody Map<String, Object> patch) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> upsert(context, patch))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param String string 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param patch patch 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private UserWorkspacePreferenceService.Preference upsert(TenantContext context, Map<String, Object> patch) {
        return preferenceService.upsertEditorPreference(context.tenantId(), context.username(), patch);
    }
}
