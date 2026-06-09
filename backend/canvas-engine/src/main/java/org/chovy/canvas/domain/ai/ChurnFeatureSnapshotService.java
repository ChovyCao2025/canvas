package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
/**
 * ChurnFeatureSnapshotService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ChurnFeatureSnapshotService {

    private static final int MAX_IDLE_DAYS = 60;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EventLogMapper eventLogMapper;
    private final MessageSendRecordMapper messageSendRecordMapper;
    private final CdpUserProfileMapper profileMapper;
    private final AiPredictionProperties properties;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 ChurnFeatureSnapshotService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageSendRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     */
    public ChurnFeatureSnapshotService(EventLogMapper eventLogMapper,
                                       MessageSendRecordMapper messageSendRecordMapper,
                                       CdpUserProfileMapper profileMapper,
                                       AiPredictionProperties properties) {
        this(eventLogMapper, messageSendRecordMapper, profileMapper, properties, Clock.systemDefaultZone());
    }

    /**
     * 初始化 ChurnFeatureSnapshotService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageSendRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param requestedLimit requested limit 参数，用于 candidateUserIds 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    public List<String> candidateUserIds(int requestedLimit) {
        int limit = boundedLimit(requestedLimit);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getStatus, STATUS_ACTIVE)
                        .orderByDesc(CdpUserProfileDO::getLastSeenAt)
                        .last("LIMIT " + limit))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(CdpUserProfileDO::getUserId)
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .distinct()
                .limit(limit)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 extract 流程生成的业务结果。
     */
    public FeatureSnapshot extract(String userId, LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now(clock) : runDate;
        LocalDateTime windowStart = effectiveRunDate.minusDays(30).atStartOfDay();
        LocalDateTime windowEnd = effectiveRunDate.plusDays(1).atStartOfDay();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long failedSends = sends.stream()
                .filter(send -> MessageSendRecordDO.STATUS_FAILED.equalsIgnoreCase(send.getStatus()))
                .count();
        double failureRate = sendCount == 0 ? 0.0d : (double) failedSends / sendCount;
        int goalCount = (int) events.stream().filter(this::isGoalEvent).count();
        int daysSinceLastEvent = daysSinceLastEvent(events, effectiveRunDate);
        int profileAgeDays = profileAgeDays(profile, effectiveRunDate);
        boolean sparse = eventCount < Math.max(1, properties.getSparseHistoryMinEvents());

        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<EventLogDO> recentEvents(String userId, LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? LocalDate.now(clock) : runDate;
        LocalDateTime windowStart = effectiveRunDate.minusDays(30).atStartOfDay();
        LocalDateTime windowEnd = effectiveRunDate.plusDays(1).atStartOfDay();
        return eventLogMapper.selectList(new LambdaQueryWrapper<EventLogDO>()
                .eq(EventLogDO::getUserId, userId)
                .ge(EventLogDO::getCreatedAt, windowStart)
                .lt(EventLogDO::getCreatedAt, windowEnd));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param events events 参数，用于 daysSinceLastEvent 流程中的校验、计算或对象转换。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 days since last event 计算得到的数量、金额或指标值。
     */
    private int daysSinceLastEvent(List<EventLogDO> events, LocalDate runDate) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return events.stream()
                .map(EventLogDO::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(lastSeen -> Math.max(0, (int) ChronoUnit.DAYS.between(lastSeen.toLocalDate(), runDate)))
                .orElse(MAX_IDLE_DAYS);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param profile profile 参数，用于 profileAgeDays 流程中的校验、计算或对象转换。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 profile age days 计算得到的数量、金额或指标值。
     */
    private int profileAgeDays(CdpUserProfileDO profile, LocalDate runDate) {
        if (profile == null || profile.getFirstSeenAt() == null) {
            return 0;
        }
        return Math.max(0, (int) ChronoUnit.DAYS.between(profile.getFirstSeenAt().toLocalDate(), runDate));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param event event 参数，用于 isGoalEvent 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isGoalEvent(EventLogDO event) {
        String eventCode = event.getEventCode() == null ? "" : event.getEventCode().toLowerCase();
        if (eventCode.contains("goal") || eventCode.contains("conversion") || eventCode.contains("purchase")) {
            return true;
        }
        String attributes = event.getAttributes() == null ? "" : event.getAttributes().toLowerCase();
        return attributes.contains("\"goal\"") || attributes.contains("\"conversion\"");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param requestedLimit requested limit 参数，用于 boundedLimit 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int requestedLimit) {
        int configured = Math.max(1, properties.getBatchSize());
        int requested = requestedLimit <= 0 ? configured : requestedLimit;
        return Math.max(1, Math.min(requested, configured));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requestedLimit requested limit 参数，用于 uniqueUserIdsFromEventsAndProfiles 流程中的校验、计算或对象转换。
     * @return 返回 unique user ids from events and profiles 汇总后的集合、分页或映射视图。
     */
    public Set<String> uniqueUserIdsFromEventsAndProfiles(int requestedLimit) {
        Set<String> userIds = new LinkedHashSet<>(candidateUserIds(requestedLimit));
        return Set.copyOf(userIds);
    }

    /**
     * FeatureSnapshot 承载对应领域的业务规则、流程编排和结果转换。
     */
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
