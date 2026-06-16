package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.TestUserDO;
import org.chovy.canvas.dal.dataobject.TestUserSetDO;
import org.chovy.canvas.domain.canvas.TestUserRerunService;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserCreateReq;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserPreview;
import org.chovy.canvas.domain.canvas.TestUserRerunService.TestUserSetCreateReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * TestUserController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/test-users")
public class TestUserController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final TestUserRerunService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 TestUserController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public TestUserController(TestUserRerunService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 处理 测试用户 请求接口，对应 GET /sets。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/sets")
    public Mono<R<List<TestUserSetDO>>> sets() {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.listSets(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 测试用户接口，对应 POST /sets。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建 测试用户后的业务数据。
     */
    @PostMapping("/sets")
    public Mono<R<TestUserSetDO>> createSet(@RequestBody TestUserSetCreateReq req) {
        return current().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.createSet(tenantId(context), req, context.username())))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 测试用户 请求接口，对应 GET /sets/{setId}/users。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param setId set ID。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/sets/{setId}/users")
    public Mono<R<List<TestUserDO>>> users(@PathVariable Long setId) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.listUsers(tenantId(context), setId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建 测试用户接口，对应 POST /sets/{setId}/users。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会写入新记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param setId set ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含创建 测试用户后的业务数据。
     */
    @PostMapping("/sets/{setId}/users")
    public Mono<R<TestUserDO>> createUser(@PathVariable Long setId, @RequestBody TestUserCreateReq req) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.createUser(tenantId(context), setId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 获取测试用户详情接口，对应 GET /{id}。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取测试用户详情后的业务数据。
     */
    @GetMapping("/{id}")
    public Mono<R<TestUserDO>> detail(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.getUser(tenantId(context), id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 预览测试用户结果接口，对应 GET /{id}/preview。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含预览测试用户结果后的业务数据。
     */
    @GetMapping("/{id}/preview")
    public Mono<R<TestUserPreview>> preview(@PathVariable Long id) {
        return current().flatMap(context -> Mono.fromCallable(() -> R.ok(service.preview(tenantId(context), id)))
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
