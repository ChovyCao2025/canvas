package org.chovy.canvas.domain.risk.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.dataobject.RiskRuleHitDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;
import org.chovy.canvas.web.risk.RiskDecisionTraceReader;
import org.chovy.canvas.web.risk.RiskDecisionTraceView;

import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 风控决策追踪读取器，从决策账本和规则命中表恢复工作台追踪列表。
 */
public class JdbcRiskDecisionTraceReader implements RiskDecisionTraceReader {

    private final RiskDecisionRunMapper runMapper;
    private final RiskRuleHitMapper hitMapper;

    /**
     * 创建 JDBC 追踪读取器。
     */
    public JdbcRiskDecisionTraceReader(RiskDecisionRunMapper runMapper, RiskRuleHitMapper hitMapper) {
        this.runMapper = runMapper;
        this.hitMapper = hitMapper;
    }

    @Override
    public List<RiskDecisionTraceView> listTraces(Long tenantId, String sceneKey, int limit) {
        List<RiskDecisionRunDO> runs = runMapper.selectList(runQuery(tenantId, sceneKey, limit));
        if (runs.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> hits = hitsByRun(tenantId, runs.stream().map(RiskDecisionRunDO::getId).toList());
        return runs.stream()
                .map(run -> toView(run, hits.getOrDefault(run.getId(), List.of())))
                .toList();
    }

    private LambdaQueryWrapper<RiskDecisionRunDO> runQuery(Long tenantId, String sceneKey, int limit) {
        LambdaQueryWrapper<RiskDecisionRunDO> query = new LambdaQueryWrapper<RiskDecisionRunDO>()
                .eq(RiskDecisionRunDO::getTenantId, tenantId)
                .orderByDesc(RiskDecisionRunDO::getCreatedAt)
                .last("LIMIT " + normalizedLimit(limit));
        if (sceneKey != null && !sceneKey.isBlank()) {
            query.eq(RiskDecisionRunDO::getSceneKey, sceneKey);
        }
        return query;
    }

    private Map<Long, List<String>> hitsByRun(Long tenantId, List<Long> runIds) {
        List<RiskRuleHitDO> rows = hitMapper.selectList(new LambdaQueryWrapper<RiskRuleHitDO>()
                .eq(RiskRuleHitDO::getTenantId, tenantId)
                .in(RiskRuleHitDO::getDecisionRunId, runIds)
                .orderByAsc(RiskRuleHitDO::getId));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (RiskRuleHitDO row : rows) {
            result.computeIfAbsent(row.getDecisionRunId(), ignored -> new java.util.ArrayList<>())
                    .add(row.getGroupKey() + ":" + row.getRuleKey());
        }
        return result;
    }

    private RiskDecisionTraceView toView(RiskDecisionRunDO run, List<String> hits) {
        return new RiskDecisionTraceView(
                String.valueOf(run.getId()),
                run.getRequestId(),
                run.getSceneKey(),
                run.getStrategyKey(),
                run.getStrategyVersion(),
                run.getMode(),
                run.getDecision(),
                run.getScore(),
                run.getRiskBand(),
                run.getLatencyMs(),
                run.getCreatedAt() == null ? null : run.getCreatedAt().toInstant(ZoneOffset.UTC).toString(),
                hits);
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
