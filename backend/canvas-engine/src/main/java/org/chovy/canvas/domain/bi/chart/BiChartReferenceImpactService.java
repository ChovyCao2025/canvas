package org.chovy.canvas.domain.bi.chart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiPortalMenuDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDashboardWidgetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiPortalMenuMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * BiChartReferenceImpactService 编排 domain.bi.chart 场景的领域业务规则。
 */
@Service
public class BiChartReferenceImpactService {

    private static final String RESOURCE_CHART = "CHART";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final BiChartMapper chartMapper;
    private final BiDashboardWidgetMapper widgetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiPortalMenuMapper menuMapper;
    private final BiPortalMapper portalMapper;
    private final BiSubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 BiChartReferenceImpactService 实例并注入 domain.bi.chart 场景依赖。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartReferenceImpactService(BiChartMapper chartMapper,
                                         BiDashboardWidgetMapper widgetMapper,
                                         BiDashboardMapper dashboardMapper,
                                         BiPortalMenuMapper menuMapper,
                                         BiPortalMapper portalMapper,
                                         BiSubscriptionMapper subscriptionMapper,
                                         ObjectMapper objectMapper) {
        this.chartMapper = chartMapper;
        this.widgetMapper = widgetMapper;
        this.dashboardMapper = dashboardMapper;
        this.menuMapper = menuMapper;
        this.portalMapper = portalMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分析指定图表被仪表盘、门户菜单和订阅引用的影响范围。
     *
     * <p>该方法只读图表及引用关系表，不写数据库、不触发缓存刷新或外部调用。归档图表会被视为不存在；
     * 返回结果包含图表基本信息、查询使用的数据集 key，以及仍处于非归档资源中的引用列表。</p>
     *
     * @param tenantId 租户 ID，空值按系统租户处理
     * @param chartKey 图表业务 key
     * @return 图表引用影响分析结果
     */
    public BiChartReferenceImpact impact(Long tenantId, String chartKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiChartDO chart = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                .eq(BiChartDO::getTenantId, scopedTenantId)
                .eq(BiChartDO::getChartKey, required(chartKey, "chartKey"))
                .ne(BiChartDO::getStatus, STATUS_ARCHIVED)
                .last("LIMIT 1"));
        if (chart == null || chart.getId() == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }

        return new BiChartReferenceImpact(
                chart.getChartKey(),
                chart.getName(),
                datasetKey(chart),
                dashboardReferences(scopedTenantId, chart.getId()),
                portalReferences(scopedTenantId, chart.getId()),
                subscriptionReferences(scopedTenantId, chart.getId()));
    }

    /**
     * 收集图表所在的仪表盘组件引用，过滤已归档或不存在的仪表盘。
     */
    private List<BiChartDashboardReference> dashboardReferences(Long tenantId, Long chartId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(widgetMapper.selectList(new LambdaQueryWrapper<BiDashboardWidgetDO>()
                        .eq(BiDashboardWidgetDO::getTenantId, tenantId)
                        .eq(BiDashboardWidgetDO::getChartId, chartId)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(widget -> {
                    BiDashboardDO dashboard = dashboardMapper.selectById(widget.getDashboardId());
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (dashboard == null || isArchived(dashboard.getStatus())) {
                        return null;
                    }
                    return new BiChartDashboardReference(
                            dashboard.getDashboardKey(),
                            dashboard.getName(),
                            widget.getWidgetKey(),
                            widget.getTitle(),
                            dashboard.getStatus());
                })
                .filter(reference -> reference != null)
                .sorted(Comparator.comparing(BiChartDashboardReference::dashboardKey)
                        .thenComparing(BiChartDashboardReference::widgetKey))
                .toList();
    }

    /**
     * 收集门户菜单中指向该图表的引用，过滤已归档或不存在的门户。
     */
    private List<BiChartPortalReference> portalReferences(Long tenantId, Long chartId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(menuMapper.selectList(new LambdaQueryWrapper<BiPortalMenuDO>()
                        .eq(BiPortalMenuDO::getTenantId, tenantId)
                        .eq(BiPortalMenuDO::getResourceType, RESOURCE_CHART)
                        .eq(BiPortalMenuDO::getResourceId, chartId)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(menu -> {
                    BiPortalDO portal = portalMapper.selectById(menu.getPortalId());
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (portal == null || isArchived(portal.getStatus())) {
                        return null;
                    }
                    return new BiChartPortalReference(
                            portal.getPortalKey(),
                            portal.getName(),
                            menu.getMenuKey(),
                            menu.getTitle(),
                            portal.getStatus());
                })
                .filter(reference -> reference != null)
                .sorted(Comparator.comparing(BiChartPortalReference::portalKey)
                        .thenComparing(BiChartPortalReference::menuKey))
                .toList();
    }

    /**
     * 收集订阅任务中直接订阅该图表的引用，用于删除或归档前提示投递影响。
     */
    private List<BiChartSubscriptionReference> subscriptionReferences(Long tenantId, Long chartId) {
        return safeList(subscriptionMapper.selectList(new LambdaQueryWrapper<BiSubscriptionDO>()
                        .eq(BiSubscriptionDO::getTenantId, tenantId)
                        .eq(BiSubscriptionDO::getResourceType, RESOURCE_CHART)
                        .eq(BiSubscriptionDO::getResourceId, chartId)))
                .stream()
                .map(row -> new BiChartSubscriptionReference(
                        row.getSubscriptionKey(),
                        row.getName(),
                        row.getEnabled()))
                .sorted(Comparator.comparing(BiChartSubscriptionReference::subscriptionKey))
                .toList();
    }

    /**
     * 从图表查询 JSON 中解析数据集 key；查询结构损坏时阻断影响分析并暴露配置错误。
     */
    private String datasetKey(BiChartDO chart) {
        try {
            BiQueryRequest query = objectMapper.readValue(chart.getQueryJson(), BiQueryRequest.class);
            return query.datasetKey();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI chart query payload", e);
        }
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isArchived(String status) {
        return STATUS_ARCHIVED.equalsIgnoreCase(status == null ? "" : status);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
