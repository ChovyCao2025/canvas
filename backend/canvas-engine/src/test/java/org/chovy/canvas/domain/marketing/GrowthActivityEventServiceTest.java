package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityEventDO;
import org.chovy.canvas.dal.mapper.GrowthActivityEventMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthActivityEventServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-07T06:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void recordEventInsertsTenantScopedEventWithPayloadAndActor() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.eventMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthActivityEventDO row = invocation.getArgument(0);
            row.setId(1000L);
            return 1;
        }).when(harness.eventMapper).insert(any(GrowthActivityEventDO.class));

        GrowthActivityEventView view = harness.service.recordEvent(7L, 10L, new GrowthActivityEventCommand(
                200L,
                "grant_transition",
                "grant-300-success",
                "reward_grant",
                300L,
                Map.of("status", "SUCCESS")), "operator-1");

        assertThat(view.id()).isEqualTo(1000L);
        assertThat(view.eventType()).isEqualTo("GRANT_TRANSITION");
        assertThat(view.payload()).containsEntry("status", "SUCCESS");
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getTenantId().equals(7L)
                        && row.getActivityId().equals(10L)
                        && row.getParticipantId().equals(200L)
                        && row.getEventType().equals("GRANT_TRANSITION")
                        && row.getEventKey().equals("grant-300-success")
                        && row.getSourceType().equals("REWARD_GRANT")
                        && row.getSourceId().equals(300L)
                        && row.getPayloadJson().contains("SUCCESS")
                        && row.getCreatedBy().equals("operator-1")));
    }

    @Test
    void recordEventReturnsExistingEventForDuplicateKey() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.eventMapper.selectOne(any())).thenReturn(event(1000L, 7L, 10L, "grant-300-success", "GRANT_TRANSITION"));

        GrowthActivityEventView view = harness.service.recordEvent(7L, 10L, new GrowthActivityEventCommand(
                null,
                "grant_transition",
                "grant-300-success",
                "reward_grant",
                300L,
                Map.of()), "operator-1");

        assertThat(view.id()).isEqualTo(1000L);
        verify(harness.eventMapper, never()).insert(any(GrowthActivityEventDO.class));
    }

    @Test
    void typedHelpersCreateStableEventKeys() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.eventMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            GrowthActivityEventDO row = invocation.getArgument(0);
            row.setId(1001L);
            return 1;
        }).when(harness.eventMapper).insert(any(GrowthActivityEventDO.class));

        harness.service.logLifecycle(7L, 10L, "PUBLISHED", "operator-2");
        harness.service.logParticipantEntry(7L, 10L, 200L, "JOINED", Map.of("channel", "app"));
        harness.service.logReferralQualification(7L, 10L, 700L, 200L, Map.of("decision", "PASS"));
        harness.service.logTaskProgress(7L, 10L, 900L, 200L, Map.of("progress", 3));
        harness.service.logGrantTransition(7L, 10L, 300L, 200L, "SUCCESS", Map.of("couponId", "c-1"));
        harness.service.logConversionEvidence(7L, 10L, "order-1", 200L, Map.of("amount", 99));

        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("ACTIVITY_LIFECYCLE")
                        && row.getEventKey().equals("activity:10:lifecycle:PUBLISHED")));
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("PARTICIPANT_ENTRY")
                        && row.getEventKey().equals("activity:10:participant:200:JOINED")));
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("REFERRAL_QUALIFICATION")
                        && row.getEventKey().equals("referral:700:qualified")));
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("TASK_PROGRESS")
                        && row.getEventKey().equals("task-progress:900")));
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("GRANT_TRANSITION")
                        && row.getEventKey().equals("grant:300:SUCCESS")));
        verify(harness.eventMapper).insert(argThat((GrowthActivityEventDO row) ->
                row.getEventType().equals("CONVERSION_EVIDENCE")
                        && row.getEventKey().equals("conversion:order-1")));
    }

    @Test
    void listEventsFiltersTenantActivityAndType() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 7L));
        when(harness.eventMapper.selectList(any())).thenReturn(List.of(
                event(1000L, 7L, 10L, "a", "GRANT_TRANSITION"),
                event(1001L, 8L, 10L, "b", "GRANT_TRANSITION"),
                event(1002L, 7L, 10L, "c", "TASK_PROGRESS")));

        assertThat(harness.service.listEvents(7L, 10L, "grant_transition", 20))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.eventKey()).isEqualTo("a");
                    assertThat(event.eventType()).isEqualTo("GRANT_TRANSITION");
                });
    }

    @Test
    void rejectsForeignActivity() {
        Harness harness = harness();
        when(harness.activityMapper.selectById(10L)).thenReturn(activity(10L, 8L));

        assertThatThrownBy(() -> harness.service.recordEvent(7L, 10L, new GrowthActivityEventCommand(
                null,
                "task_progress",
                "task-progress-1",
                "task_progress",
                900L,
                Map.of()), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth activity does not belong to tenant");
    }

    private static Harness harness() {
        GrowthActivityMapper activityMapper = mock(GrowthActivityMapper.class);
        GrowthActivityEventMapper eventMapper = mock(GrowthActivityEventMapper.class);
        return new Harness(activityMapper, eventMapper,
                new GrowthActivityEventService(activityMapper, eventMapper, new ObjectMapper(), CLOCK));
    }

    private static GrowthActivityDO activity(Long id, Long tenantId) {
        GrowthActivityDO row = new GrowthActivityDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setStatus("ACTIVE");
        return row;
    }

    private static GrowthActivityEventDO event(Long id, Long tenantId, Long activityId, String key, String type) {
        GrowthActivityEventDO row = new GrowthActivityEventDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setActivityId(activityId);
        row.setEventKey(key);
        row.setEventType(type);
        row.setSourceType("REWARD_GRANT");
        row.setSourceId(300L);
        row.setPayloadJson("{}");
        row.setCreatedBy("operator");
        return row;
    }

    private record Harness(
            GrowthActivityMapper activityMapper,
            GrowthActivityEventMapper eventMapper,
            GrowthActivityEventService service) {
    }
}
