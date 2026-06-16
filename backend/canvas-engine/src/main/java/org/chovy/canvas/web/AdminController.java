package org.chovy.canvas.web;

import org.chovy.canvas.auth.domain.*;
import org.chovy.canvas.auth.dto.*;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import org.chovy.canvas.dal.dataobject.SysUserDO;

/**
 * 管理员用户管理接口。
 *
 * 边界约定：
 * 1) 由安全配置与租户上下文共同限制管理员访问；
 * 2) Controller 只负责 HTTP 入参与响应包装；
 * 3) 业务校验与持久化在 `SysUserService` 中处理。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    /** 用户服务：封装查询、创建、更新、禁用逻辑。 */
    private final SysUserService userService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /** 查询全部用户。 */
    @GetMapping
    public Mono<R<List<SysUserDO>>> list() {
        // MyBatis 查询为阻塞 IO，切到 boundedElastic 避免堵塞 Netty 事件线程
        return requireUserAdmin()
                .flatMap(context -> Mono.fromCallable(() -> userService.listVisible(context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok)
                        .onErrorMap(AccessDeniedException.class, this::forbidden));
    }

    /** 创建用户（管理员接口）。 */
    @PostMapping
    public Mono<R<SysUserDO>> create(@RequestBody UserCreateReq req) {
        // 创建成功后直接返回新建用户（password 字段默认不出参）
        return requireUserAdmin()
                .flatMap(context -> Mono.fromCallable(() ->
                                userService.create(req.getUsername(), req.getPassword(),
                                        req.getDisplayName(), req.getRole(), req.getTenantId(), context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok)
                        .onErrorMap(AccessDeniedException.class, this::forbidden));
    }

    /** 更新用户展示名/密码/角色。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody UserUpdateReq req) {
        // 采用“部分更新”语义：未传字段保持原值
        return requireUserAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() ->
                                userService.update(id, req.getDisplayName(), req.getPassword(),
                                        req.getRole(), context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok())
                        .onErrorMap(AccessDeniedException.class, this::forbidden));
    }

    /** 禁用用户（enabled=0）。 */
    @PutMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return requireUserAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() -> userService.disable(id, context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok())
                        .onErrorMap(AccessDeniedException.class, this::forbidden));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 requireUserAdmin 流程生成的业务结果。
     */
    private Mono<TenantContext> requireUserAdmin() {
        return tenantContextResolver.current()
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "无权限管理用户")));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param e e 参数，用于 forbidden 流程中的校验、计算或对象转换。
     * @return 返回 forbidden 流程生成的业务结果。
     */
    private ResponseStatusException forbidden(AccessDeniedException e) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    }
}
