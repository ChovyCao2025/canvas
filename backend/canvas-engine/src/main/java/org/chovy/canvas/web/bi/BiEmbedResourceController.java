package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketPayload;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.portal.BiPortalResource;
import org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Returns BI embed resources only after a ticket has been checked against resource scope and request origin.
 *
 * <p>The first verification reads ticket metadata for scope checks; the second binds actual use to the
 * Origin/Referer value before resource data is loaded.</p>
 */
@RestController
@RequestMapping("/canvas/bi/embed/resources")
public class BiEmbedResourceController {

    private final BiEmbedTicketService embedTicketService;
    private final BiDashboardResourceService dashboardResourceService;
    private final BiDashboardRuntimeStateService runtimeStateService;
    private final BiPortalRuntimeService portalRuntimeService;

    /**
     * 执行 BiEmbedResourceController 流程，围绕 bi embed resource controller 完成校验、计算或结果组装。
     *
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiEmbedResourceController(BiEmbedTicketService embedTicketService,
                                     BiDashboardResourceService dashboardResourceService) {
        this(embedTicketService, dashboardResourceService, null, null);
    }

    /**
     * 执行 BiEmbedResourceController 流程，围绕 bi embed resource controller 完成校验、计算或结果组装。
     *
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeStateService 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiEmbedResourceController(BiEmbedTicketService embedTicketService,
                                     BiDashboardResourceService dashboardResourceService,
                                     BiDashboardRuntimeStateService runtimeStateService) {
        this(embedTicketService, dashboardResourceService, runtimeStateService, null);
    }

    /**
     * 执行 BiEmbedResourceController 流程，围绕 bi embed resource controller 完成校验、计算或结果组装。
     *
     * @param embedTicketService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeStateService 时间参数，用于计算窗口、过期或审计时间。
     * @param portalRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public BiEmbedResourceController(BiEmbedTicketService embedTicketService,
                                     BiDashboardResourceService dashboardResourceService,
                                     BiDashboardRuntimeStateService runtimeStateService,
                                     BiPortalRuntimeService portalRuntimeService) {
        this.embedTicketService = embedTicketService;
        this.dashboardResourceService = dashboardResourceService;
        this.runtimeStateService = runtimeStateService;
        this.portalRuntimeService = portalRuntimeService;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param origin origin 参数，用于 getDashboardResource 流程中的校验、计算或对象转换。
     * @param referer referer 参数，用于 getDashboardResource 流程中的校验、计算或对象转换。
     * @return 返回 getDashboardResource 流程生成的业务结果。
     */
    @PostMapping("/dashboard")
    public Mono<R<BiDashboardResource>> getDashboardResource(
            @RequestBody EmbedDashboardResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null) {
                        throw new IllegalArgumentException("embed dashboard resource request is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforceDashboardOrPortalMenuDashboardResourceScope(preview, request);
                    // Check scope before and after origin verification so tickets cannot be replayed
                    // for another resource.
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforceDashboardOrPortalMenuDashboardResourceScope(payload, request);
                    return R.ok(dashboardResourceService.get(payload.tenantId(), request.resourceKey()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param origin origin 参数，用于 getDashboardRuntimeState 流程中的校验、计算或对象转换。
     * @param referer referer 参数，用于 getDashboardRuntimeState 流程中的校验、计算或对象转换。
     * @return 返回 getDashboardRuntimeState 流程生成的业务结果。
     */
    @PostMapping("/dashboard/runtime-state")
    public Mono<R<BiDashboardRuntimeStateView>> getDashboardRuntimeState(
            @RequestBody EmbedDashboardResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null) {
                        throw new IllegalArgumentException("embed dashboard resource request is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforceDashboardOrPortalMenuDashboardResourceScope(preview, request);
                    // Runtime state shares the same ticket/resource binding as the dashboard definition endpoint.
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforceDashboardOrPortalMenuDashboardResourceScope(payload, request);
                    return R.ok(requireRuntimeStateService().get(
                            payload.tenantId(),
                            payload.username(),
                            request.resourceKey()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param origin origin 参数，用于 getPortalResource 流程中的校验、计算或对象转换。
     * @param referer referer 参数，用于 getPortalResource 流程中的校验、计算或对象转换。
     * @return 返回 getPortalResource 流程生成的业务结果。
     */
    @PostMapping("/portal")
    public Mono<R<BiPortalResource>> getPortalResource(
            @RequestBody EmbedDashboardResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null) {
                        throw new IllegalArgumentException("embed portal resource request is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforcePortalResourceScope(preview, request);
                    // Published portal reads carry the ticket username into BI query context.
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforcePortalResourceScope(payload, request);
                    return R.ok(requirePortalRuntimeService().getPublished(
                            payload.tenantId(),
                            payload.resourceKey(),
                            new BiQueryContext(payload.tenantId(), payload.username())));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 enforceDashboardResourceScope 流程，围绕 enforce dashboard resource scope 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void enforceDashboardResourceScope(BiEmbedTicketPayload payload, EmbedDashboardResourceRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if (!"DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed resource only supports dashboard tickets");
        }
        if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                || !payload.resourceKey().equals(request.resourceKey())) {
            throw new SecurityException("BI embed dashboard resource does not match ticket");
        }
    }

    /**
     * 执行 enforceDashboardOrPortalMenuDashboardResourceScope 流程，围绕 enforce dashboard or portal menu dashboard resource scope 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void enforceDashboardOrPortalMenuDashboardResourceScope(BiEmbedTicketPayload payload,
                                                                    EmbedDashboardResourceRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if ("DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            enforceDashboardResourceScope(payload, request);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (!"PORTAL".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed resource only supports dashboard or portal tickets");
        }
        if (!"DASHBOARD".equalsIgnoreCase(request.resourceType())) {
            throw new SecurityException("BI portal ticket can only open dashboard menu resources");
        }
        if (!portalContainsDashboard(payload, request.resourceKey())) {
            throw new SecurityException("BI embed dashboard resource is not declared in portal menu");
        }
    }

    /**
     * 执行 enforcePortalResourceScope 流程，围绕 enforce portal resource scope 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void enforcePortalResourceScope(BiEmbedTicketPayload payload, EmbedDashboardResourceRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if (!"PORTAL".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed resource only supports portal tickets");
        }
        if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                || !payload.resourceKey().equals(request.resourceKey())) {
            throw new SecurityException("BI embed portal resource does not match ticket");
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param left left 参数，用于 equalsIgnoreCase 流程中的校验、计算或对象转换。
     * @param right right 参数，用于 equalsIgnoreCase 流程中的校验、计算或对象转换。
     * @return 返回 equals ignore case 的布尔判断结果。
     */
    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    /**
     * 执行 portalContainsDashboard 流程，围绕 portal contains dashboard 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 portal contains dashboard 的布尔判断结果。
     */
    private boolean portalContainsDashboard(BiEmbedTicketPayload payload, String dashboardKey) {
        BiPortalResource portal = requirePortalRuntimeService().getPublished(
                payload.tenantId(),
                payload.resourceKey(),
                new BiQueryContext(payload.tenantId(), payload.username()));
        return portal.menus().stream()
                .anyMatch(menu -> isDashboardMenu(menu, dashboardKey));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param menu menu 参数，用于 isDashboardMenu 流程中的校验、计算或对象转换。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private boolean isDashboardMenu(BiPortalMenuResource menu, String dashboardKey) {
        return menu != null
                && "DASHBOARD".equalsIgnoreCase(menu.resourceType())
                && dashboardKey != null
                && dashboardKey.equals(menu.resourceKey());
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireRuntimeStateService 流程生成的业务结果。
     */
    private BiDashboardRuntimeStateService requireRuntimeStateService() {
        if (runtimeStateService == null) {
            throw new IllegalStateException("BI dashboard runtime state service is required");
        }
        return runtimeStateService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requirePortalRuntimeService 流程生成的业务结果。
     */
    private BiPortalRuntimeService requirePortalRuntimeService() {
        if (portalRuntimeService == null) {
            throw new IllegalStateException("BI portal runtime service is required");
        }
        return portalRuntimeService;
    }

    /**
     * EmbedDashboardResourceRequest 数据记录。
     */
    public record EmbedDashboardResourceRequest(
            String ticket,
            String resourceType,
            String resourceKey
    ) {
    }
}
