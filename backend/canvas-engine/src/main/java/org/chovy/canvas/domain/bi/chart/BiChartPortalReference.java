package org.chovy.canvas.domain.bi.chart;

/**
 * 描述向门户用户暴露 BI 图表的门户菜单项。
 *
 * @param portalKey 菜单项所属门户的稳定标识
 * @param name 影响提示中展示的门户名称
 * @param menuKey 引用图表的菜单项稳定标识
 * @param menuTitle 门户导航中展示的菜单标题
 * @param status 门户或菜单引用的生命周期状态
 */
public record BiChartPortalReference(
        String portalKey,
        String name,
        String menuKey,
        String menuTitle,
        String status
) {
}
