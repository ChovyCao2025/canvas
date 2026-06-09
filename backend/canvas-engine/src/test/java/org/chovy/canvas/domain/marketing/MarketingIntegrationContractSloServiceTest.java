package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractSloServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void evaluateAndSyncContractPagesWhenFastBurnWindowsBreachErrorBudget() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeObservationMapper observationMapper =
                mock(MarketingIntegrationContractProbeObservationMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        MarketingIntegrationContractSloService service = new MarketingIntegrationContractSloService(
                contractMapper,
                observationMapper,
                alertMapper,
                new ObjectMapper(),
                fanoutService,
                CLOCK);
        MarketingIntegrationContractDO contract = contract("STANDARD");
        when(observationMapper.selectWindowStats(any(), any(), any(), any()))
                .thenReturn(stats(100, 20))
                .thenReturn(stats(10, 2));
        when(alertMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAlertDO>getArgument(0).setId(901L);
            return 1;
        }).when(alertMapper).insert(any(MarketingMonitorAlertDO.class));

        MarketingIntegrationContractSloEvaluationView view = service.evaluateAndSyncContract(
                7L,
                contract,
                "prod-readiness-probe",
                "probe-scheduler");

        assertThat(view.status()).isEqualTo("PAGE");
        assertThat(view.triggeredRuleKey()).isEqualTo("PAGE_FAST_BURN");
        assertThat(view.targetPercent()).isEqualTo(99.0);
        assertThat(view.windows()).hasSize(2);
        assertThat(view.windows()).allSatisfy(window -> {
            assertThat(window.badRatio()).isEqualTo(0.2);
            assertThat(window.burnRate()).isEqualTo(20.0);
            assertThat(window.breached()).isTrue();
        });
        ArgumentCaptor<MarketingMonitorAlertDO> alertCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(alertMapper).insert(alertCaptor.capture());
        MarketingMonitorAlertDO alert = alertCaptor.getValue();
        assertThat(alert.getAlertType()).isEqualTo("INTEGRATION_CONTRACT_SLO_BURN_RATE");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getStatus()).isEqualTo("OPEN");
        assertThat(alert.getScopeKey()).isEqualTo("google-ads-keyword-write");
        assertThat(alert.getReason()).contains("PAGE_FAST_BURN", "20.00x");
        assertThat(alert.getWindowStart()).isEqualTo(LocalDateTime.parse("2026-06-06T09:00:00"));
        assertThat(alert.getWindowEnd()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(alert.getMetadataJson()).contains("\"targetPercent\":99.0");
        verify(fanoutService).dispatchAlert(7L, alert, "probe-scheduler");
    }

    @Test
    void listProductionSloEvaluationsFiltersActiveProductionContractsAndBoundsLimit() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeObservationMapper observationMapper =
                mock(MarketingIntegrationContractProbeObservationMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingIntegrationContractSloService service = new MarketingIntegrationContractSloService(
                contractMapper,
                observationMapper,
                alertMapper,
                new ObjectMapper(),
                null,
                CLOCK);
        when(contractMapper.selectList(any())).thenReturn(List.of(contract("STANDARD")));
        when(observationMapper.selectWindowStats(any(), any(), any(), any()))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0))
                .thenReturn(stats(100, 0))
                .thenReturn(stats(10, 0));

        List<MarketingIntegrationContractSloEvaluationView> rows =
                service.listProductionSloEvaluations(7L, 500);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.contractKey()).isEqualTo("google-ads-keyword-write");
                    assertThat(row.status()).isEqualTo("OK");
                    assertThat(row.triggeredRuleKey()).isNull();
                });
    }

    private static MarketingIntegrationContractProbeWindowStatsDO stats(long total, long bad) {
        MarketingIntegrationContractProbeWindowStatsDO row = new MarketingIntegrationContractProbeWindowStatsDO();
        row.setTotalCount(total);
        row.setBadCount(bad);
        return row;
    }

    private static MarketingIntegrationContractDO contract(String slaTier) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setContractKey("google-ads-keyword-write");
        row.setDisplayName("Google Ads keyword write");
        row.setProviderFamily("SEM");
        row.setEnvironment("PRODUCTION");
        row.setStatus("ACTIVE");
        row.setSlaTier(slaTier);
        row.setMetadataJson("{\"sloProbeKey\":\"prod-readiness-probe\"}");
        return row;
    }
}
