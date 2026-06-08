package org.chovy.canvas.domain.bi.portal;

import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BiPortalRuntimeService 编排 domain.bi.portal 场景的领域业务规则。
 */
@Service
public class BiPortalRuntimeService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final BiPortalResourceService portalResourceService;
    private final BiPermissionService permissionService;

    /**
     * 创建 BiPortalRuntimeService 实例并注入 domain.bi.portal 场景依赖。
     * @param portalResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalRuntimeService(BiPortalResourceService portalResourceService,
                                  BiPermissionService permissionService) {
        this.portalResourceService = portalResourceService;
        this.permissionService = permissionService;
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 符合条件的业务列表
     */
    public List<BiPortalResource> listPublished(Long tenantId, BiQueryContext context) {
        return portalResourceService.list(tenantId).stream()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .filter(this::published)
                .map(portal -> withVisibleMenus(portal, context))
                .toList();
    }

    /**
     * 读取已发布的 BI 门户资源，并结合查询上下文执行运行态可见性和权限裁剪。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param portalKey 门户业务键，用于定位门户草稿、发布版本和菜单配置
     * @param context BI 查询上下文，包含租户、用户、角色和权限治理信息
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiPortalResource getPublished(Long tenantId, String portalKey, BiQueryContext context) {
        BiPortalResource portal = portalResourceService.get(tenantId, portalKey);
        if (!published(portal)) {
            throw new IllegalArgumentException("BI portal is not published: " + portalKey);
        }
        return withVisibleMenus(portal, context);
    }

    /**
     * 执行 withVisibleMenus 流程，围绕 with visible menus 完成校验、计算或结果组装。
     *
     * @param portal portal 参数，用于 withVisibleMenus 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 withVisibleMenus 流程生成的业务结果。
     */
    private BiPortalResource withVisibleMenus(BiPortalResource portal, BiQueryContext context) {
        return new BiPortalResource(
                portal.portalKey(),
                portal.name(),
                portal.theme(),
                permissionService.visibleMenus(portal.menus(), context),
                portal.status(),
                portal.source());
    }

    /**
     * 判断门户资源是否处于可运行态发布状态。
     *
     * <p>运行态接口只暴露 PUBLISHED 门户，草稿、归档或待审批版本都不会进入嵌入/门户菜单渲染。</p>
     */
    private boolean published(BiPortalResource portal) {
        return portal != null && STATUS_PUBLISHED.equals(portal.status());
    }
}
