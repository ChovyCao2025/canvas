package org.chovy.canvas.domain.bi.subscription;

import org.chovy.canvas.dal.dataobject.BiDeliverySchedulerLeaseDO;
import org.chovy.canvas.dal.mapper.BiDeliverySchedulerLeaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDeliverySchedulerLeaseServiceTest {

    @Test
    void acquireWritesLeaseAndConfirmsOwnership() {
        BiDeliverySchedulerLeaseMapper mapper = mock(BiDeliverySchedulerLeaseMapper.class);
        when(mapper.tryAcquire(any(), any())).thenReturn(1);
        when(mapper.findByKey(7L, "BI_DELIVERY_SCHEDULER")).thenReturn(leaseRow("pod-a"));
        BiDeliverySchedulerLeaseService service = new BiDeliverySchedulerLeaseService(mapper, "pod-a");

        boolean acquired = service.acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120));

        assertThat(acquired).isTrue();
        ArgumentCaptor<BiDeliverySchedulerLeaseDO> row = ArgumentCaptor.forClass(BiDeliverySchedulerLeaseDO.class);
        verify(mapper).tryAcquire(row.capture(), any());
        assertThat(row.getValue().getTenantId()).isEqualTo(7L);
        assertThat(row.getValue().getLeaseKey()).isEqualTo("BI_DELIVERY_SCHEDULER");
        assertThat(row.getValue().getOwnerId()).isEqualTo("pod-a");
        assertThat(row.getValue().getLeaseUntil()).isAfter(row.getValue().getLastAcquiredAt());
    }

    @Test
    void acquireReturnsFalseWhenAnotherOwnerStillHoldsLease() {
        BiDeliverySchedulerLeaseMapper mapper = mock(BiDeliverySchedulerLeaseMapper.class);
        when(mapper.tryAcquire(any(), any())).thenReturn(0);
        when(mapper.findByKey(7L, "BI_DELIVERY_SCHEDULER")).thenReturn(leaseRow("pod-b"));
        BiDeliverySchedulerLeaseService service = new BiDeliverySchedulerLeaseService(mapper, "pod-a");

        boolean acquired = service.acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120));

        assertThat(acquired).isFalse();
    }

    @Test
    void releaseOnlyReleasesCurrentOwner() {
        BiDeliverySchedulerLeaseMapper mapper = mock(BiDeliverySchedulerLeaseMapper.class);
        BiDeliverySchedulerLeaseService service = new BiDeliverySchedulerLeaseService(mapper, "pod-a");

        service.release(7L, "BI_DELIVERY_SCHEDULER");

        verify(mapper).release(eq(7L), eq("BI_DELIVERY_SCHEDULER"), eq("pod-a"), any());
    }

    @Test
    void rejectsInvalidLeaseRequests() {
        BiDeliverySchedulerLeaseService service = new BiDeliverySchedulerLeaseService(
                mock(BiDeliverySchedulerLeaseMapper.class),
                "pod-a");

        assertThatThrownBy(() -> service.acquire(7L, "", Duration.ofSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leaseKey is required");
        assertThatThrownBy(() -> service.acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl must be positive");
    }

    private BiDeliverySchedulerLeaseDO leaseRow(String ownerId) {
        BiDeliverySchedulerLeaseDO row = new BiDeliverySchedulerLeaseDO();
        row.setTenantId(7L);
        row.setLeaseKey("BI_DELIVERY_SCHEDULER");
        row.setOwnerId(ownerId);
        row.setLeaseUntil(LocalDateTime.now().plusSeconds(120));
        row.setLastAcquiredAt(LocalDateTime.now());
        return row;
    }
}
