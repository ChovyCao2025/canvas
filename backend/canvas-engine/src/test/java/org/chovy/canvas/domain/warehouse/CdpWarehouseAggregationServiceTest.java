package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseAggregationServiceTest {

    @Test
    void aggregateRejectsInvalidWindow() {
        CdpWarehouseAggregationService service = service(null);

        assertThatThrownBy(() -> service.aggregate(9L,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be before to");
    }

    @Test
    void aggregateNoopsWhenDorisDisabled() {
        CdpWarehouseAggregationService service = service(null);

        CdpWarehouseAggregationService.AggregationResult result = service.aggregate(9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "operator");

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.dwdRows()).isZero();
        assertThat(result.dwsRows()).isZero();
    }

    @Test
    void aggregateExecutesBoundedDwdAndDwsSqlWithTenantScope() {
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.update(contains("INSERT INTO canvas_dwd.cdp_user_event_fact"), any(Object[].class))).thenReturn(3);
        when(doris.update(contains("INSERT INTO canvas_dws.user_event_metric_daily"), any(Object[].class))).thenReturn(2);
        CdpWarehouseAggregationService service = service(doris);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseAggregationService.AggregationResult result = service.aggregate(9L, from, to, "operator");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.dwdRows()).isEqualTo(3);
        assertThat(result.dwsRows()).isEqualTo(2);
        verify(doris).update(contains("WHERE tenant_id = ?"), eq(9L), eq(from), eq(to));
        verify(doris).update(contains("WHERE tenant_id = ?"), eq(9L), eq(from.toLocalDate()), eq(to.toLocalDate()));
    }

    @Test
    void aggregateRecordsOfflineAssetAvailabilityAfterSuccess() {
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.update(contains("INSERT INTO canvas_dwd.cdp_user_event_fact"), any(Object[].class))).thenReturn(3);
        when(doris.update(contains("INSERT INTO canvas_dws.user_event_metric_daily"), any(Object[].class))).thenReturn(2);
        CdpWarehouseSyncRunMapper runMapper = mock(CdpWarehouseSyncRunMapper.class);
        doAnswer(invocation -> {
            CdpWarehouseSyncRunDO run = invocation.getArgument(0);
            run.setId(42L);
            return 1;
        }).when(runMapper).insert(any(CdpWarehouseSyncRunDO.class));
        CdpWarehouseConsumerAvailabilityService availabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseAggregationService service = service(
                doris, runMapper, mock(CdpWarehouseWatermarkMapper.class), availabilityService);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);

        CdpWarehouseAggregationService.AggregationResult result = service.aggregate(9L, from, to, "operator");

        assertThat(result.status()).isEqualTo("SUCCESS");
        ArgumentCaptor<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand> captor =
                ArgumentCaptor.forClass(CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand.class);
        verify(availabilityService, times(4)).recordAssetAvailability(eq(9L), captor.capture());
        List<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand> commands = captor.getAllValues();
        assertThat(commands)
                .extracting(command -> command.assetType() + ":" + command.assetKey())
                .containsExactly(
                        "TABLE:canvas_dwd.cdp_user_event_fact",
                        "TABLE:canvas_dws.user_event_metric_daily",
                        "DATASET:cdp_dwd_user_event_fact",
                        "DATASET:cdp_dws_user_event_metric_daily");
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.availabilityMode()).isEqualTo("OFFLINE");
            assertThat(command.windowStart()).isEqualTo(from);
            assertThat(command.windowEnd()).isEqualTo(to);
            assertThat(command.availableUntil()).isEqualTo(to);
            assertThat(command.status()).isEqualTo("PASS");
            assertThat(command.evidenceSource()).isEqualTo("AGGREGATE_JOB");
            assertThat(command.evidenceRef()).isEqualTo("run:42");
            assertThat(command.reason()).contains("offline aggregation completed");
        });
    }

    @Test
    void aggregateAvailabilitySideEffectFailureDoesNotRejectSuccess() {
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(doris.update(contains("INSERT INTO canvas_dwd.cdp_user_event_fact"), any(Object[].class))).thenReturn(3);
        when(doris.update(contains("INSERT INTO canvas_dws.user_event_metric_daily"), any(Object[].class))).thenReturn(2);
        CdpWarehouseConsumerAvailabilityService availabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        doThrow(new RuntimeException("availability store unavailable"))
                .when(availabilityService).recordAssetAvailability(any(), any());
        CdpWarehouseAggregationService service = service(
                doris,
                mock(CdpWarehouseSyncRunMapper.class),
                mock(CdpWarehouseWatermarkMapper.class),
                availabilityService);

        CdpWarehouseAggregationService.AggregationResult result = service.aggregate(9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "operator");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.dwdRows()).isEqualTo(3);
        assertThat(result.dwsRows()).isEqualTo(2);
    }

    private CdpWarehouseAggregationService service(JdbcTemplate doris) {
        return service(doris, mock(CdpWarehouseSyncRunMapper.class), mock(CdpWarehouseWatermarkMapper.class), null);
    }

    @SuppressWarnings("unchecked")
    private CdpWarehouseAggregationService service(JdbcTemplate doris,
                                                   CdpWarehouseSyncRunMapper runMapper,
                                                   CdpWarehouseWatermarkMapper watermarkMapper,
                                                   CdpWarehouseConsumerAvailabilityService availabilityService) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(doris);
        ObjectProvider<CdpWarehouseConsumerAvailabilityService> availabilityProvider = mock(ObjectProvider.class);
        when(availabilityProvider.getIfAvailable()).thenReturn(availabilityService);
        return new CdpWarehouseAggregationService(
                provider,
                runMapper,
                watermarkMapper,
                availabilityProvider);
    }
}
