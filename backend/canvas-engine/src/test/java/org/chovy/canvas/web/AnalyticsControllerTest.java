package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.domain.analytics.AnalyticsQueryService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {

    @Test
    void eventCountsUseDefaultTenantWhenResolverIsAbsent() {
        AnalyticsQueryService service = mock(AnalyticsQueryService.class);
        when(service.eventCounts(0L, "2026-01-01", "2026-01-31"))
                .thenReturn(List.of(new AnalyticsQueryService.EventCountRow("purchase", 12L)));
        AnalyticsController controller = new AnalyticsController(service);

        StepVerifier.create(controller.eventCounts("2026-01-01", "2026-01-31"))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).containsExactly(
                            new AnalyticsQueryService.EventCountRow("purchase", 12L));
                })
                .verifyComplete();
    }

    @Test
    void userTimelineDelegatesPagingParams() {
        AnalyticsQueryService service = mock(AnalyticsQueryService.class);
        PageResult<AnalyticsQueryService.UserTimelineRow> page = PageResult.of(1L, List.of(
                new AnalyticsQueryService.UserTimelineRow("open", "2026-01-02T10:00:00")));
        when(service.userTimeline(0L, "u1", "2026-01-01", "2026-01-31", 2, 25)).thenReturn(page);
        AnalyticsController controller = new AnalyticsController(service);

        StepVerifier.create(controller.userTimeline("u1", "2026-01-01", "2026-01-31", 2, 25))
                .assertNext(response -> assertThat(response.getData().getTotal()).isEqualTo(1L))
                .verifyComplete();

        verify(service).userTimeline(0L, "u1", "2026-01-01", "2026-01-31", 2, 25);
    }

    @Test
    void attributeDistributionDelegatesAttributePath() {
        AnalyticsQueryService service = mock(AnalyticsQueryService.class);
        when(service.attributeDistribution(0L, "profile.vip", "2026-01-01", "2026-01-31"))
                .thenReturn(List.of(new AnalyticsQueryService.AttributeDistributionRow("true", 3L)));
        AnalyticsController controller = new AnalyticsController(service);

        StepVerifier.create(controller.attributeDistribution("profile.vip", "2026-01-01", "2026-01-31"))
                .assertNext(response -> assertThat(response.getData()).containsExactly(
                        new AnalyticsQueryService.AttributeDistributionRow("true", 3L)))
                .verifyComplete();
    }
}
