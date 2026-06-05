package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseJobLeaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseJobLeaseServiceTest {

    @Test
    void acquiredLeaseExecutesWorkAndReleases() {
        CdpWarehouseJobLeaseMapper mapper = mock(CdpWarehouseJobLeaseMapper.class);
        when(mapper.tryAcquire(any(), any())).thenReturn(1);
        when(mapper.findByKey(9L, "CDP_WAREHOUSE_MAIN")).thenReturn(leaseRow("pod-a"));
        CdpWarehouseJobLeaseService service = new CdpWarehouseJobLeaseService(mapper, "pod-a");
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean executed = service.runWithLease(9L, "CDP_WAREHOUSE_MAIN", Duration.ofSeconds(60), () -> {
            ran.set(true);
            return true;
        });

        assertThat(executed).isTrue();
        assertThat(ran).isTrue();
        ArgumentCaptor<CdpWarehouseJobLeaseDO> row = ArgumentCaptor.forClass(CdpWarehouseJobLeaseDO.class);
        verify(mapper).tryAcquire(row.capture(), any());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getLeaseKey()).isEqualTo("CDP_WAREHOUSE_MAIN");
        assertThat(row.getValue().getOwnerId()).isEqualTo("pod-a");
        verify(mapper).release(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("CDP_WAREHOUSE_MAIN"),
                org.mockito.ArgumentMatchers.eq("pod-a"),
                any());
    }

    @Test
    void deniedLeaseSkipsWork() {
        CdpWarehouseJobLeaseMapper mapper = mock(CdpWarehouseJobLeaseMapper.class);
        when(mapper.tryAcquire(any(), any())).thenReturn(0);
        when(mapper.findByKey(9L, "CDP_WAREHOUSE_MAIN")).thenReturn(leaseRow("pod-b"));
        CdpWarehouseJobLeaseService service = new CdpWarehouseJobLeaseService(mapper, "pod-a");
        AtomicBoolean ran = new AtomicBoolean(false);

        boolean executed = service.runWithLease(9L, "CDP_WAREHOUSE_MAIN", Duration.ofSeconds(60), () -> {
            ran.set(true);
            return true;
        });

        assertThat(executed).isFalse();
        assertThat(ran).isFalse();
    }

    @Test
    void failedWorkStillReleasesLease() {
        CdpWarehouseJobLeaseMapper mapper = mock(CdpWarehouseJobLeaseMapper.class);
        when(mapper.tryAcquire(any(), any())).thenReturn(1);
        when(mapper.findByKey(9L, "CDP_WAREHOUSE_MAIN")).thenReturn(leaseRow("pod-a"));
        CdpWarehouseJobLeaseService service = new CdpWarehouseJobLeaseService(mapper, "pod-a");

        assertThatThrownBy(() -> service.runWithLease(9L, "CDP_WAREHOUSE_MAIN", Duration.ofSeconds(60), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        verify(mapper).release(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("CDP_WAREHOUSE_MAIN"),
                org.mockito.ArgumentMatchers.eq("pod-a"),
                any());
    }

    @Test
    void rejectsInvalidLeaseRequest() {
        CdpWarehouseJobLeaseService service = new CdpWarehouseJobLeaseService(
                mock(CdpWarehouseJobLeaseMapper.class), "pod-a");

        assertThatThrownBy(() -> service.runWithLease(9L, "", Duration.ofSeconds(60), () -> true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leaseKey is required");
        assertThatThrownBy(() -> service.runWithLease(9L, "lease", Duration.ZERO, () -> true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl must be positive");
    }

    private CdpWarehouseJobLeaseDO leaseRow(String ownerId) {
        CdpWarehouseJobLeaseDO row = new CdpWarehouseJobLeaseDO();
        row.setTenantId(9L);
        row.setLeaseKey("CDP_WAREHOUSE_MAIN");
        row.setOwnerId(ownerId);
        row.setLeaseUntil(LocalDateTime.now().plusSeconds(60));
        row.setLastAcquiredAt(LocalDateTime.now());
        return row;
    }
}
