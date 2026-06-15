package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade;
import org.junit.jupiter.api.Test;

class CdpTagOperationApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T04:30:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void createsListsGetsAndRetriesFailedOperationsWithinTenant() {
        CdpTagOperationFacade service = new CdpTagOperationApplicationService(CLOCK);

        CdpTagOperationFacade.TagOperationView created = service.create(7L,
                new CdpTagOperationFacade.BatchTagCommand(" user-1 ", "vip_level", "gold",
                        List.of("a", "b", "a", " "), Map.of("source", "rule")), "operator-1");
        CdpTagOperationFacade.TagOperationView failed = service.create(7L,
                new CdpTagOperationFacade.BatchTagCommand("user-2", "vip_level", "silver",
                        List.of(), Map.of("simulateStatus", "FAILED")), "operator-2");

        assertThat(created).returns(7L, CdpTagOperationFacade.TagOperationView::tenantId)
                .returns("user-1", CdpTagOperationFacade.TagOperationView::userId)
                .returns("vip_level", CdpTagOperationFacade.TagOperationView::tagCode)
                .returns("gold", CdpTagOperationFacade.TagOperationView::tagValue)
                .returns("SUCCESS", CdpTagOperationFacade.TagOperationView::status)
                .returns(2, CdpTagOperationFacade.TagOperationView::affectedCount)
                .returns("operator-1", CdpTagOperationFacade.TagOperationView::createdBy);
        assertThat(created.memberIds()).containsExactly("a", "b");

        assertThat(service.listRecent(7L, 20)).extracting(CdpTagOperationFacade.TagOperationView::id)
                .containsExactly(failed.id(), created.id());
        assertThat(service.get(7L, created.id())).isEqualTo(created);
        assertThat(service.retryFailed(7L, failed.id(), "retry-operator"))
                .returns("RETRYING", CdpTagOperationFacade.TagOperationView::status)
                .returns("retry-operator", CdpTagOperationFacade.TagOperationView::updatedBy);
        assertThat(service.listRecent(8L, 20)).isEmpty();
    }

    @Test
    void validationDefaultsAndLimitBoundsFollowLegacyCompatibility() {
        CdpTagOperationFacade service = new CdpTagOperationApplicationService(CLOCK);
        for (int i = 0; i < 3; i++) {
            service.create(null, new CdpTagOperationFacade.BatchTagCommand("user-" + i, "tag", null,
                    List.of(), Map.of()), "");
        }

        assertThat(service.listRecent(null, 0)).hasSize(1);
        assertThat(service.listRecent(null, 200)).hasSize(3);
        assertThat(service.get(null, 1L).tenantId()).isEqualTo(0L);
        assertThat(service.get(null, 1L).createdBy()).isEqualTo("system");

        assertThatThrownBy(() -> service.create(7L, null, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag operation request is required");
        assertThatThrownBy(() -> service.create(7L,
                new CdpTagOperationFacade.BatchTagCommand(" ", "tag", null, List.of(), Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
        assertThatThrownBy(() -> service.get(7L, 404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag operation is not found");
        assertThatThrownBy(() -> service.retryFailed(null, 1L, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only failed tag operations can be retried");
    }
}
