package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ChurnFeatureSnapshotService {

    private static final int MAX_IDLE_DAYS = 60;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EventLogMapper eventLogMapper;
    private final MessageSendRecordMapper messageSendRecordMapper;
    private final CdpUserProfileMapper profileMapper;
    private final AiPredictionProperties properties;
    private final Clock clock;

    public ChurnFeatureSnapshotService(EventLogMapper eventLogMapper,
                                       MessageSendRecordMapper messageSendRecordMapper,
                                       CdpUserProfileMapper profileMapper,
                                       AiPredictionProperties properties) {
        this(eventLogMapper, messageSendRecordMapper, profileMapper, properties, Clock.systemDefaultZone());
    }

    ChurnFeatureSnapshotService(EventLogMapper eventLogMapper,
                                MessageSendRecordMapper messageSendRecordMapper,
                                CdpUserProfileMapper profileMapper,
                                AiPredictionProperties properties,
                                Clock clock) {
        this.eventLogMapper = eventLogMapper;
        this.messageSendRecordMapper = messageSendRecordMapper;
        this.profileMapper = profileMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public List<String> candidateUserIds(int requestedLimit) {
        int limit = boundedLimit(requestedLimit);
        return profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getStatus, STATUS_ACTIVE)
                        .orderByDesc(CdpUserProfileDO::getLastSeenAt)
                        .last("LIMIT " + limit))
                .stream()
                .map(CdpUserProfileDO::getUserId)
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .distinct()
                .limit(limit)
                .toList();
    }

    public FeatureSnapshot extract(String userId, LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now(clock) : runDate;
        LocalDateTime windowStart = effectiveRunDate.minusDays(30).atStartOfDay();
        LocalDateTime windowEnd = effectiveRunDate.plusDays(1).atStartOfDay();
        CdpUserProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        List<EventLogDO> events = eventLogMapper.selectList(new LambdaQueryWrapper<EventLogDO>()
                .eq(EventLogDO::getUserId, userId)
                .ge(EventLogDO::getCreatedAt, windowStart)
                .lt(EventLogDO::getCreatedAt, windowEnd));
        List<MessageSendRecordDO> sends = messageSendRecordMapper.selectList(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getUserId, userId)
                .ge(MessageSendRecordDO::getCreatedAt, windowStart)
                .lt(MessageSendRecordDO::getCreatedAt, windowEnd));

        int eventCount = events.size();
        int sendCount = sends.size();
        long failedSends = sends.stream()
                .filter(send -> MessageSendRecordDO.STATUS_FAILED.equalsIgnoreCase(send.getStatus()))
                .count();
        double failureRate = sendCount == 0 ? 0.0d : (double) failedSends / sendCount;
        int goalCount = (int) events.stream().filter(this::isGoalEvent).count();
        int daysSinceLastEvent = daysSinceLastEvent(events, effectiveRunDate);
        int profileAgeDays = profileAgeDays(profile, effectiveRunDate);
        boolean sparse = eventCount < Math.max(1, properties.getSparseHistoryMinEvents());

        return new FeatureSnapshot(
                userId,
                daysSinceLastEvent,
                eventCount,
                sendCount,
                failureRate,
                goalCount,
                profileAgeDays,
                sparse);
    }

    public List<EventLogDO> recentEvents(String userId, LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now(clock) : runDate;
        LocalDateTime windowStart = effectiveRunDate.minusDays(30).atStartOfDay();
        LocalDateTime windowEnd = effectiveRunDate.plusDays(1).atStartOfDay();
        return eventLogMapper.selectList(new LambdaQueryWrapper<EventLogDO>()
                .eq(EventLogDO::getUserId, userId)
                .ge(EventLogDO::getCreatedAt, windowStart)
                .lt(EventLogDO::getCreatedAt, windowEnd));
    }

    private int daysSinceLastEvent(List<EventLogDO> events, LocalDate runDate) {
        return events.stream()
                .map(EventLogDO::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(lastSeen -> Math.max(0, (int) ChronoUnit.DAYS.between(lastSeen.toLocalDate(), runDate)))
                .orElse(MAX_IDLE_DAYS);
    }

    private int profileAgeDays(CdpUserProfileDO profile, LocalDate runDate) {
        if (profile == null || profile.getFirstSeenAt() == null) {
            return 0;
        }
        return Math.max(0, (int) ChronoUnit.DAYS.between(profile.getFirstSeenAt().toLocalDate(), runDate));
    }

    private boolean isGoalEvent(EventLogDO event) {
        String eventCode = event.getEventCode() == null ? "" : event.getEventCode().toLowerCase();
        if (eventCode.contains("goal") || eventCode.contains("conversion") || eventCode.contains("purchase")) {
            return true;
        }
        String attributes = event.getAttributes() == null ? "" : event.getAttributes().toLowerCase();
        return attributes.contains("\"goal\"") || attributes.contains("\"conversion\"");
    }

    private int boundedLimit(int requestedLimit) {
        int configured = Math.max(1, properties.getBatchSize());
        int requested = requestedLimit <= 0 ? configured : requestedLimit;
        return Math.max(1, Math.min(requested, configured));
    }

    public Set<String> uniqueUserIdsFromEventsAndProfiles(int requestedLimit) {
        Set<String> userIds = new LinkedHashSet<>(candidateUserIds(requestedLimit));
        return Set.copyOf(userIds);
    }

    public record FeatureSnapshot(
            String userId,
            int daysSinceLastEvent,
            int eventCount30d,
            int sendCount30d,
            double deliveryFailureRate30d,
            int goalCount30d,
            int profileAgeDays,
            boolean sparseHistory) {
    }
}
