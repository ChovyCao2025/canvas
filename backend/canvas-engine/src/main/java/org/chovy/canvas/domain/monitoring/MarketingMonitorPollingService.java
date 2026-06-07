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

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorSourcePollingView configurePolling(Long tenantId,
                                                              Long sourceId,
                                                              MarketingMonitorSourcePollingCommand command,
                                                              String actor) {
        if (command == null) {
            throw new IllegalArgumentException("monitoring source polling command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorSourceDO source = requireSource(scopedTenantId, sourceId);
        LocalDateTime changedAt = now();
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
        return toPollingView(source);
    }

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
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = now();
            failRun(run, ex, finishedAt);
            updateSourceAfterPoll(source, "FAILED", null, finishedAt);
            return toRunView(run);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorTrendSnapshotView buildTrendSnapshot(Long tenantId,
                                                                MarketingMonitorTrendSnapshotCommand command,
                                                                String actor) {
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
        List<MarketingMonitorItemDO> items = safeList(itemMapper.selectList(new LambdaQueryWrapper<MarketingMonitorItemDO>()
                .eq(MarketingMonitorItemDO::getTenantId, scopedTenantId)
                .eq(MarketingMonitorItemDO::getSourceId, source.getId())));
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

    public List<MarketingMonitorTrendSnapshotView> trendSnapshots(Long tenantId,
                                                                  MarketingMonitorTrendSnapshotQuery query) {
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorTrendSnapshotQuery effectiveQuery = query == null
                ? new MarketingMonitorTrendSnapshotQuery(null, null, null, 50)
                : query;
        LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO> wrapper =
                new LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO>()
                        .eq(MarketingMonitorTrendSnapshotDO::getTenantId, scopedTenantId)
                        .orderByDesc(MarketingMonitorTrendSnapshotDO::getBucketStart)
                        .last("LIMIT " + boundedLimit(effectiveQuery.limit()));
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
        return safeList(trendMapper.selectList(wrapper)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> effectiveQuery.sourceId() == null || effectiveQuery.sourceId().equals(row.getSourceId()))
                .filter(row -> brandKey == null || brandKey.equals(normalizeOptionalKey(row.getBrandKey())))
                .filter(row -> competitorKey == null || competitorKey.equals(normalizeOptionalKey(row.getCompetitorKey())))
                .map(this::toTrendView)
                .toList();
    }

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

    private MarketingMonitorPollClient clientFor(String sourceType) {
        return pollClients.stream()
                .filter(client -> client.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no monitor poll client supports source type: " + sourceType));
    }

    private MarketingMonitorPollRunDO runningRun(MarketingMonitorSourceDO source,
                                                 MarketingMonitorPollCommand command,
                                                 String cursorBefore,
                                                 String actor,
                                                 LocalDateTime startedAt) {
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
        run.setInsertedCount(0);
        run.setDuplicateCount(0);
        run.setAlertCount(0);
        run.setMetadataJson("{}");
        run.setCreatedBy(actor(actor));
        run.setStartedAt(startedAt);
        run.setCreatedAt(startedAt);
        run.setUpdatedAt(startedAt);
        return run;
    }

    private PollCounts ingestPolledItems(Long tenantId,
                                         MarketingMonitorSourceDO source,
                                         MarketingMonitorPollResponse response,
                                         String actor,
                                         int maxItems) {
        int itemCount = 0;
        int insertedCount = 0;
        int duplicateCount = 0;
        int alertCount = 0;
        Map<String, List<String>> competitors = competitors(source);
        for (MarketingMonitorPollItem item : safeList(response == null ? null : response.items()).stream()
                .limit(maxItems)
                .toList()) {
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

    private void completeRun(MarketingMonitorPollRunDO run,
                             PollCounts counts,
                             String cursorAfter,
                             Map<String, Object> metadata,
                             LocalDateTime finishedAt) {
        run.setStatus("COMPLETED");
        run.setCursorAfter(cursorAfter);
        run.setItemCount(counts.itemCount());
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

    private void failRun(MarketingMonitorPollRunDO run, RuntimeException ex, LocalDateTime finishedAt) {
        run.setStatus("FAILED");
        run.setErrorMessage(message(ex));
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        MarketingMonitorPollRunDO update = new MarketingMonitorPollRunDO();
        update.setId(run.getId());
        update.setStatus(run.getStatus());
        update.setErrorMessage(run.getErrorMessage());
        update.setFinishedAt(finishedAt);
        update.setUpdatedAt(finishedAt);
        runMapper.updateById(update);
    }

    private void updateSourceAfterPoll(MarketingMonitorSourceDO source,
                                       String status,
                                       String cursorAfter,
                                       LocalDateTime changedAt) {
        MarketingMonitorSourceDO update = new MarketingMonitorSourceDO();
        update.setId(source.getId());
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
            } else if (terms instanceof String text && hasText(text)) {
                result.put(key, singleList(text));
            }
        }
        return result;
    }

    private int sentimentCount(Map<Long, MarketingSentimentAnalysisDO> sentiments, String label) {
        int count = 0;
        for (MarketingSentimentAnalysisDO sentiment : sentiments.values()) {
            if (label.equals(normalizeOptionalUpper(sentiment.getSentimentLabel()))) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal averageSentiment(List<MarketingMonitorItemDO> items,
                                        Map<Long, MarketingSentimentAnalysisDO> sentiments) {
        if (items.isEmpty()) {
            return scaled(0);
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (MarketingMonitorItemDO item : items) {
            MarketingSentimentAnalysisDO sentiment = sentiments.get(item.getId());
            if (sentiment != null && sentiment.getSentimentScore() != null) {
                sum = sum.add(sentiment.getSentimentScore());
                count++;
            }
        }
        return count == 0 ? scaled(0) : sum.divide(BigDecimal.valueOf(count), 5, RoundingMode.HALF_UP);
    }

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

    private MarketingMonitorPollRunView toRunView(MarketingMonitorPollRunDO run) {
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

    private MarketingMonitorTrendSnapshotView toTrendView(MarketingMonitorTrendSnapshotDO row) {
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
                row.getUpdatedAt());
    }

    private String cursor(MarketingMonitorPollCommand command, MarketingMonitorSourceDO source) {
        return hasText(command == null ? null : command.cursorOverride())
                ? command.cursorOverride().trim()
                : trimToNull(source.getPollCursor());
    }

    private int maxItems(MarketingMonitorPollCommand command) {
        if (command == null || command.maxItems() < 1) {
            return 100;
        }
        return Math.min(command.maxItems(), 100);
    }

    private LocalDateTime itemTime(MarketingMonitorItemDO item) {
        return item.getPublishedAt() == null ? item.getIngestedAt() : item.getPublishedAt();
    }

    private boolean inWindow(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private String normalizeGrain(String grain) {
        String normalized = hasText(grain) ? grain.trim().toUpperCase(Locale.ROOT) : "DAY";
        if (!List.of("HOUR", "DAY", "WEEK", "MONTH").contains(normalized)) {
            throw new IllegalArgumentException("unsupported monitoring trend bucket grain: " + grain);
        }
        return normalized;
    }

    private LocalDateTime requiredTime(LocalDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("marketing monitoring polling JSON serialization failed", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<String> singleList(String value) {
        return value == null ? List.of() : List.of(value);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private int positive(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String actor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String message(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record PollCounts(int itemCount, int insertedCount, int duplicateCount, int alertCount) {
    }
}
