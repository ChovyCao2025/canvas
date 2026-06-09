package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.ExecutionRerunAuditDO;
import org.chovy.canvas.domain.canvas.TestUserRerunService;
import org.chovy.canvas.domain.canvas.TestUserRerunService.RerunRequest;
import org.chovy.canvas.domain.canvas.TestUserRerunService.RerunResult;
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

/**
 * ExecutionRerunController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/execution-reruns")
public class ExecutionRerunController {

    private final TestUserRerunService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 ExecutionRerunController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ExecutionRerunController(TestUserRerunService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 触发执行重跑运行接口，对应 POST /canvas/{canvasId}。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会触发一次运行流程。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param canvasId 画布 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含触发执行重跑运行后的业务数据。
     */
    @PostMapping("/canvas/{canvasId}")
    public Mono<R<RerunResult>> rerun(@PathVariable Long canvasId, @RequestBody RerunRequest req) {
        return current().flatMap(context -> service.rerun(tenantId(context), context, canvasId, req).map(R::ok));
    }
    /**
     * 查询执行重跑状态接口，对应 GET /{id}。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含查询执行重跑状态后的业务数据。
     */
    @GetMapping("/{id}")
    public Mono<R<ExecutionRerunAuditDO>> status(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.audit(tenantId(context), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询执行重跑审计接口，对应 GET 请求。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param canvasId 画布 ID，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<ExecutionRerunAuditDO>>> audits(@RequestParam(required = false) Long canvasId) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.audits(tenantId(context), canvasId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current 流程生成的业务结果。
     */
    private Mono<TenantContext> current() {
        return tenantContextResolver.current().defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }
}
