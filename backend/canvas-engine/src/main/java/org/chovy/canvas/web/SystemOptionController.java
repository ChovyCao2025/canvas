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

@RestController
@RequestMapping("/admin/system-options")
@RequiredArgsConstructor
public class SystemOptionController {

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
