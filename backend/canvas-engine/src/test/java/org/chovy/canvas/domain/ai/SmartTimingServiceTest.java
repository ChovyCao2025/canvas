package org.chovy.canvas.domain.ai;

import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartTimingServiceTest {

    @Test
    void choosesMostCommonRecentEngagementHour() {
        ChurnFeatureSnapshotService featureSnapshotService = mock(ChurnFeatureSnapshotService.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setDefaultBestSendHour(20);
        properties.setSparseHistoryMinEvents(3);
        SmartTimingService service = new SmartTimingService(featureSnapshotService, properties);
        LocalDate runDate = LocalDate.of(2026, 6, 4);
        when(featureSnapshotService.recentEvents("u1", runDate)).thenReturn(List.of(
                event(9),
                event(20),
                event(20),
                event(9),
                event(20)));

        assertThat(service.bestSendHour("u1", runDate)).isEqualTo(20);
    }

    @Test
    void fallsBackToConfiguredDefaultForSparseHistoryAndNormalizesDefaultHour() {
        ChurnFeatureSnapshotService featureSnapshotService = mock(ChurnFeatureSnapshotService.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setDefaultBestSendHour(27);
        properties.setSparseHistoryMinEvents(3);
        SmartTimingService service = new SmartTimingService(featureSnapshotService, properties);
        LocalDate runDate = LocalDate.of(2026, 6, 4);
        when(featureSnapshotService.recentEvents("u1", runDate)).thenReturn(List.of(event(8), event(9)));

        assertThat(service.bestSendHour("u1", runDate)).isEqualTo(23);
    }

    @Test
    void tieBreaksTowardEarlierHourForOperationalStability() {
        ChurnFeatureSnapshotService featureSnapshotService = mock(ChurnFeatureSnapshotService.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setDefaultBestSendHour(20);
        properties.setSparseHistoryMinEvents(2);
        SmartTimingService service = new SmartTimingService(featureSnapshotService, properties);
        LocalDate runDate = LocalDate.of(2026, 6, 4);
        when(featureSnapshotService.recentEvents("u1", runDate)).thenReturn(List.of(
                event(10),
                event(22),
                event(10),
                event(22)));

        assertThat(service.bestSendHour("u1", runDate)).isEqualTo(10);
    }

    private static EventLogDO event(int hour) {
        EventLogDO event = new EventLogDO();
        event.setUserId("u1");
        event.setCreatedAt(LocalDateTime.of(2026, 6, 3, hour, 0));
        return event;
    }
}
