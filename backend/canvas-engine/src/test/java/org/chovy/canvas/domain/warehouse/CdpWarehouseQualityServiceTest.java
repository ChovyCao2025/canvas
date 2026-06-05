package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseQualityCheckDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseQualityCheckMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class CdpWarehouseQualityServiceTest {

    @Test
    void reconcileOdsRecordsPassWhenCountsAreWithinTolerance() {
        Fixture fixture = fixture(10L, 10L);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseQualityService.QualityCheckResult result =
                fixture.service.reconcileOds(9L, from, to, 0, "operator");

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.sourceCount()).isEqualTo(10);
        assertThat(result.warehouseCount()).isEqualTo(10);
        ArgumentCaptor<CdpWarehouseQualityCheckDO> row =
                ArgumentCaptor.forClass(CdpWarehouseQualityCheckDO.class);
        verify(fixture.qualityMapper).insert(row.capture());
        assertThat(row.getValue().getCheckType()).isEqualTo("ODS_COUNT");
        assertThat(row.getValue().getStatus()).isEqualTo("PASS");
        assertThat(row.getValue().getDiffCount()).isZero();
    }

    @Test
    void reconcileOdsRecordsWarnWhenCountDiffExceedsTolerance() {
        Fixture fixture = fixture(10L, 7L);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseQualityService.QualityCheckResult result =
                fixture.service.reconcileOds(9L, from, to, 1, "operator");

        assertThat(result.status()).isEqualTo("WARN");
        assertThat(result.diffCount()).isEqualTo(3);
    }

    @Test
    void reconcileOdsWarnRecordsIncidentSideEffect() {
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        Fixture fixture = fixture(10L, 7L, incidentService);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseQualityService.QualityCheckResult result =
                fixture.service.reconcileOds(9L, from, to, 1, "operator");

        assertThat(result.status()).isEqualTo("WARN");
        verify(incidentService).recordQualityIncident(result);
    }

    @Test
    void incidentFailureDoesNotFailQualityCheck() {
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        doThrow(new IllegalStateException("incident store unavailable"))
                .when(incidentService).recordQualityIncident(org.mockito.ArgumentMatchers.any());
        Fixture fixture = fixture(10L, 7L, incidentService);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseQualityService.QualityCheckResult result =
                fixture.service.reconcileOds(9L, from, to, 1, "operator");

        assertThat(result.status()).isEqualTo("WARN");
    }

    @Test
    void reconcileOdsRecordsSkippedWhenDorisIsDisabled() {
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectCount(any())).thenReturn(10L);
        CdpWarehouseQualityCheckMapper qualityMapper = mock(CdpWarehouseQualityCheckMapper.class);
        CdpWarehouseQualityService service = service(eventLogMapper, null, qualityMapper,
                mock(CdpWarehouseWatermarkMapper.class));
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseQualityService.QualityCheckResult result =
                service.reconcileOds(9L, from, to, 0, "operator");

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.sourceCount()).isEqualTo(10);
        assertThat(result.warehouseCount()).isNull();
    }

    @Test
    void reconcileOdsRejectsInvalidWindow() {
        CdpWarehouseQualityService service = fixture(10L, 10L).service;
        LocalDateTime at = LocalDateTime.of(2026, 6, 5, 10, 0);

        assertThatThrownBy(() -> service.reconcileOds(9L, at, at, 0, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be before to");
    }

    @Test
    void aggregateLagPassesWhenWatermarkIsWithinThreshold() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setWatermarkValue(now.minusMinutes(10).toString());
        watermark.setWatermarkTime(now.minusMinutes(10));
        when(watermarkMapper.selectOne(any())).thenReturn(watermark);
        CdpWarehouseQualityService service = service(mock(CdpEventLogMapper.class), mock(JdbcTemplate.class),
                mock(CdpWarehouseQualityCheckMapper.class), watermarkMapper);

        CdpWarehouseQualityService.QualityCheckResult result =
                service.checkAggregateLag(9L, now, 30, "operator");

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.diffCount()).isEqualTo(10);
    }

    @Test
    void aggregateLagWarnsWhenWatermarkIsMissing() {
        CdpWarehouseQualityCheckMapper qualityMapper = mock(CdpWarehouseQualityCheckMapper.class);
        CdpWarehouseQualityService service = service(mock(CdpEventLogMapper.class), mock(JdbcTemplate.class),
                qualityMapper, mock(CdpWarehouseWatermarkMapper.class));

        CdpWarehouseQualityService.QualityCheckResult result =
                service.checkAggregateLag(9L, LocalDateTime.of(2026, 6, 5, 12, 0), 30, "operator");

        assertThat(result.status()).isEqualTo("WARN");
        assertThat(result.details()).contains("missing aggregate watermark");
    }

    @Test
    void recentChecksAreTenantScopedAndBounded() {
        CdpWarehouseQualityCheckMapper qualityMapper = mock(CdpWarehouseQualityCheckMapper.class);
        when(qualityMapper.selectList(any())).thenReturn(List.of(check("ODS_COUNT", "PASS")));
        CdpWarehouseQualityService service = service(mock(CdpEventLogMapper.class), mock(JdbcTemplate.class),
                qualityMapper, mock(CdpWarehouseWatermarkMapper.class));

        List<CdpWarehouseQualityService.QualityCheckResult> checks = service.recentChecks(9L, 200);

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).checkType()).isEqualTo("ODS_COUNT");
        verify(qualityMapper).selectList(any());
    }

    private Fixture fixture(Long mysqlCount, Long dorisCount) {
        return fixture(mysqlCount, dorisCount, null);
    }

    private Fixture fixture(Long mysqlCount, Long dorisCount, CdpWarehouseIncidentService incidentService) {
        CdpEventLogMapper eventLogMapper = mock(CdpEventLogMapper.class);
        when(eventLogMapper.selectCount(any())).thenReturn(mysqlCount);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.queryForObject(contains("canvas_ods.cdp_event_log"), eq(Long.class), any(Object[].class)))
                .thenReturn(dorisCount);
        CdpWarehouseQualityCheckMapper qualityMapper = mock(CdpWarehouseQualityCheckMapper.class);
        CdpWarehouseQualityService service = service(eventLogMapper, doris, qualityMapper,
                mock(CdpWarehouseWatermarkMapper.class), incidentService);
        return new Fixture(service, qualityMapper);
    }

    private CdpWarehouseQualityService service(CdpEventLogMapper eventLogMapper,
                                               JdbcTemplate doris,
                                               CdpWarehouseQualityCheckMapper qualityMapper,
                                               CdpWarehouseWatermarkMapper watermarkMapper) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(doris);
        return new CdpWarehouseQualityService(eventLogMapper, provider, qualityMapper, watermarkMapper);
    }

    private CdpWarehouseQualityService service(CdpEventLogMapper eventLogMapper,
                                               JdbcTemplate doris,
                                               CdpWarehouseQualityCheckMapper qualityMapper,
                                               CdpWarehouseWatermarkMapper watermarkMapper,
                                               CdpWarehouseIncidentService incidentService) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(doris);
        ObjectProvider<CdpWarehouseIncidentService> incidentProvider = mock(ObjectProvider.class);
        when(incidentProvider.getIfAvailable()).thenReturn(incidentService);
        return new CdpWarehouseQualityService(eventLogMapper, provider, qualityMapper, watermarkMapper,
                incidentProvider);
    }

    private CdpWarehouseQualityCheckDO check(String type, String status) {
        CdpWarehouseQualityCheckDO row = new CdpWarehouseQualityCheckDO();
        row.setId(1L);
        row.setTenantId(9L);
        row.setCheckType(type);
        row.setStatus(status);
        row.setSourceCount(10L);
        row.setWarehouseCount(10L);
        row.setDiffCount(0L);
        row.setCheckedAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        return row;
    }

    private record Fixture(CdpWarehouseQualityService service, CdpWarehouseQualityCheckMapper qualityMapper) {
    }
}
