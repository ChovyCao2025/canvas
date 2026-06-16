package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.cdp.CdpUserDirectoryService;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserInsightService;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CanvasUserDetailDTO;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagHistoryDTO;
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
 * CDP 用户 HTTP 控制器，根路由为 {@code /cdp/users}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/cdp/users")
@RequiredArgsConstructor
public class CdpUserController {

    /** CDP 用户目录服务，用于查询用户列表。 */
    private final CdpUserDirectoryService directoryService;
    /** CDP 用户洞察服务，用于查询用户洞察数据。 */
    private final CdpUserInsightService insightService;
    /** CDP 用户服务，用于维护用户基础资料。 */
    private final CdpUserService userService;
    /** CDP 标签服务，用于查询用户标签。 */
    private final CdpTagService tagService;
    /** 租户上下文解析器，用于隔离 CDP 用户和标签数据。 */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 查询 CDP 用户列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError, directoryService.listUsers 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param keyword 搜索关键字，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(directoryService.listUsers(tenantId(context), keyword)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 get 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param userId userId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{userId}")
    public Mono<R<CdpUserDetailDTO>> get(@PathVariable String userId) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(userService.toDetail(userService.getRequiredProfile(tenantId(context), userId))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 get Insight 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param userId userId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{userId}/insight")
    public Mono<R<CanvasUserDetailDTO>> getInsight(@PathVariable String userId) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(insightService.getUserInsight(tenantId(context), userId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 list Tags 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param userId userId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{userId}/tags")
    public Mono<R<List<CdpUserTagDTO>>> listTags(@PathVariable String userId) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(tagService.listCurrentTags(tenantId(context), userId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 list Tag History 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param userId userId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{userId}/tag-history")
    public Mono<R<List<CdpUserTagHistoryDTO>>> listTagHistory(@PathVariable String userId) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> R.ok(tagService.listHistory(tenantId(context), userId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 add Tag 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param userId userId 对应的业务主键或标识
     * @param req 请求对象，承载调用方提交的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/{userId}/tags")
    public Mono<R<Void>> addTag(@PathVariable String userId, @RequestBody CdpTagWriteReq req) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> {
                    tagService.setTag(tenantId(context), userId, req);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * removeTag 删除或清理 web 场景的业务数据。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 removeTag 流程生成的业务结果。
     */
    @DeleteMapping("/{userId}/tags/{tagCode}")
    public Mono<R<Void>> removeTag(@PathVariable String userId, @PathVariable String tagCode) {
        return tenantContextResolver.currentOrError().flatMap(context ->
                Mono.fromCallable(() -> {
                    tagService.removeTag(tenantId(context), userId, tagCode, "用户详情移除标签", null);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        if (context.tenantId() == null && RoleNames.ADMIN.equals(context.role())) {
            return null;
        }
        if (context.tenantId() == null) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param context" context"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return context.tenantId();
    }
}
