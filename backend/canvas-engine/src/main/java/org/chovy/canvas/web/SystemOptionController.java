package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 系统选项 HTTP 控制器，根路由为 {@code /admin/system-options}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/admin/system-options")
@RequiredArgsConstructor
public class SystemOptionController {

    /** 系统选项服务，用于管理全局配置项。 */
    private final SystemOptionService service;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping
    public Mono<R<PageResult<SystemOptionDO>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tenantId) {
        return requireAdminContext()
                .flatMap(context -> Mono.fromCallable(() -> {
                    Long effectiveTenantId = context.isSuperAdmin() ? tenantId : context.tenantId();
                    List<SystemOptionDO> rows = service.listForAdmin(
                            category, enabled, keyword, effectiveTenantId, context.isSuperAdmin());
                    return PageResult.of(rows.size(), rows);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok));
    }

    /**
     * 处理 update 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody SystemOptionDO body) {
        return requireAdminContext()
                .flatMap(context -> Mono.<Void>fromRunnable(() ->
                                service.updateEditable(id, body, context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    private Mono<TenantContext> requireAdminContext() {
        return tenantContextResolver.current()
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "无权限管理系统选项")));
    }
}
