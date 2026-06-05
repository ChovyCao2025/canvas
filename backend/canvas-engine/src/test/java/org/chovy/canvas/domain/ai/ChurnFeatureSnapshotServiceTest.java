package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChurnFeatureSnapshotServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void extractsDaysSinceLastEventAndThirtyDayEventCount() {
        Fixture fixture = fixture();
        when(fixture.profileMapper.selectOne(any(Wrapper.class))).thenReturn(profile("u1"));
        when(fixture.eventLogMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                event("u1", "browse", LocalDateTime.of(2026, 6, 1, 10, 0)),
                event("u1", "purchase", LocalDateTime.of(2026, 6, 3, 20, 0))));
        when(fixture.messageSendRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        var snapshot = fixture.service.extract("u1", LocalDate.of(2026, 6, 4));

        assertThat(snapshot.daysSinceLastEvent()).isEqualTo(1);
        assertThat(snapshot.eventCount30d()).isEqualTo(2);
        assertThat(snapshot.goalCount30d()).isEqualTo(1);
    }

    @Test
    void extractsThirtyDaySendCountAndFailureRate() {
        Fixture fixture = fixture();
        when(fixture.profileMapper.selectOne(any(Wrapper.class))).thenReturn(profile("u1"));
        when(fixture.eventLogMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                event("u1", "browse", LocalDateTime.of(2026, 6, 1, 10, 0)),
                event("u1", "browse", LocalDateTime.of(2026, 6, 2, 10, 0)),
                event("u1", "browse", LocalDateTime.of(2026, 6, 3, 10, 0))));
        when(fixture.messageSendRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                send("u1", MessageSendRecordDO.STATUS_SENT),
                send("u1", MessageSendRecordDO.STATUS_FAILED),
                send("u1", MessageSendRecordDO.STATUS_FAILED),
                send("u1", MessageSendRecordDO.STATUS_SKIPPED)));

        var snapshot = fixture.service.extract("u1", LocalDate.of(2026, 6, 4));

        assertThat(snapshot.sendCount30d()).isEqualTo(4);
        assertThat(snapshot.deliveryFailureRate30d()).isEqualTo(0.5d);
    }

    @Test
    void marksSparseHistoryWhenUserHasFewerThanThreeEvents() {
        Fixture fixture = fixture();
        when(fixture.profileMapper.selectOne(any(Wrapper.class))).thenReturn(profile("u1"));
        when(fixture.eventLogMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                event("u1", "browse", LocalDateTime.of(2026, 6, 3, 10, 0))));
        when(fixture.messageSendRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(fixture.service.extract("u1", LocalDate.of(2026, 6, 4)).sparseHistory()).isTrue();
    }

    @Test
    void capsBatchByConfiguredLimit() {
        Fixture fixture = fixture();
        when(fixture.profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile("u1"), profile("u2"), profile("u3")));

        List<String> userIds = fixture.service.candidateUserIds(100);

        assertThat(userIds).containsExactly("u1", "u2");
    }

    private static Fixture fixture() {
        EventLogMapper eventLogMapper = mock(EventLogMapper.class);
        MessageSendRecordMapper messageSendRecordMapper = mock(MessageSendRecordMapper.class);
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setBatchSize(2);
        properties.setSparseHistoryMinEvents(3);
        return new Fixture(eventLogMapper, messageSendRecordMapper, profileMapper,
                new ChurnFeatureSnapshotService(eventLogMapper, messageSendRecordMapper, profileMapper, properties, CLOCK));
    }

    private static CdpUserProfileDO profile(String userId) {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId(userId);
        profile.setStatus("ACTIVE");
        profile.setFirstSeenAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        profile.setLastSeenAt(LocalDateTime.of(2026, 6, 3, 0, 0));
        return profile;
    }

    private static EventLogDO event(String userId, String eventCode, LocalDateTime createdAt) {
        EventLogDO event = new EventLogDO();
        event.setUserId(userId);
        event.setEventCode(eventCode);
        event.setCreatedAt(createdAt);
        return event;
    }

    private static MessageSendRecordDO send(String userId, String status) {
        MessageSendRecordDO send = new MessageSendRecordDO();
        send.setUserId(userId);
        send.setStatus(status);
        send.setCreatedAt(LocalDateTime.of(2026, 6, 3, 0, 0));
        return send;
    }

    private record Fixture(
            EventLogMapper eventLogMapper,
            MessageSendRecordMapper messageSendRecordMapper,
            CdpUserProfileMapper profileMapper,
            ChurnFeatureSnapshotService service) {
    }
}
