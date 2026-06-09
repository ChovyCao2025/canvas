package org.chovy.canvas.domain.risk.lab;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;

import java.util.List;

/**
 * JDBC 风控仿真样本仓储，从历史决策运行中加载有界样本用于离线策略对比。
 */
public class JdbcRiskSimulationSampleRepository implements RiskSimulationSampleRepository {

    private static final int MAX_SAMPLE_LIMIT = 10_000;

    private final RiskDecisionRunMapper runMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JDBC 仿真样本仓储。
     */
    public JdbcRiskSimulationSampleRepository(RiskDecisionRunMapper runMapper, ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 按租户和场景加载最近决策运行样本，并限制最大样本量。
     */
    @Override
    public List<RiskDecisionRunRecord> findSamples(Long tenantId, String sceneKey, int limit) {
        // 在仓储边界限制样本量，避免实验请求耗尽决策运行存储。
        int safeLimit = Math.max(1, Math.min(limit, MAX_SAMPLE_LIMIT));
        List<RiskDecisionRunDO> rows = runMapper.selectList(new QueryWrapper<RiskDecisionRunDO>()
                .eq("tenant_id", tenantId)
                .eq("scene_key", sceneKey)
                .orderByDesc("id")
                .last("LIMIT " + safeLimit));
        return findSamplesFromRows(rows);
    }

    /**
     * 评估候选策略在样本上的动作，JDBC 默认实现复用历史动作。
     */
    @Override
    public RiskDecisionAction evaluateCandidate(RiskDecisionRunRecord sample, String strategyKey, int candidateVersion) {
        // 候选评估保持可插拔；JDBC 兜底实现复用已记录结果。
        return sample.response().action();
    }

    /**
     * 将数据库行列表转换为仿真样本记录。
     */
    List<RiskDecisionRunRecord> findSamplesFromRows(List<RiskDecisionRunDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(this::toRecord).toList();
    }

    /**
     * 将单条决策运行行转换为领域记录。
     */
    private RiskDecisionRunRecord toRecord(RiskDecisionRunDO row) {
        return new RiskDecisionRunRecord(
                String.valueOf(row.getId()),
                row.getTenantId(),
                row.getRequestId(),
                row.getRequestHash(),
                row.getSubjectHash(),
                row.getInputSnapshotJson(),
                response(row));
    }

    /**
     * 从输出 JSON 或列级投影恢复样本响应。
     */
    private RiskDecisionResponse response(RiskDecisionRunDO row) {
        if (row.getOutputJson() != null && !row.getOutputJson().isBlank()) {
            try {
                RiskDecisionResponse response = objectMapper.readValue(row.getOutputJson(), RiskDecisionResponse.class);
                return withDecisionRunId(response, String.valueOf(row.getId()));
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (JsonProcessingException error) {
                throw new IllegalStateException("Failed to parse risk decision sample output JSON", error);
            }
        }
        // 旧样本行可能早于完整 output JSON，这里重建仿真所需字段。
        return new RiskDecisionResponse(
                row.getRequestId(),
                String.valueOf(row.getId()),
                row.getSceneKey(),
                row.getStrategyKey(),
                row.getStrategyVersion() == null ? 0 : row.getStrategyVersion(),
                row.getMode() == null ? null : RiskRuntimeMode.valueOf(row.getMode()),
                row.getDecision() == null ? null : RiskDecisionAction.valueOf(row.getDecision()),
                row.getScore() == null ? 0 : row.getScore(),
                row.getRiskBand() == null ? null : RiskBand.valueOf(row.getRiskBand()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                row.getLatencyMs() == null ? 0 : row.getLatencyMs(),
                false);
    }

    /**
     * 将数据库运行编号写回样本响应。
     */
    private RiskDecisionResponse withDecisionRunId(RiskDecisionResponse response, String decisionRunId) {
        return new RiskDecisionResponse(
                response.requestId(),
                decisionRunId,
                response.sceneKey(),
                response.strategyKey(),
                response.strategyVersion(),
                response.mode(),
                response.action(),
                response.score(),
                response.riskBand(),
                response.reasons(),
                response.matchedRules(),
                response.labels(),
                response.missingFeatures(),
                response.latencyMs(),
                response.traceAvailable());
    }
}
