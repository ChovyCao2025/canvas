package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceOwnershipView;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceTransferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * BiResourceTransferController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/resources")
public class BiResourceTransferController {

    private final TenantContextResolver tenantContextResolver;
    private final BiResourceTransferService transferService;

    /**
     * 创建 BiResourceTransferController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param transferService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiResourceTransferController(TenantContextResolver tenantContextResolver,
                                        BiResourceTransferService transferService) {
        this.tenantContextResolver = tenantContextResolver;
        this.transferService = transferService;
    }
    /**
     * 转移 BI 资源转移接口，对应 POST /transfer。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 transferService.transfer 完成业务处理。
     * 副作用：会变更资源归属。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含转移 BI 资源转移后的业务数据。
     */
    @PostMapping("/transfer")
    public Mono<R<BiResourceOwnershipView>> transfer(@RequestBody BiResourceTransferCommand command) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(transferService.transfer(
                                context.tenantId(),
                                context.username(),
                                command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 资源转移列表接口，对应 GET /ownerships。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 transferService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/ownerships")
    public Mono<R<List<BiResourceOwnershipView>>> list(
            @RequestParam(required = false) String resourceType) {
        return currentTenant()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(transferService.list(
                                context.tenantId(),
                                resourceType)))
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
