package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteService;
import org.chovy.canvas.domain.bi.resource.BiResourceFavoriteView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * BiResourceFavoriteController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/resources")
public class BiResourceFavoriteController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * favorite服务，用于承接对应业务能力和领域编排。
     */
    private final BiResourceFavoriteService favoriteService;

    /**
     * 创建 BiResourceFavoriteController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param favoriteService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiResourceFavoriteController(TenantContextResolver tenantContextResolver,
                                        BiResourceFavoriteService favoriteService) {
        this.tenantContextResolver = tenantContextResolver;
        this.favoriteService = favoriteService;
    }
    /**
     * 切换 BI 资源收藏 收藏接口，对应 POST /favorites。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 favoriteService.favorite 完成业务处理。
     * 副作用：会变更收藏关系。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含切换 BI 资源收藏 收藏后的业务数据。
     */
    @PostMapping("/favorites")
    public Mono<R<BiResourceFavoriteView>> favorite(@RequestBody BiResourceFavoriteCommand command) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(favoriteService.favorite(
                                context.tenantId(),
                                context.username(),
                                command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 资源收藏列表接口，对应 GET /favorites。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 favoriteService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/favorites")
    public Mono<R<List<BiResourceFavoriteView>>> list(
            @RequestParam(required = false) String resourceType) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(favoriteService.list(
                                context.tenantId(),
                                context.username(),
                                resourceType)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 切换 BI 资源收藏 收藏接口，对应 DELETE /favorites/{resourceType}/{resourceKey}。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 favoriteService.unfavorite 完成业务处理。
     * 副作用：会变更收藏关系。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 路径参数。
     * @param resourceKey resource 唯一键。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/favorites/{resourceType}/{resourceKey}")
    public Mono<R<Void>> unfavorite(
            @PathVariable String resourceType,
            @PathVariable String resourceKey) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> {
                            favoriteService.unfavorite(
                                    context.tenantId(),
                                    context.username(),
                                    resourceType,
                                    resourceKey);
                            return R.ok();
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }
}
