package org.chovy.canvas.engine.insights;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.AudienceComputeRunMapper;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MauticInspiredInsightService {

    private static final String STATUS_READY = "READY";

    private final AudienceDefinitionMapper audienceMapper;
    private final AudienceStatMapper audienceStatMapper;
    private final AudienceComputeRunMapper audienceRunMapper;
    private final AudienceBitmapStore audienceBitmapStore;
    private final CanvasExecutionTraceMapper traceMapper;
    private final CustomerChannelMapper channelMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;

    public AudienceMembershipReport explainAudienceMembership(Long audienceId, String userId) {
        AudienceDefinitionDO audience = audienceMapper.selectById(audienceId);
        if (audience == null) {
            return new AudienceMembershipReport(audienceId, null, userId, "UNKNOWN", null, null, null,
                    List.of("audience not found"));
        }
        AudienceStatDO stat = audienceStatMapper.selectById(audienceId);
        AudienceComputeRunDO run = latestAudienceRun(audienceId);
        List<String> evidence = new ArrayList<>();
        evidence.add("rule engine: " + defaultString(audience.getEngineType(), "UNKNOWN"));
        evidence.add("data source: " + defaultString(audience.getDataSourceType(), "UNKNOWN"));
        if (stat != null && stat.getComputedAt() != null) {
            evidence.add("last computed at " + stat.getComputedAt());
        }
        String statStatus = stat == null ? null : stat.getStatus();
        if (!Integer.valueOf(1).equals(audience.getEnabled())) {
            evidence.add("audience disabled");
            return report(audience, userId, "NOT_READY", statStatus, stat, run, evidence);
        }
        if (!STATUS_READY.equalsIgnoreCase(defaultString(statStatus, ""))) {
            evidence.add("audience stat is not READY");
            return report(audience, userId, "NOT_READY", statStatus, stat, run, evidence);
        }
        boolean matched = audienceBitmapStore.isMember(audienceId, userId);
        evidence.add(matched ? "bitmap membership matched" : "bitmap membership missed");
        return report(audience, userId, matched ? "MATCHED" : "NOT_MATCHED", statStatus, stat, run, evidence);
    }

    public JourneyPathReport explainJourneyPath(String executionId) {
        List<JourneyStep> steps = safeList(traceMapper.selectList(new LambdaQueryWrapper<CanvasExecutionTraceDO>()
                .eq(CanvasExecutionTraceDO::getExecutionId, executionId)
                .orderByAsc(CanvasExecutionTraceDO::getStartedAt)
                .orderByAsc(CanvasExecutionTraceDO::getId))).stream()
                .map(this::toJourneyStep)
                .toList();
        int success = (int) steps.stream().filter(step -> step.status() == 1).count();
        int failed = (int) steps.stream().filter(step -> step.status() == 2).count();
        int skipped = (int) steps.stream().filter(step -> step.status() == 3).count();
        return new JourneyPathReport(executionId, steps, success, failed, skipped);
    }

    public ChannelPreferenceReport resolveChannelPreference(String userId, String preferredChannel) {
        List<CustomerChannelDO> channels = safeList(channelMapper.selectList(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getUserId, userId)
                .orderByAsc(CustomerChannelDO::getId)));
        List<MarketingSuppressionDO> suppressions = activeSuppressions(userId);
        Set<String> suppressed = suppressedChannels(suppressions);
        List<ChannelCandidate> candidates = channels.stream()
                .map(channel -> candidate(channel, suppressed))
                .toList();
        String preferred = normalize(preferredChannel);
        String recommended = candidates.stream()
                .filter(candidate -> "ELIGIBLE".equals(candidate.state()))
                .filter(candidate -> candidate.channel().equals(preferred))
                .map(ChannelCandidate::channel)
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> "ELIGIBLE".equals(candidate.state()))
                        .map(ChannelCandidate::channel)
                        .findFirst()
                        .orElse(null));
        String fallback = candidates.stream()
                .filter(candidate -> "ELIGIBLE".equals(candidate.state()))
                .map(ChannelCandidate::channel)
                .filter(channel -> !channel.equals(recommended))
                .findFirst()
                .orElse(null);
        return new ChannelPreferenceReport(userId, recommended, fallback, candidates);
    }

    public SuppressionTimeline suppressionTimeline(String userId) {
        List<SuppressionRecord> records = safeList(suppressionMapper.selectList(new LambdaQueryWrapper<MarketingSuppressionDO>()
                .eq(MarketingSuppressionDO::getUserId, userId)
                .orderByDesc(MarketingSuppressionDO::getCreatedAt)
                .orderByDesc(MarketingSuppressionDO::getId))).stream()
                .sorted(Comparator.comparing((MarketingSuppressionDO row) -> row.getCreatedAt() == null
                        ? LocalDateTime.MIN
                        : row.getCreatedAt()).reversed())
                .map(this::toSuppressionRecord)
                .toList();
        return new SuppressionTimeline(userId, records);
    }

    public PublishHealthReport publishHealth(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            return new PublishHealthReport(canvasId, null, 0, List.of(
                    new HealthCheck("CANVAS_EXISTS", false, "画布不存在")));
        }
        CanvasVersionDO version = canvas.getPublishedVersionId() == null
                ? null
                : canvasVersionMapper.selectById(canvas.getPublishedVersionId());
        String graph = version == null ? null : version.getGraphJson();
        List<HealthCheck> checks = List.of(
                new HealthCheck("CANVAS_ACTIVE", Integer.valueOf(1).equals(canvas.getStatus()), "画布处于发布状态"),
                new HealthCheck("PUBLISHED_VERSION", version != null, "存在已发布版本"),
                new HealthCheck("GRAPH_PRESENT", graph != null && !graph.isBlank(), "发布版本包含图结构"),
                new HealthCheck("TRIGGER_PRESENT", contains(graph, "START") || contains(graph, "triggerType"), "图结构包含触发入口"),
                new HealthCheck("SEND_NODE_PRESENT", contains(graph, "SEND_MESSAGE"), "图结构包含发送节点")
        );
        long failed = checks.stream().filter(check -> !check.passed()).count();
        int score = Math.max(0, 100 - (int) failed * 20);
        return new PublishHealthReport(canvasId, canvas.getName(), score, checks);
    }

    public List<FrequencyTemplate> frequencyTemplates() {
        return List.of(
                new FrequencyTemplate("global_weekly_guard", "GLOBAL", 3, 604_800, "全局 7 天最多 3 次"),
                new FrequencyTemplate("journey_daily_default", "JOURNEY", 1, 86_400, "单旅程每天最多 1 次"),
                new FrequencyTemplate("channel_daily_guard", "CHANNEL", 1, 86_400, "单渠道每天最多 1 次"),
                new FrequencyTemplate("node_once_window", "NODE", 1, 604_800, "关键节点 7 天最多 1 次")
        );
    }

    private AudienceMembershipReport report(AudienceDefinitionDO audience,
                                            String userId,
                                            String status,
                                            String statStatus,
                                            AudienceStatDO stat,
                                            AudienceComputeRunDO run,
                                            List<String> evidence) {
        return new AudienceMembershipReport(
                audience.getId(),
                audience.getName(),
                userId,
                status,
                statStatus,
                stat == null ? null : stat.getEstimatedSize(),
                run == null ? null : run.getStatus(),
                evidence);
    }

    private AudienceComputeRunDO latestAudienceRun(Long audienceId) {
        return safeList(audienceRunMapper.selectList(new LambdaQueryWrapper<AudienceComputeRunDO>()
                .eq(AudienceComputeRunDO::getAudienceId, audienceId)
                .orderByDesc(AudienceComputeRunDO::getUpdatedAt)
                .orderByDesc(AudienceComputeRunDO::getId)
                .last("LIMIT 1"))).stream().findFirst().orElse(null);
    }

    private JourneyStep toJourneyStep(CanvasExecutionTraceDO trace) {
        int status = trace.getStatus() == null ? 0 : trace.getStatus();
        return new JourneyStep(
                trace.getNodeId(),
                trace.getNodeName(),
                trace.getNodeType(),
                status,
                statusLabel(status),
                reason(status),
                trace.getErrorMsg(),
                trace.getDurationMs());
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 1 -> "SUCCESS";
            case 2 -> "FAILED";
            case 3 -> "SKIPPED";
            default -> "RUNNING";
        };
    }

    private String reason(int status) {
        return switch (status) {
            case 1 -> "节点执行成功";
            case 2 -> "节点执行失败";
            case 3 -> "节点未进入当前执行路径或被策略跳过";
            default -> "节点仍在执行或等待写入结果";
        };
    }

    private List<MarketingSuppressionDO> activeSuppressions(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return safeList(suppressionMapper.selectList(new LambdaQueryWrapper<MarketingSuppressionDO>()
                .eq(MarketingSuppressionDO::getUserId, userId)
                .eq(MarketingSuppressionDO::getActive, 1)
                .and(w -> w.isNull(MarketingSuppressionDO::getExpiresAt)
                        .or()
                        .gt(MarketingSuppressionDO::getExpiresAt, now))));
    }

    private Set<String> suppressedChannels(List<MarketingSuppressionDO> suppressions) {
        Set<String> channels = new LinkedHashSet<>();
        for (MarketingSuppressionDO row : suppressions) {
            channels.add(normalize(row.getChannel()));
        }
        return channels;
    }

    private ChannelCandidate candidate(CustomerChannelDO row, Set<String> suppressedChannels) {
        String channel = normalize(row.getChannel());
        String state;
        String reason;
        if (suppressedChannels.contains("ALL") || suppressedChannels.contains(channel)) {
            state = "SUPPRESSED";
            reason = "命中抑制名单";
        } else if (!Integer.valueOf(1).equals(row.getEnabled())
                || row.getAddress() == null
                || row.getAddress().isBlank()) {
            state = "UNAVAILABLE";
            reason = "渠道未启用或地址为空";
        } else {
            state = "ELIGIBLE";
            reason = "渠道可用";
        }
        return new ChannelCandidate(channel, state, reason);
    }

    private SuppressionRecord toSuppressionRecord(MarketingSuppressionDO row) {
        return new SuppressionRecord(
                row.getId(),
                normalize(row.getChannel()),
                row.getReason(),
                suppressionState(row),
                row.getCreatedAt(),
                row.getExpiresAt());
    }

    private String suppressionState(MarketingSuppressionDO row) {
        if (!Integer.valueOf(1).equals(row.getActive())) {
            return "INACTIVE";
        }
        LocalDateTime expiresAt = row.getExpiresAt();
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now()) ? "ACTIVE" : "EXPIRED";
    }

    private boolean contains(String value, String token) {
        return value != null && value.toUpperCase(Locale.ROOT).contains(token.toUpperCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public record AudienceMembershipReport(Long audienceId, String audienceName, String userId, String status,
                                           String statStatus, Long estimatedSize, String latestRunStatus,
                                           List<String> evidence) {
    }

    public record JourneyPathReport(String executionId, List<JourneyStep> steps, int successCount, int failedCount,
                                    int skippedCount) {
    }

    public record JourneyStep(String nodeId, String nodeName, String nodeType, int status, String statusLabel,
                              String reason, String errorMessage, Long durationMs) {
    }

    public record ChannelPreferenceReport(String userId, String recommendedChannel, String fallbackChannel,
                                          List<ChannelCandidate> channels) {
    }

    public record ChannelCandidate(String channel, String state, String reason) {
    }

    public record SuppressionTimeline(String userId, List<SuppressionRecord> records) {
    }

    public record SuppressionRecord(Long id, String channel, String reason, String state, LocalDateTime createdAt,
                                    LocalDateTime expiresAt) {
    }

    public record PublishHealthReport(Long canvasId, String canvasName, int score, List<HealthCheck> checks) {
    }

    public record HealthCheck(String checkKey, boolean passed, String message) {
    }

    public record FrequencyTemplate(String templateKey, String scope, int maxCount, int windowSeconds,
                                    String description) {
    }
}
