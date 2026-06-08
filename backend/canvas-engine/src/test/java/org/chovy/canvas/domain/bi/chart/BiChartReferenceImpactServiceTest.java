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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiChartReferenceImpactServiceTest {

    @Test
    void impactAggregatesDashboardPortalAndSubscriptionReferences() {
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart());
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget(31L)));
        when(dashboardMapper.selectById(31L)).thenReturn(dashboard());
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu(41L)));
        when(portalMapper.selectById(41L)).thenReturn(portal());
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(subscription()));
        BiChartReferenceImpactService service = new BiChartReferenceImpactService(
                chartMapper,
                widgetMapper,
                dashboardMapper,
                menuMapper,
                portalMapper,
                subscriptionMapper,
                new ObjectMapper());

        BiChartReferenceImpact impact = service.impact(7L, "trend-executions");

        assertThat(impact.chartKey()).isEqualTo("trend-executions");
        assertThat(impact.chartName()).isEqualTo("Execution Trend");
        assertThat(impact.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(impact.dashboards()).singleElement().satisfies(reference -> {
            assertThat(reference.dashboardKey()).isEqualTo("canvas-effect");
            assertThat(reference.title()).isEqualTo("Canvas Effect");
            assertThat(reference.widgetKey()).isEqualTo("trend-widget");
            assertThat(reference.widgetTitle()).isEqualTo("Execution Trend Widget");
            assertThat(reference.status()).isEqualTo("PUBLISHED");
        });
        assertThat(impact.portals()).singleElement().satisfies(reference -> {
            assertThat(reference.portalKey()).isEqualTo("executive-home");
            assertThat(reference.name()).isEqualTo("Executive Home");
            assertThat(reference.menuKey()).isEqualTo("trend-menu");
            assertThat(reference.menuTitle()).isEqualTo("Trend Menu");
            assertThat(reference.status()).isEqualTo("DRAFT");
        });
        assertThat(impact.subscriptions()).singleElement().satisfies(reference -> {
            assertThat(reference.subscriptionKey()).isEqualTo("trend-daily");
            assertThat(reference.name()).isEqualTo("Trend Daily");
            assertThat(reference.enabled()).isTrue();
        });
    }

    private BiChartDO chart() {
        BiChartDO row = new BiChartDO();
        row.setId(21L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setChartKey("trend-executions");
        row.setName("Execution Trend");
        row.setQueryJson("""
                {"datasetKey":"canvas_daily_stats","dimensions":["stat_date"],"metrics":["total_executions"],"filters":[],"sorts":[],"limit":500}
                """);
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiDashboardWidgetDO widget(Long dashboardId) {
        BiDashboardWidgetDO row = new BiDashboardWidgetDO();
        row.setId(32L);
        row.setTenantId(7L);
        row.setDashboardId(dashboardId);
        row.setChartId(21L);
        row.setWidgetKey("trend-widget");
        row.setTitle("Execution Trend Widget");
        return row;
    }

    private BiDashboardDO dashboard() {
        BiDashboardDO row = new BiDashboardDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDashboardKey("canvas-effect");
        row.setName("Canvas Effect");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiPortalMenuDO menu(Long portalId) {
        BiPortalMenuDO row = new BiPortalMenuDO();
        row.setId(42L);
        row.setTenantId(7L);
        row.setPortalId(portalId);
        row.setMenuKey("trend-menu");
        row.setTitle("Trend Menu");
        row.setResourceType("CHART");
        row.setResourceId(21L);
        return row;
    }

    private BiPortalDO portal() {
        BiPortalDO row = new BiPortalDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setPortalKey("executive-home");
        row.setName("Executive Home");
        row.setStatus("DRAFT");
        return row;
    }

    private BiSubscriptionDO subscription() {
        BiSubscriptionDO row = new BiSubscriptionDO();
        row.setId(51L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSubscriptionKey("trend-daily");
        row.setName("Trend Daily");
        row.setResourceType("CHART");
        row.setResourceId(21L);
        row.setEnabled(true);
        return row;
    }
}
