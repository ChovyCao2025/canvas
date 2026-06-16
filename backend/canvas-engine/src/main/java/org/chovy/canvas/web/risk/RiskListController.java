package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.governance.RiskListCommand;
import org.chovy.canvas.domain.risk.governance.RiskListEntryCommand;
import org.chovy.canvas.domain.risk.governance.RiskListEntryView;
import org.chovy.canvas.domain.risk.governance.RiskListImportCommand;
import org.chovy.canvas.domain.risk.governance.RiskListImportResult;
import org.chovy.canvas.domain.risk.governance.RiskListService;
import org.chovy.canvas.domain.risk.governance.RiskListView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 租户级风控名单接口，提供名单创建、条目写入和批量导入能力。
 */
@RestController
@RequestMapping("/canvas/risk/lists")
public class RiskListController {

    /**
     * list服务，用于承接对应业务能力和领域编排。
     */
    private final RiskListService listService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建风控名单控制器。
     */
    @Autowired
    public RiskListController(RiskListService listService, TenantContextResolver tenantContextResolver) {
        this.listService = listService;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 创建风控名单。
     */
    @PostMapping
    public Mono<R<RiskListView>> createList(@RequestBody RiskListCommand command) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(listService.createList(context.tenantId(), command, actor(context)))));
    }

    /**
     * 查询当前租户风控名单。
     */
    @GetMapping
    public Mono<R<List<RiskListView>>> listLists() {
        return currentReader().flatMap(context -> blocking(() ->
                R.ok(listService.listLists(context.tenantId()))));
    }

    /**
     * 向指定名单添加条目。
     */
    @PostMapping("/{listKey}/entries")
    public Mono<R<RiskListEntryView>> addEntry(@PathVariable String listKey,
                                               @RequestBody RiskListEntryCommand command) {
        return currentWriter().flatMap(context -> blocking(() ->
                R.ok(listService.addEntry(context.tenantId(), listKey, command, actor(context)))));
    }

    /**
     * 查询指定名单当前生效条目。
     */
    @GetMapping("/{listKey}/entries")
    public Mono<R<List<RiskListEntryView>>> listEntries(@PathVariable String listKey) {
        return currentReader().flatMap(context -> blocking(() ->
                R.ok(listService.entries(context.tenantId(), listKey))));
    }

    /**
     * 删除指定名单条目。
     */
    @DeleteMapping("/{listKey}/entries/{entryId}")
    public Mono<R<Void>> removeEntry(@PathVariable String listKey,
                                     @PathVariable long entryId) {
        return currentWriter().flatMap(context -> blocking(() -> {
            listService.removeEntry(context.tenantId(), listKey, entryId, actor(context));
            return R.ok();
        }));
    }

    /**
     * 批量导入名单条目。
     */
    @PostMapping("/{listKey}/entries/import")
    public Mono<R<RiskListImportResult>> importEntries(@PathVariable String listKey,
                                                       @RequestBody RiskListImportCommand command) {
        // 批量导入可能显著改变在线决策结果，因此仅管理员可执行。
        return currentImporter().flatMap(context -> blocking(() ->
                R.ok(listService.importEntries(context.tenantId(), listKey, command, actor(context)))));
    }

    /**
     * 获取具备名单写权限的租户上下文。
     */
    private Mono<TenantContext> currentWriter() {
        return currentReader()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk list write permission required")));
    }

    /**
     * 获取具备名单读权限的租户上下文。
     */
    private Mono<TenantContext> currentReader() {
        return current().filter(context -> context.isSuperAdmin()
                        || context.isTenantAdmin()
                        || RoleNames.OPERATOR.equals(context.role()))
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk list read permission required")));
    }

    /**
     * 获取具备名单导入权限的租户上下文。
     */
    private Mono<TenantContext> currentImporter() {
        return current().filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk list import permission required")));
    }

    /**
     * 读取当前租户上下文，测试环境未注入解析器时回退系统管理员。
     */
    private Mono<TenantContext> current() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, RoleNames.ADMIN, "system"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("risk list permission required")));
    }

    /**
     * 在线程池执行阻塞型名单治理操作。
     */
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
        // 名单变更会写审计记录，不能在响应式事件循环上执行。
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从租户上下文获取审计操作者。
     */
    private String actor(TenantContext context) {
        return context.username() == null ? "system" : context.username();
    }
}
