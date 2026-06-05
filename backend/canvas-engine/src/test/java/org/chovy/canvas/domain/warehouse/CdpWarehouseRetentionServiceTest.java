package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseIncidentMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRetentionServiceTest {

    @Test
    void planCountsEligibleRowsWithCutoffs() {
        CdpWarehouseSyncRunMapper syncRunMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseRealtimeRetryMapper retryMapper = mock(CdpWarehouseRealtimeRetryMapper.class);
        CdpWarehouseIncidentMapper incidentMapper = mock(CdpWarehouseIncidentMapper.class);
        when(syncRunMapper.selectCount(any())).thenReturn(2L);
        when(retryMapper.selectCount(any())).thenReturn(3L);
        when(incidentMapper.selectCount(any())).thenReturn(4L);
        CdpWarehouseRetentionService service = new CdpWarehouseRetentionService(
                syncRunMapper, retryMapper, incidentMapper);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseRetentionService.RetentionPlan plan =
                service.plan(9L, now, 30, 14, 90);

        assertThat(plan.tenantId()).isEqualTo(9L);
        assertThat(plan.generatedAt()).isEqualTo(now);
        assertThat(plan.syncRuns().cutoff()).isEqualTo(now.minusDays(30));
        assertThat(plan.realtimeRetries().cutoff()).isEqualTo(now.minusDays(14));
        assertThat(plan.resolvedIncidents().cutoff()).isEqualTo(now.minusDays(90));
        assertThat(plan.totalEligibleRows()).isEqualTo(9L);
    }

    @Test
    void cleanupDeletesEligibleRowsAndReturnsCounts() {
        CdpWarehouseSyncRunMapper syncRunMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseRealtimeRetryMapper retryMapper = mock(CdpWarehouseRealtimeRetryMapper.class);
        CdpWarehouseIncidentMapper incidentMapper = mock(CdpWarehouseIncidentMapper.class);
        when(syncRunMapper.selectCount(any())).thenReturn(2L);
        when(retryMapper.selectCount(any())).thenReturn(3L);
        when(incidentMapper.selectCount(any())).thenReturn(4L);
        when(syncRunMapper.delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseSyncRunDO>>any()))
                .thenReturn(1);
        when(retryMapper.delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseRealtimeRetryDO>>any()))
                .thenReturn(2);
        when(incidentMapper.delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseIncidentDO>>any()))
                .thenReturn(3);
        CdpWarehouseRetentionService service = new CdpWarehouseRetentionService(
                syncRunMapper, retryMapper, incidentMapper);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);

        CdpWarehouseRetentionService.RetentionCleanupResult result =
                service.cleanup(9L, now, 30, 14, 90, "operator");

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.operator()).isEqualTo("operator");
        assertThat(result.syncRuns().eligibleRows()).isEqualTo(2L);
        assertThat(result.syncRuns().deletedRows()).isEqualTo(1);
        assertThat(result.realtimeRetries().deletedRows()).isEqualTo(2);
        assertThat(result.resolvedIncidents().deletedRows()).isEqualTo(3);
        assertThat(result.totalDeletedRows()).isEqualTo(6L);
        verify(syncRunMapper).delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseSyncRunDO>>any());
        verify(retryMapper).delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseRealtimeRetryDO>>any());
        verify(incidentMapper).delete(org.mockito.ArgumentMatchers.<Wrapper<CdpWarehouseIncidentDO>>any());
    }

    @Test
    void rejectsInvalidRetentionDays() {
        CdpWarehouseRetentionService service = new CdpWarehouseRetentionService(
                mock(CdpWarehouseSyncRunMapper.class),
                mock(CdpWarehouseRealtimeRetryMapper.class),
                mock(CdpWarehouseIncidentMapper.class));

        assertThatThrownBy(() -> service.plan(9L, LocalDateTime.now(), 0, 14, 90))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("syncRunRetentionDays");
        assertThatThrownBy(() -> service.plan(9L, LocalDateTime.now(), 30, 3651, 90))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("realtimeRetryRetentionDays");
    }
}
