package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
/**
 * MarketingMonitorPollingScheduleService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class MarketingMonitorPollingScheduleService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_MAX_ITEMS = 100;

    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitorPollingService pollingService;

    /**
     * 初始化 MarketingMonitorPollingScheduleService 实例。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param pollingService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingMonitorPollingScheduleService(MarketingMonitorSourceMapper sourceMapper,
                                                  MarketingMonitorPollingService pollingService) {
        this.sourceMapper = sourceMapper;
        this.pollingService = pollingService;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 pollDueSources 流程生成的业务结果。
     */
    public ScheduledPollResult pollDueSources(Long tenantId,
                                              LocalDateTime evaluatedAt,
                                              int limit,
                                              String operator) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        LocalDateTime now = evaluatedAt == null ? LocalDateTime.now() : evaluatedAt;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MarketingMonitorSourceDO> candidates = safeList(sourceMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getEnabled, 1)
                        .eq(MarketingMonitorSourceDO::getPollEnabled, 1)
                        .orderByAsc(MarketingMonitorSourceDO::getNextPollAt)));
        int boundedLimit = boundedLimit(limit);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ScheduledPollResult(
                scopedTenantId,
                candidates.size(),
                dueSources.size(),
                succeeded,
                failed,
                Math.max(0, candidates.size() - dueSources.size()),
                now);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sources sources 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private List<MarketingMonitorSourceDO> safeList(List<MarketingMonitorSourceDO> sources) {
        return sources == null ? List.of() : sources;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String operator) {
        return operator == null || operator.isBlank() ? "monitoring-scheduler" : operator.trim();
    }

    /**
     * ScheduledPollResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
