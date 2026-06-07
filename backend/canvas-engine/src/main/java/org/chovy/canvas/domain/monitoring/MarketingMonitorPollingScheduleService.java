package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MarketingMonitorPollingScheduleService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_MAX_ITEMS = 100;

    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitorPollingService pollingService;

    public MarketingMonitorPollingScheduleService(MarketingMonitorSourceMapper sourceMapper,
                                                  MarketingMonitorPollingService pollingService) {
        this.sourceMapper = sourceMapper;
        this.pollingService = pollingService;
    }

    public ScheduledPollResult pollDueSources(Long tenantId,
                                              LocalDateTime evaluatedAt,
                                              int limit,
                                              String operator) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        LocalDateTime now = evaluatedAt == null ? LocalDateTime.now() : evaluatedAt;
        List<MarketingMonitorSourceDO> candidates = safeList(sourceMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getEnabled, 1)
                        .eq(MarketingMonitorSourceDO::getPollEnabled, 1)
                        .orderByAsc(MarketingMonitorSourceDO::getNextPollAt)));
        int boundedLimit = boundedLimit(limit);
        List<MarketingMonitorSourceDO> dueSources = candidates.stream()
                .filter(source -> scopedTenantId.equals(source.getTenantId()))
                .filter(source -> enabled(source.getEnabled()))
                .filter(source -> enabled(source.getPollEnabled()))
                .filter(source -> source.getNextPollAt() == null || !source.getNextPollAt().isAfter(now))
                .sorted(Comparator.comparing(
                        MarketingMonitorSourceDO::getNextPollAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(boundedLimit)
                .toList();

        int succeeded = 0;
        int failed = 0;
        MarketingMonitorPollCommand command = new MarketingMonitorPollCommand(
                null,
                now,
                null,
                DEFAULT_MAX_ITEMS,
                false);
        for (MarketingMonitorSourceDO source : dueSources) {
            try {
                pollingService.pollSource(scopedTenantId, source.getId(), command, actor(operator));
                succeeded++;
            } catch (RuntimeException ex) {
                failed++;
            }
        }
        return new ScheduledPollResult(
                scopedTenantId,
                candidates.size(),
                dueSources.size(),
                succeeded,
                failed,
                Math.max(0, candidates.size() - dueSources.size()),
                now);
    }

    private List<MarketingMonitorSourceDO> safeList(List<MarketingMonitorSourceDO> sources) {
        return sources == null ? List.of() : sources;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private String actor(String operator) {
        return operator == null || operator.isBlank() ? "monitoring-scheduler" : operator.trim();
    }

    public record ScheduledPollResult(
            Long tenantId,
            int candidateCount,
            int dueCount,
            int succeededCount,
            int failedCount,
            int skippedCount,
            LocalDateTime evaluatedAt) {
    }
}
