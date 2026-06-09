package org.chovy.canvas.domain.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.AnalyticsAlertRuleDO;
import org.chovy.canvas.dal.dataobject.AnalyticsExportJobDO;
import org.chovy.canvas.dal.dataobject.AnalyticsFunnelDefinitionDO;
import org.chovy.canvas.dal.mapper.AnalyticsAlertRuleMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsExportJobMapper;
import org.chovy.canvas.dal.mapper.AnalyticsFunnelDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalyticsQueryServiceTest {

    @Test
    void returnsEventCountsFromMapperRows() {
        AnalyticsEventMapper mapper = mock(AnalyticsEventMapper.class);
        when(mapper.selectEventCounts(7L, "2026-01-01", "2026-01-31"))
                .thenReturn(List.of(Map.of("eventCode", "purchase", "count", 12L)));
        AnalyticsQueryService service = new AnalyticsQueryService(mapper, new AnalyticsQueryGuard());

        List<AnalyticsQueryService.EventCountRow> rows =
                service.eventCounts(7L, "2026-01-01", "2026-01-31");

        assertThat(rows).containsExactly(new AnalyticsQueryService.EventCountRow("purchase", 12L));
    }

    @Test
    void userTimelineAppliesPageLimitAndReturnsTotal() {
        AnalyticsEventMapper mapper = mock(AnalyticsEventMapper.class);
        when(mapper.selectUserTimeline(7L, "u1", "2026-01-01", "2026-01-31", 400, 200))
                .thenReturn(List.of(Map.of("eventCode", "open", "eventTime", "2026-01-02T10:00:00")));
        when(mapper.countUserTimeline(7L, "u1", "2026-01-01", "2026-01-31")).thenReturn(501L);
        AnalyticsQueryService service = new AnalyticsQueryService(mapper, new AnalyticsQueryGuard());

        PageResult<AnalyticsQueryService.UserTimelineRow> page =
                service.userTimeline(7L, "u1", "2026-01-01", "2026-01-31", 3, 999);

        assertThat(page.getTotal()).isEqualTo(501L);
        assertThat(page.getList()).containsExactly(
                new AnalyticsQueryService.UserTimelineRow("open", "2026-01-02T10:00:00"));
        verify(mapper).selectUserTimeline(7L, "u1", "2026-01-01", "2026-01-31", 400, 200);
    }

    @Test
    void eventCountDelegatesToEventCodeCountWhenProvided() {
        AnalyticsEventMapper mapper = mock(AnalyticsEventMapper.class);
        when(mapper.countByEventCode(7L, "purchase", "2026-01-01", "2026-01-31")).thenReturn(9L);
        AnalyticsQueryService service = new AnalyticsQueryService(mapper, new AnalyticsQueryGuard());

        AnalyticsQueryService.EventTotal total =
                service.countEvents(7L, "2026-01-01", "2026-01-31", "purchase");

        assertThat(total.count()).isEqualTo(9L);
    }

    @Test
    void rejectsUnsafeAttributeBeforeMapperCall() {
        AnalyticsEventMapper mapper = mock(AnalyticsEventMapper.class);
        AnalyticsQueryService service = new AnalyticsQueryService(mapper, new AnalyticsQueryGuard());

        assertThatThrownBy(() -> service.attributeDistribution(7L, "a[0]", "2026-01-01", "2026-01-31"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dot-separated identifier path");
        verifyNoInteractions(mapper);
    }

    @Test
    void funnelResultUsesLatestEnabledDefinitionAndBoundedScope() {
        AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
        AnalyticsFunnelDefinitionMapper funnelMapper = mock(AnalyticsFunnelDefinitionMapper.class);
        AnalyticsFunnelDefinitionDO row = new AnalyticsFunnelDefinitionDO();
        row.setFunnelKey("signup");
        row.setVersion(3);
        row.setName("Signup");
        row.setStepsJson("[{\"eventCode\":\"view\"},{\"eventCode\":\"submit\"}]");
        when(funnelMapper.selectLatestEnabled(7L, "signup")).thenReturn(row);
        AnalyticsQueryService service = service(eventMapper, funnelMapper, null, null, 100_000);

        AnalyticsQueryService.FunnelResult result =
                service.funnelResult(7L, "signup", "2026-01-01", "2026-01-31");

        assertThat(result.version()).isEqualTo(3);
        assertThat(result.startDate()).isEqualTo("2026-01-01");
        assertThat(result.steps()).hasSize(2);
        verify(funnelMapper).selectLatestEnabled(7L, "signup");
    }

    @Test
    void alertPreviewCountsScopedEventAndEvaluatesThreshold() {
        AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
        AnalyticsAlertRuleMapper alertRuleMapper = mock(AnalyticsAlertRuleMapper.class);
        AnalyticsAlertRuleDO row = new AnalyticsAlertRuleDO();
        row.setRuleKey("purchase-spike");
        row.setName("Purchase spike");
        row.setThresholdJson("{\"countGte\":10}");
        when(alertRuleMapper.selectOne(any())).thenReturn(row);
        when(eventMapper.countByEventCode(7L, "purchase", "2026-01-01", "2026-01-31")).thenReturn(12L);
        AnalyticsQueryService service = service(eventMapper, null, alertRuleMapper, null, 100_000);

        AnalyticsQueryService.AlertPreviewResult result = service.alertPreview(7L,
                new AnalyticsQueryService.AlertPreviewRequest(
                        "purchase-spike", "purchase", "2026-01-01", "2026-01-31"));

        assertThat(result.count()).isEqualTo(12L);
        assertThat(result.threshold()).isEqualTo(10L);
        assertThat(result.triggered()).isTrue();
    }

    @Test
    void createExportQueuesRowLimitedJob() {
        AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
        AnalyticsExportJobMapper exportJobMapper = mock(AnalyticsExportJobMapper.class);
        when(eventMapper.countByEventCode(7L, "purchase", "2026-01-01", "2026-01-31")).thenReturn(99L);
        doAnswer(invocation -> {
            AnalyticsExportJobDO row = invocation.getArgument(0);
            row.setId(88L);
            return 1;
        }).when(exportJobMapper).insert(any(AnalyticsExportJobDO.class));
        AnalyticsQueryService service = service(eventMapper, null, null, exportJobMapper, 100_000);

        AnalyticsQueryService.ExportJobView view = service.createExport(7L,
                new AnalyticsQueryService.ExportRequest(
                        "event_analysis", "2026-01-01", "2026-01-31", "purchase", 100, "alice"));

        assertThat(view.id()).isEqualTo(88L);
        assertThat(view.reportType()).isEqualTo("EVENT_ANALYSIS");
        assertThat(view.status()).isEqualTo("QUEUED");
        ArgumentCaptor<AnalyticsExportJobDO> captor = ArgumentCaptor.forClass(AnalyticsExportJobDO.class);
        verify(exportJobMapper).insert(captor.capture());
        assertThat(captor.getValue().getQueryJson()).contains("purchase").contains("estimatedRows");
    }

    @Test
    void createExportRejectsRowsAboveLimitBeforeInsert() {
        AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
        AnalyticsExportJobMapper exportJobMapper = mock(AnalyticsExportJobMapper.class);
        when(eventMapper.countEvents(7L, "2026-01-01", "2026-01-31")).thenReturn(101L);
        AnalyticsQueryService service = service(eventMapper, null, null, exportJobMapper, 100_000);

        assertThatThrownBy(() -> service.createExport(7L,
                new AnalyticsQueryService.ExportRequest(
                        "EVENT_ANALYSIS", "2026-01-01", "2026-01-31", null, 100, "alice")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("export row limit exceeded");
        verifyNoInteractions(exportJobMapper);
    }

    @Test
    void exportStatusRequiresMatchingTenant() {
        AnalyticsEventMapper eventMapper = mock(AnalyticsEventMapper.class);
        AnalyticsExportJobMapper exportJobMapper = mock(AnalyticsExportJobMapper.class);
        AnalyticsExportJobDO row = new AnalyticsExportJobDO();
        row.setId(88L);
        row.setTenantId(9L);
        when(exportJobMapper.selectById(88L)).thenReturn(row);
        AnalyticsQueryService service = service(eventMapper, null, null, exportJobMapper, 100_000);

        assertThatThrownBy(() -> service.exportStatus(7L, 88L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("export job not found");
    }

    private AnalyticsQueryService service(AnalyticsEventMapper eventMapper,
                                          AnalyticsFunnelDefinitionMapper funnelDefinitionMapper,
                                          AnalyticsAlertRuleMapper alertRuleMapper,
                                          AnalyticsExportJobMapper exportJobMapper,
                                          int maxExportRows) {
        return new AnalyticsQueryService(
                eventMapper,
                new AnalyticsQueryGuard(),
                funnelDefinitionMapper,
                alertRuleMapper,
                exportJobMapper,
                new ObjectMapper(),
                maxExportRows);
    }
}
