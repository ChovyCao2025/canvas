package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiSubscriptionAdminServiceTest {

    @Test
    void upsertSubscriptionPersistsScheduleAndEnforcesSubscribePermission() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard());
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, subscriptionRow());
        BiSubscriptionAdminService service = service(subscriptionMapper, alertRuleMapper, datasetMapper, metricMapper,
                dashboardMapper, chartMapper, portalMapper, permissionService);

        BiSubscriptionView view = service.upsertSubscription(7L, "alice", "OPERATOR", subscriptionCommand());

        ArgumentCaptor<BiSubscriptionDO> captor = ArgumentCaptor.forClass(BiSubscriptionDO.class);
        verify(subscriptionMapper).insert(captor.capture());
        verify(permissionService).enforceResourceAccess(
                7L,
                5L,
                "DASHBOARD",
                21L,
                new BiQueryContext(7L, "alice", "OPERATOR"),
                BiPermissionService.ACTION_SUBSCRIBE);
        assertThat(captor.getValue().getSubscriptionKey()).isEqualTo("canvas-daily");
        assertThat(captor.getValue().getScheduleJson()).contains("\"frequency\":\"DAILY\"");
        assertThat(captor.getValue().getReceiverJson()).contains("\"EMAIL\"");
        assertThat(view.subscriptionKey()).isEqualTo("canvas-daily");
        assertThat(view.resourceType()).isEqualTo("DASHBOARD");
    }

    @Test
    void upsertSubscriptionRejectsMissingReceiverChannels() {
        BiSubscriptionAdminService service = service(
                mock(BiSubscriptionMapper.class),
                mock(BiAlertRuleMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiMetricMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                null);

        BiSubscriptionCommand command = new BiSubscriptionCommand(
                "bad-subscription",
                "Bad",
                "DASHBOARD",
                "canvas-effect",
                null,
                Map.of("frequency", "DAILY"),
                Map.of("users", List.of("alice")),
                Map.of(),
                true);

        assertThatThrownBy(() -> service.upsertSubscription(7L, "alice", "OPERATOR", command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsertAlertPersistsMetricCondition() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        when(metricMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metric());
        when(alertRuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, alertRow());
        BiSubscriptionAdminService service = service(subscriptionMapper, alertRuleMapper, datasetMapper, metricMapper,
                dashboardMapper, chartMapper, portalMapper, null);

        BiAlertRuleView view = service.upsertAlert(7L, "alice", "OPERATOR", alertCommand());

        ArgumentCaptor<BiAlertRuleDO> captor = ArgumentCaptor.forClass(BiAlertRuleDO.class);
        verify(alertRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getAlertKey()).isEqualTo("success-rate-alert");
        assertThat(captor.getValue().getMetricKey()).isEqualTo("success_rate");
        assertThat(captor.getValue().getConditionJson()).contains("\"threshold\":0.9");
        assertThat(view.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(view.metricKey()).isEqualTo("success_rate");
    }

    @Test
    void upsertAlertAcceptsAnomalyConditionWithoutStaticThreshold() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        when(metricMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metric());
        BiAlertRuleDO persisted = alertRow();
        persisted.setConditionJson("""
                {"operator":"ANOMALY_DROP","baselineWindow":7,"minSamples":3,"sensitivity":2.0}
                """);
        when(alertRuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, persisted);
        BiSubscriptionAdminService service = service(subscriptionMapper, alertRuleMapper, datasetMapper, metricMapper,
                dashboardMapper, chartMapper, portalMapper, null);

        BiAlertRuleView view = service.upsertAlert(7L, "alice", "OPERATOR", anomalyAlertCommand());

        ArgumentCaptor<BiAlertRuleDO> captor = ArgumentCaptor.forClass(BiAlertRuleDO.class);
        verify(alertRuleMapper).insert(captor.capture());
        assertThat(captor.getValue().getConditionJson()).contains("ANOMALY_DROP", "baselineWindow", "minSamples");
        assertThat(view.condition()).containsEntry("operator", "ANOMALY_DROP");
    }

    private BiSubscriptionAdminService service(BiSubscriptionMapper subscriptionMapper,
                                               BiAlertRuleMapper alertRuleMapper,
                                               BiDatasetMapper datasetMapper,
                                               BiMetricMapper metricMapper,
                                               BiDashboardMapper dashboardMapper,
                                               BiChartMapper chartMapper,
                                               BiPortalMapper portalMapper,
                                               BiPermissionService permissionService) {
        return new BiSubscriptionAdminService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                metricMapper,
                dashboardMapper,
                chartMapper,
                portalMapper,
                permissionService,
                new ObjectMapper());
    }

    private BiSubscriptionCommand subscriptionCommand() {
        return new BiSubscriptionCommand(
                "canvas-daily",
                "Canvas Daily",
                "DASHBOARD",
                "canvas-effect",
                null,
                Map.of("frequency", "DAILY", "time", "09:00"),
                Map.of("channels", List.of("EMAIL", "LARK"), "users", List.of("alice")),
                Map.of("content", "SNAPSHOT_LINK"),
                true);
    }

    private BiAlertRuleCommand alertCommand() {
        return new BiAlertRuleCommand(
                "success-rate-alert",
                "Success Rate Alert",
                "canvas_daily_stats",
                "success_rate",
                Map.of("operator", "LT", "threshold", 0.9),
                Map.of("channels", List.of("LARK"), "users", List.of("alice")),
                true);
    }

    private BiAlertRuleCommand anomalyAlertCommand() {
        return new BiAlertRuleCommand(
                "success-rate-anomaly",
                "Success Rate Anomaly",
                "canvas_daily_stats",
                "success_rate",
                Map.of("operator", "ANOMALY_DROP", "baselineWindow", 7, "minSamples", 3, "sensitivity", 2.0),
                Map.of("channels", List.of("LARK"), "users", List.of("alice")),
                true);
    }

    private BiSubscriptionDO subscriptionRow() {
        BiSubscriptionDO row = new BiSubscriptionDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSubscriptionKey("canvas-daily");
        row.setName("Canvas Daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setScheduleJson("{\"frequency\":\"DAILY\"}");
        row.setReceiverJson("{\"channels\":[\"EMAIL\"]}");
        row.setDeliveryJson("{\"content\":\"SNAPSHOT_LINK\"}");
        row.setEnabled(true);
        return row;
    }

    private BiAlertRuleDO alertRow() {
        BiAlertRuleDO row = new BiAlertRuleDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setAlertKey("success-rate-alert");
        row.setName("Success Rate Alert");
        row.setDatasetId(11L);
        row.setMetricKey("success_rate");
        row.setConditionJson("{\"operator\":\"LT\",\"threshold\":0.9}");
        row.setReceiverJson("{\"channels\":[\"LARK\"]}");
        row.setEnabled(true);
        return row;
    }

    private BiDashboardDO dashboard() {
        BiDashboardDO row = new BiDashboardDO();
        row.setId(21L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDashboardKey("canvas-effect");
        row.setName("Canvas Effect");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiDatasetDO dataset() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDatasetKey("canvas_daily_stats");
        row.setName("Canvas Daily Stats");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiMetricDO metric() {
        BiMetricDO row = new BiMetricDO();
        row.setId(12L);
        row.setTenantId(7L);
        row.setDatasetId(11L);
        row.setMetricKey("success_rate");
        row.setStatus("ACTIVE");
        return row;
    }

    @SuppressWarnings("unused")
    private BiChartDO chart() {
        return new BiChartDO();
    }

    @SuppressWarnings("unused")
    private BiPortalDO portal() {
        return new BiPortalDO();
    }
}
