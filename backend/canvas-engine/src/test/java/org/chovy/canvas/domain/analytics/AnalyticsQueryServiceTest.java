package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
