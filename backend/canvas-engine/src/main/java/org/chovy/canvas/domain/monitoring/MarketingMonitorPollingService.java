package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingCompetitorMentionDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorPollRunDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorTrendSnapshotDO;
import org.chovy.canvas.dal.dataobject.MarketingSentimentAnalysisDO;
import org.chovy.canvas.dal.mapper.MarketingCompetitorMentionMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorPollRunMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorTrendSnapshotMapper;
import org.chovy.canvas.dal.mapper.MarketingSentimentAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MarketingMonitorPollingService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorPollingService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitorItemMapper itemMapper;
    private final MarketingSentimentAnalysisMapper sentimentMapper;
    private final MarketingCompetitorMentionMapper competitorMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final MarketingMonitorPollRunMapper runMapper;
    private final MarketingMonitorTrendSnapshotMapper trendMapper;
    private final MarketingMonitoringService monitoringService;
    private final List<MarketingMonitorPollClient> pollClients;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 MarketingMonitorPollingService 实例并注入 domain.monitoring 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sentimentMapper 时间参数，用于计算窗口、过期或审计时间。
     * @param competitorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param trendMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param monitoringService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollClients 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingMonitorPollingService(MarketingMonitorSourceMapper sourceMapper,
                                          MarketingMonitorItemMapper itemMapper,
                                          MarketingSentimentAnalysisMapper sentimentMapper,
                                          MarketingCompetitorMentionMapper competitorMapper,
                                          MarketingMonitorAlertMapper alertMapper,
                                          MarketingMonitorPollRunMapper runMapper,
                                          MarketingMonitorTrendSnapshotMapper trendMapper,
                                          MarketingMonitoringService monitoringService,
                                          List<MarketingMonitorPollClient> pollClients,
                                          ObjectMapper objectMapper) {
        this(sourceMapper, itemMapper, sentimentMapper, competitorMapper, alertMapper, runMapper, trendMapper,
                monitoringService, pollClients, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorPollingService 流程，围绕 marketing monitor polling service 完成校验、计算或结果组装。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sentimentMapper 时间参数，用于计算窗口、过期或审计时间。
     * @param competitorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param trendMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param monitoringService 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollClients 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorPollingService(MarketingMonitorSourceMapper sourceMapper,
                                   MarketingMonitorItemMapper itemMapper,
                                   MarketingSentimentAnalysisMapper sentimentMapper,
                                   MarketingCompetitorMentionMapper competitorMapper,
                                   MarketingMonitorAlertMapper alertMapper,
                                   MarketingMonitorPollRunMapper runMapper,
                                   MarketingMonitorTrendSnapshotMapper trendMapper,
                                   MarketingMonitoringService monitoringService,
                                   List<MarketingMonitorPollClient> pollClients,
                                   ObjectMapper objectMapper,
                                   Clock clock) {
        this.sourceMapper = sourceMapper;
        this.itemMapper = itemMapper;
        this.sentimentMapper = sentimentMapper;
        this.competitorMapper = competitorMapper;
        this.alertMapper = alertMapper;
        this.runMapper = runMapper;
        this.trendMapper = trendMapper;
        this.monitoringService = monitoringService;
        this.pollClients = pollClients == null ? List.of() : List.copyOf(pollClients);
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 configurePolling，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param sourceId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorSourcePollingView configurePolling(Long tenantId,
                                                              Long sourceId,
                                                              MarketingMonitorSourcePollingCommand command,
                                                              String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("monitoring source polling command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorSourceDO source = requireSource(scopedTenantId, sourceId);
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO update = new MarketingMonitorSourceDO();
        update.setId(source.getId());
        update.setPollEnabled(Boolean.TRUE.equals(command.pollEnabled()) ? 1 : 0);
        update.setPollIntervalMinutes(positive(command.pollIntervalMinutes(), 60));
        update.setPollCursor(trimToNull(command.pollCursor()));
        update.setNextPollAt(command.nextPollAt());
        update.setUpdatedAt(changedAt);
        sourceMapper.updateById(update);

        source.setPollEnabled(update.getPollEnabled());
        source.setPollIntervalMinutes(update.getPollIntervalMinutes());
        source.setPollCursor(update.getPollCursor());
        source.setNextPollAt(update.getNextPollAt());
        source.setUpdatedAt(changedAt);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toPollingView(source);
    }

    /**
     * 执行业务操作 pollSource，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param sourceId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorPollRunView pollSource(Long tenantId,
                                                  Long sourceId,
                                                  MarketingMonitorPollCommand command,
                                                  String actor) {
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorSourceDO source = requireSource(scopedTenantId, sourceId);
        if (!enabled(source.getEnabled())) {
            throw new IllegalStateException("monitor source is disabled");
        }
        boolean force = command != null && command.force();
        if (!force && !enabled(source.getPollEnabled())) {
            throw new IllegalStateException("monitor source polling is disabled");
        }
        MarketingMonitorPollClient client = clientFor(source.getSourceType());
        LocalDateTime startedAt = now();
        String cursorBefore = cursor(command, source);
        MarketingMonitorPollRunDO run = runningRun(source, command, cursorBefore, actor, startedAt);
        runMapper.insert(run);
        try {
            MarketingMonitorPollResponse response = client.fetch(new MarketingMonitorPollRequest(
                    scopedTenantId,
                    source.getId(),
                    source.getSourceKey(),
                    source.getSourceType(),
                    cursorBefore,
                    command == null ? null : command.requestedFrom(),
                    command == null ? null : command.requestedUntil(),
                    maxItems(command),
                    map(source.getMetadataJson())));
            PollCounts counts = ingestPolledItems(scopedTenantId, source, response, actor, maxItems(command));
            LocalDateTime finishedAt = now();
            String cursorAfter = trimToNull(response == null ? null : response.nextCursor());
            completeRun(run, counts, cursorAfter, response == null ? Map.of() : response.metadata(), finishedAt);
            updateSourceAfterPoll(source, "COMPLETED", cursorAfter, finishedAt);
            return toRunView(run);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = now();
            failRun(run, ex, finishedAt);
            updateSourceAfterPoll(source, "FAILED", null, finishedAt);
            return toRunView(run);
        }
    }

    /**
     * 执行业务操作 buildTrendSnapshot，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorTrendSnapshotView buildTrendSnapshot(Long tenantId,
                                                                MarketingMonitorTrendSnapshotCommand command,
                                                                String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("monitoring trend snapshot command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorSourceDO source = requireSource(scopedTenantId, command.sourceId());
        LocalDateTime bucketStart = requiredTime(command.bucketStart(), "bucketStart");
        LocalDateTime bucketEnd = requiredTime(command.bucketEnd(), "bucketEnd");
        if (!bucketEnd.isAfter(bucketStart)) {
            throw new IllegalArgumentException("bucketEnd must be after bucketStart");
        }
        String brandKey = normalizeOptionalKey(command.brandKey());
        String competitorKey = normalizeOptionalKey(command.competitorKey());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MarketingMonitorItemDO> items = safeList(itemMapper.selectList(new LambdaQueryWrapper<MarketingMonitorItemDO>()
                .eq(MarketingMonitorItemDO::getTenantId, scopedTenantId)
                .eq(MarketingMonitorItemDO::getSourceId, source.getId())));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<MarketingMonitorItemDO> scopedItems = items.stream()
                .filter(item -> scopedTenantId.equals(item.getTenantId()))
                .filter(item -> Objects.equals(source.getId(), item.getSourceId()))
                .filter(item -> inWindow(itemTime(item), bucketStart, bucketEnd))
                .filter(item -> brandKey == null || brandKey.equals(normalizeOptionalKey(item.getBrandKey())))
                .toList();
        Set<Long> itemIds = scopedItems.stream()
                .map(MarketingMonitorItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MarketingSentimentAnalysisDO> sentiments = safeList(sentimentMapper.selectList(
                new LambdaQueryWrapper<MarketingSentimentAnalysisDO>()
                        .eq(MarketingSentimentAnalysisDO::getTenantId, scopedTenantId)))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> itemIds.contains(row.getItemId()))
                .collect(Collectors.toMap(MarketingSentimentAnalysisDO::getItemId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<MarketingCompetitorMentionDO> competitors = safeList(competitorMapper.selectList(
                new LambdaQueryWrapper<MarketingCompetitorMentionDO>()
                        .eq(MarketingCompetitorMentionDO::getTenantId, scopedTenantId)))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> itemIds.contains(row.getItemId()))
                .filter(row -> competitorKey == null || competitorKey.equals(normalizeOptionalKey(row.getCompetitorKey())))
                .toList();
        List<MarketingMonitorAlertDO> alerts = safeList(alertMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> inWindow(row.getCreatedAt(), bucketStart, bucketEnd))
                .filter(row -> brandKey == null || brandKey.equals(normalizeOptionalKey(row.getScopeKey())))
                .toList();

        LocalDateTime createdAt = now();
        MarketingMonitorTrendSnapshotDO snapshot = new MarketingMonitorTrendSnapshotDO();
        snapshot.setTenantId(scopedTenantId);
        snapshot.setSourceId(source.getId());
        snapshot.setSourceKey(source.getSourceKey());
        snapshot.setBucketGrain(normalizeGrain(command.bucketGrain()));
        snapshot.setBucketStart(bucketStart);
        snapshot.setBucketEnd(bucketEnd);
        snapshot.setBrandKey(brandKey);
        snapshot.setCompetitorKey(competitorKey);
        snapshot.setMentionCount(scopedItems.size());
        snapshot.setPositiveCount(sentimentCount(sentiments, "POSITIVE"));
        snapshot.setNeutralCount(sentimentCount(sentiments, "NEUTRAL"));
        snapshot.setNegativeCount(sentimentCount(sentiments, "NEGATIVE"));
        snapshot.setCompetitorCount(competitors.size());
        snapshot.setAlertCount(alerts.size());
        snapshot.setAvgSentimentScore(averageSentiment(scopedItems, sentiments));
        snapshot.setMetadataJson(json(command.metadata()));
        snapshot.setCreatedBy(actor(actor));
        snapshot.setCreatedAt(createdAt);
        snapshot.setUpdatedAt(createdAt);
        trendMapper.insert(snapshot);
        return toTrendView(snapshot);
    }

    /**
     * 执行业务操作 trendSnapshots，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 trendSnapshots 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorTrendSnapshotView> trendSnapshots(Long tenantId,
                                                                  MarketingMonitorTrendSnapshotQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorTrendSnapshotQuery effectiveQuery = query == null
                ? new MarketingMonitorTrendSnapshotQuery(null, null, null, 50)
                : query;
        LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO> wrapper =
                new LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO>()
                        .eq(MarketingMonitorTrendSnapshotDO::getTenantId, scopedTenantId)
                        .orderByDesc(MarketingMonitorTrendSnapshotDO::getBucketStart)
                        .last("LIMIT " + boundedLimit(effectiveQuery.limit()));
        // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
        if (effectiveQuery.sourceId() != null) {
            wrapper.eq(MarketingMonitorTrendSnapshotDO::getSourceId, effectiveQuery.sourceId());
        }
        String brandKey = normalizeOptionalKey(effectiveQuery.brandKey());
        if (brandKey != null) {
            wrapper.eq(MarketingMonitorTrendSnapshotDO::getBrandKey, brandKey);
        }
        String competitorKey = normalizeOptionalKey(effectiveQuery.competitorKey());
        if (competitorKey != null) {
            wrapper.eq(MarketingMonitorTrendSnapshotDO::getCompetitorKey, competitorKey);
        }
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(trendMapper.selectList(wrapper)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> effectiveQuery.sourceId() == null || effectiveQuery.sourceId().equals(row.getSourceId()))
                .filter(row -> brandKey == null || brandKey.equals(normalizeOptionalKey(row.getBrandKey())))
                .filter(row -> competitorKey == null || competitorKey.equals(normalizeOptionalKey(row.getCompetitorKey())))
                .map(this::toTrendView)
                .toList();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireSource 流程生成的业务结果。
     */
    private MarketingMonitorSourceDO requireSource(Long tenantId, Long sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId is required");
        }
        MarketingMonitorSourceDO source = sourceMapper.selectById(sourceId);
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("monitor source is not found");
        }
        return source;
    }

    /**
     * 执行 clientFor 流程，围绕 client for 完成校验、计算或结果组装。
     *
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @return 返回 clientFor 流程生成的业务结果。
     */
    private MarketingMonitorPollClient clientFor(String sourceType) {
        return pollClients.stream()
                .filter(client -> client.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no monitor poll client supports source type: " + sourceType));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param source source 参数，用于 runningRun 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param cursorBefore cursor before 参数，用于 runningRun 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private MarketingMonitorPollRunDO runningRun(MarketingMonitorSourceDO source,
                                                 MarketingMonitorPollCommand command,
                                                 String cursorBefore,
                                                 String actor,
                                                 LocalDateTime startedAt) {
        // 准备本次处理所需的上下文和中间变量。
        MarketingMonitorPollRunDO run = new MarketingMonitorPollRunDO();
        run.setTenantId(source.getTenantId());
        run.setSourceId(source.getId());
        run.setSourceKey(source.getSourceKey());
        run.setSourceType(source.getSourceType());
        run.setStatus("RUNNING");
        run.setRequestedFrom(command == null ? null : command.requestedFrom());
        run.setRequestedUntil(command == null ? null : command.requestedUntil());
        run.setCursorBefore(cursorBefore);
        run.setItemCount(0);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        run.setInsertedCount(0);
        run.setDuplicateCount(0);
        run.setAlertCount(0);
        run.setMetadataJson("{}");
        run.setCreatedBy(actor(actor));
        run.setStartedAt(startedAt);
        run.setCreatedAt(startedAt);
        run.setUpdatedAt(startedAt);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return run;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 ingestPolledItems 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 ingestPolledItems 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param maxItems max items 参数，用于 ingestPolledItems 流程中的校验、计算或对象转换。
     * @return 返回 ingestPolledItems 流程生成的业务结果。
     */
    private PollCounts ingestPolledItems(Long tenantId,
                                         MarketingMonitorSourceDO source,
                                         MarketingMonitorPollResponse response,
                                         String actor,
                                         int maxItems) {
        int itemCount = 0;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        int insertedCount = 0;
        int duplicateCount = 0;
        int alertCount = 0;
        Map<String, List<String>> competitors = competitors(source);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (MarketingMonitorPollItem item : safeList(response == null ? null : response.items()).stream()
                .limit(maxItems)
                .toList()) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (item == null || !hasText(item.externalItemId())) {
                continue;
            }
            itemCount++;
            MarketingMonitorItemDO existing = itemMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorItemDO>()
                    .eq(MarketingMonitorItemDO::getTenantId, tenantId)
                    .eq(MarketingMonitorItemDO::getSourceId, source.getId())
                    .eq(MarketingMonitorItemDO::getExternalItemId, item.externalItemId())
                    .last("LIMIT 1"));
            if (existing != null && tenantId.equals(existing.getTenantId())) {
                duplicateCount++;
                continue;
            }
            MarketingMonitorIngestResult result = monitoringService.ingestItem(tenantId,
                    new MarketingMonitorItemIngestCommand(
                            source.getId(),
                            item.externalItemId(),
                            item.sourceUrl(),
                            item.authorKey(),
                            item.brandKey(),
                            item.text(),
                            item.language(),
                            item.publishedAt(),
                            competitors,
                            item.rawPayload()),
                    actor(actor));
            insertedCount++;
            alertCount += result == null || result.alerts() == null ? 0 : result.alerts().size();
        }
        return new PollCounts(itemCount, insertedCount, duplicateCount, alertCount);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param run run 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param counts counts 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param cursorAfter cursor after 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param finishedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void completeRun(MarketingMonitorPollRunDO run,
                             PollCounts counts,
                             String cursorAfter,
                             Map<String, Object> metadata,
                             LocalDateTime finishedAt) {
        // 准备本次处理所需的上下文和中间变量。
        run.setStatus("COMPLETED");
        run.setCursorAfter(cursorAfter);
        run.setItemCount(counts.itemCount());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        run.setInsertedCount(counts.insertedCount());
        run.setDuplicateCount(counts.duplicateCount());
        run.setAlertCount(counts.alertCount());
        run.setMetadataJson(json(metadata));
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        MarketingMonitorPollRunDO update = new MarketingMonitorPollRunDO();
        update.setId(run.getId());
        update.setStatus(run.getStatus());
        update.setCursorAfter(run.getCursorAfter());
        update.setItemCount(run.getItemCount());
        update.setInsertedCount(run.getInsertedCount());
        update.setDuplicateCount(run.getDuplicateCount());
        update.setAlertCount(run.getAlertCount());
        update.setMetadataJson(run.getMetadataJson());
        update.setFinishedAt(finishedAt);
        update.setUpdatedAt(finishedAt);
        runMapper.updateById(update);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param run run 参数，用于 failRun 流程中的校验、计算或对象转换。
     * @param ex ex 参数，用于 failRun 流程中的校验、计算或对象转换。
     * @param finishedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void failRun(MarketingMonitorPollRunDO run, RuntimeException ex, LocalDateTime finishedAt) {
        // 准备本次处理所需的上下文和中间变量。
        run.setStatus("FAILED");
        run.setErrorMessage(message(ex));
        run.setFinishedAt(finishedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        run.setUpdatedAt(finishedAt);
        MarketingMonitorPollRunDO update = new MarketingMonitorPollRunDO();
        update.setId(run.getId());
        update.setStatus(run.getStatus());
        update.setErrorMessage(run.getErrorMessage());
        update.setFinishedAt(finishedAt);
        update.setUpdatedAt(finishedAt);
        runMapper.updateById(update);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param source source 参数，用于 updateSourceAfterPoll 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param cursorAfter cursor after 参数，用于 updateSourceAfterPoll 流程中的校验、计算或对象转换。
     * @param changedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void updateSourceAfterPoll(MarketingMonitorSourceDO source,
                                       String status,
                                       String cursorAfter,
                                       LocalDateTime changedAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO update = new MarketingMonitorSourceDO();
        update.setId(source.getId());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("COMPLETED".equals(status) && hasText(cursorAfter)) {
            update.setPollCursor(cursorAfter);
            source.setPollCursor(cursorAfter);
        }
        update.setLastPolledAt(changedAt);
        update.setLastPollStatus(status);
        update.setNextPollAt(changedAt.plusMinutes(positive(source.getPollIntervalMinutes(), 60)));
        update.setUpdatedAt(changedAt);
        sourceMapper.updateById(update);
        source.setLastPolledAt(changedAt);
        source.setLastPollStatus(status);
        source.setNextPollAt(update.getNextPollAt());
        source.setUpdatedAt(changedAt);
    }

    /**
     * 执行 competitors 流程，围绕 competitors 完成校验、计算或结果组装。
     *
     * @param source source 参数，用于 competitors 流程中的校验、计算或对象转换。
     * @return 返回 competitors 汇总后的集合、分页或映射视图。
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> competitors(MarketingMonitorSourceDO source) {
        Object value = map(source.getMetadataJson()).get("competitors");
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = normalizeOptionalKey(entry.getKey() == null ? null : String.valueOf(entry.getKey()));
            if (key == null) {
                continue;
            }
            Object terms = entry.getValue();
            if (terms instanceof List<?> list) {
                result.put(key, list.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .filter(this::hasText)
                        .toList());
            // 根据前序判断结果进入后续条件分支。
            } else if (terms instanceof String text && hasText(text)) {
                result.put(key, singleList(text));
            }
        }
        return result;
    }

    /**
     * 执行 sentimentCount 流程，围绕 sentiment count 完成校验、计算或结果组装。
     *
     * @param Long long 参数，用于 sentimentCount 流程中的校验、计算或对象转换。
     * @param sentiments 时间参数，用于计算窗口、过期或审计时间。
     * @param label label 参数，用于 sentimentCount 流程中的校验、计算或对象转换。
     * @return 返回 sentiment count 计算得到的数量、金额或指标值。
     */
    private int sentimentCount(Map<Long, MarketingSentimentAnalysisDO> sentiments, String label) {
        int count = 0;
        for (MarketingSentimentAnalysisDO sentiment : sentiments.values()) {
            if (label.equals(normalizeOptionalUpper(sentiment.getSentimentLabel()))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 执行 averageSentiment 流程，围绕 average sentiment 完成校验、计算或结果组装。
     *
     * @param items items 参数，用于 averageSentiment 流程中的校验、计算或对象转换。
     * @param sentiments 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 average sentiment 计算得到的数量、金额或指标值。
     */
    private BigDecimal averageSentiment(List<MarketingMonitorItemDO> items,
                                        Map<Long, MarketingSentimentAnalysisDO> sentiments) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (items.isEmpty()) {
            return scaled(0);
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (MarketingMonitorItemDO item : items) {
            MarketingSentimentAnalysisDO sentiment = sentiments.get(item.getId());
            if (sentiment != null && sentiment.getSentimentScore() != null) {
                sum = sum.add(sentiment.getSentimentScore());
                count++;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return count == 0 ? scaled(0) : sum.divide(BigDecimal.valueOf(count), 5, RoundingMode.HALF_UP);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param source source 参数，用于 toPollingView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorSourcePollingView toPollingView(MarketingMonitorSourceDO source) {
        return new MarketingMonitorSourcePollingView(
                source.getTenantId(),
                source.getId(),
                source.getSourceKey(),
                source.getSourceType(),
                enabled(source.getPollEnabled()),
                positive(source.getPollIntervalMinutes(), 60),
                source.getPollCursor(),
                source.getLastPolledAt(),
                source.getNextPollAt(),
                source.getLastPollStatus(),
                source.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param run run 参数，用于 toRunView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorPollRunView toRunView(MarketingMonitorPollRunDO run) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorPollRunView(
                run.getId(),
                run.getTenantId(),
                run.getSourceId(),
                run.getSourceKey(),
                run.getSourceType(),
                run.getStatus(),
                run.getRequestedFrom(),
                run.getRequestedUntil(),
                run.getCursorBefore(),
                run.getCursorAfter(),
                value(run.getItemCount()),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                value(run.getInsertedCount()),
                value(run.getDuplicateCount()),
                value(run.getAlertCount()),
                run.getErrorMessage(),
                map(run.getMetadataJson()),
                run.getCreatedBy(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorTrendSnapshotView toTrendView(MarketingMonitorTrendSnapshotDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorTrendSnapshotView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getSourceKey(),
                row.getBucketGrain(),
                row.getBucketStart(),
                row.getBucketEnd(),
                row.getBrandKey(),
                row.getCompetitorKey(),
                value(row.getMentionCount()),
                value(row.getPositiveCount()),
                value(row.getNeutralCount()),
                value(row.getNegativeCount()),
                value(row.getCompetitorCount()),
                value(row.getAlertCount()),
                row.getAvgSentimentScore() == null ? scaled(0) : row.getAvgSentimentScore(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行 cursor 流程，围绕 cursor 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param source source 参数，用于 cursor 流程中的校验、计算或对象转换。
     * @return 返回 cursor 生成的文本或业务键。
     */
    private String cursor(MarketingMonitorPollCommand command, MarketingMonitorSourceDO source) {
        return hasText(command == null ? null : command.cursorOverride())
                ? command.cursorOverride().trim()
                /**
                 * 解析、归一化或保护输入值，生成安全可用的中间结果。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                : trimToNull(source.getPollCursor());
    }

    /**
     * 执行 maxItems 流程，围绕 max items 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 max items 计算得到的数量、金额或指标值。
     */
    private int maxItems(MarketingMonitorPollCommand command) {
        if (command == null || command.maxItems() < 1) {
            return 100;
        }
        return Math.min(command.maxItems(), 100);
    }

    /**
     * 执行 itemTime 流程，围绕 item time 完成校验、计算或结果组装。
     *
     * @param item item 参数，用于 itemTime 流程中的校验、计算或对象转换。
     * @return 返回 itemTime 流程生成的业务结果。
     */
    private LocalDateTime itemTime(MarketingMonitorItemDO item) {
        return item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt();
    }

    /**
     * 执行 inWindow 流程，围绕 in window 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param start start 参数，用于 inWindow 流程中的校验、计算或对象转换。
     * @param end end 参数，用于 inWindow 流程中的校验、计算或对象转换。
     * @return 返回 in window 的布尔判断结果。
     */
    private boolean inWindow(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    /**
     * 规范化输入值。
     *
     * @param grain grain 参数，用于 normalizeGrain 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeGrain(String grain) {
        String normalized = hasText(grain) ? grain.trim().toUpperCase(Locale.ROOT) : "DAY";
        if (!List.of("HOUR", "DAY", "WEEK", "MONTH").contains(normalized)) {
            throw new IllegalArgumentException("unsupported monitoring trend bucket grain: " + grain);
        }
        return normalized;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 requiredTime 流程生成的业务结果。
     */
    private LocalDateTime requiredTime(LocalDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("marketing monitoring polling JSON serialization failed", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 执行 singleList 流程，围绕 single list 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 single list 汇总后的集合、分页或映射视图。
     */
    private List<String> singleList(String value) {
        return value == null ? List.of() : List.of(value);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private int positive(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 执行 scaled 流程，围绕 scaled 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 scaled 计算得到的数量、金额或指标值。
     */
    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param throwable throwable 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * PollCounts 数据记录。
     */
    private record PollCounts(int itemCount, int insertedCount, int duplicateCount, int alertCount) {
    }
}
