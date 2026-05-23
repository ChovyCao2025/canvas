package org.chovy.canvas.auth.controller;

import org.chovy.canvas.auth.domain.*;
import org.chovy.canvas.auth.dto.*;
import org.chovy.canvas.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 管理员用户管理接口。
 *
 * 边界约定：
 * 1) 仅 ADMIN 角色可访问（由安全配置与路由层统一拦截）；
 * 2) Controller 只负责 HTTP 入参与响应包装；
 * 3) 业务校验与持久化在 `SysUserService` 中处理。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    /** 用户服务：封装查询、创建、更新、禁用逻辑。 */
    private final SysUserService userService;

    /** 查询全部用户。 */
    @GetMapping
    public Mono<R<List<SysUser>>> list() {
        // MyBatis 查询为阻塞 IO，切到 boundedElastic 避免堵塞 Netty 事件线程
        return Mono.fromCallable(userService::listAll)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 创建用户（管理员接口）。 */
    @PostMapping
    public Mono<R<SysUser>> create(@RequestBody UserCreateReq req) {
        // 创建成功后直接返回新建用户（password 字段默认不出参）
        return Mono.fromCallable(() ->
                userService.create(req.getUsername(), req.getPassword(),
                        req.getDisplayName(), req.getRole()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    /** 更新用户展示名/密码/角色。 */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody UserUpdateReq req) {
        // 采用“部分更新”语义：未传字段保持原值
        return Mono.<Void>fromRunnable(() ->
                userService.update(id, req.getDisplayName(), req.getPassword(), req.getRole()))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    /** 禁用用户（enabled=0）。 */
    @PutMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> userService.disable(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
}
