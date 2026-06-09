package org.chovy.canvas.domain.risk.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.dataobject.RiskRuleHitDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC 风控决策账本，持久化决策运行和规则命中证据，并按请求编号支持幂等回放。
 */
public class JdbcRiskDecisionLedger implements RiskDecisionLedger {

    private static final String STATUS_SUCCEEDED = "SUCCEEDED";

    private final RiskDecisionRunMapper runMapper;
    private final RiskRuleHitMapper hitMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, RunContext> savedRunContexts = new ConcurrentHashMap<>();

    /**
     * 创建 JDBC 决策账本，未传入 ObjectMapper 时使用默认 JSON 映射器。
     */
    public JdbcRiskDecisionLedger(RiskDecisionRunMapper runMapper,
                                  RiskRuleHitMapper hitMapper,
                                  ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.hitMapper = hitMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 按租户和请求编号查找已持久化的决策运行，用于幂等回放。
     */
    @Override
    public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
        RiskDecisionRunDO row = runMapper.selectOne(new QueryWrapper<RiskDecisionRunDO>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId)
                .last("LIMIT 1"));
        return Optional.ofNullable(row).map(this::toRecord);
    }

    /**
     * 保存一次决策运行，并在数据库生成编号后回写对外响应 JSON。
     */
    @Override
    public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
        RiskDecisionRunDO row = toRow(run);
        runMapper.insert(row);
        RiskDecisionRunRecord saved = run.withDecisionRunId(String.valueOf(row.getId()));
        // 数据库生成编号属于对外响应的一部分，插入后需要重写 output JSON。
        row.setOutputJson(writeJson(saved.response()));
        runMapper.updateById(row);
        remember(saved);
        return saved;
    }

    /**
     * 保存规则命中证据，并补齐所属决策运行的策略上下文。
     */
    @Override
    public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
        if (decisionRunId == null || decisionRunId.isBlank() || hits == null || hits.isEmpty()) {
            return;
        }
        // 规则命中行需要从决策运行中复制策略上下文，因为调用方只传公开运行编号。
        RunContext context = savedRunContexts.computeIfAbsent(decisionRunId, this::loadContext);
        Long runId = parseDecisionRunId(decisionRunId);
        for (RiskDecisionRuleHit hit : hits) {
            RiskRuleHitDO row = new RiskRuleHitDO();
            row.setTenantId(context.tenantId());
            row.setDecisionRunId(runId);
            row.setStrategyKey(context.strategyKey());
            row.setStrategyVersion(context.strategyVersion());
            row.setGroupKey(hit.groupKey());
            row.setRuleKey(hit.ruleKey());
            row.setMode(context.mode());
            row.setAction(hit.action() == null ? null : hit.action().name());
            row.setScoreDelta(hit.scoreDelta());
            row.setReasonCode(hit.reasonCode());
            row.setEvidenceJson(writeJson(Map.of(
                    "groupKey", hit.groupKey(),
                    "ruleKey", hit.ruleKey(),
                    "shadow", hit.shadow())));
            row.setCreatedAt(LocalDateTime.now());
            hitMapper.insert(row);
        }
    }

    /**
     * 将领域决策运行转换为持久化行。
     */
    private RiskDecisionRunDO toRow(RiskDecisionRunRecord run) {
        // 准备本次处理所需的上下文和中间变量。
        RiskDecisionResponse response = run.response();
        RiskDecisionRunDO row = new RiskDecisionRunDO();
        row.setTenantId(run.tenantId());
        row.setRequestId(run.requestId());
        row.setRequestHash(run.requestHash());
        row.setSubjectHash(run.subjectHash());
        row.setSceneKey(response == null ? null : response.sceneKey());
        row.setStrategyKey(response == null ? null : response.strategyKey());
        row.setStrategyVersion(response == null ? null : response.strategyVersion());
        row.setDecision(response == null || response.action() == null ? null : response.action().name());
        row.setScore(response == null ? null : response.score());
        row.setRiskBand(response == null || response.riskBand() == null ? null : response.riskBand().name());
        row.setMode(response == null || response.mode() == null ? null : response.mode().name());
        row.setLatencyMs(response == null ? null : response.latencyMs());
        row.setStatus(STATUS_SUCCEEDED);
        row.setInputSnapshotJson(run.inputSnapshotJson());
        row.setOutputJson(writeJson(response));
        row.setCreatedAt(LocalDateTime.now());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 将持久化行恢复为领域决策运行，并缓存后续保存命中所需的上下文。
     */
    private RiskDecisionRunRecord toRecord(RiskDecisionRunDO row) {
        RiskDecisionResponse response = readResponse(row);
        RiskDecisionRunRecord record = new RiskDecisionRunRecord(
                String.valueOf(row.getId()),
                row.getTenantId(),
                row.getRequestId(),
                row.getRequestHash(),
                row.getSubjectHash(),
                row.getInputSnapshotJson(),
                response);
        remember(record);
        return record;
    }

    /**
     * 从 output JSON 或列级投影恢复对外决策响应。
     */
    private RiskDecisionResponse readResponse(RiskDecisionRunDO row) {
        if (row.getOutputJson() != null && !row.getOutputJson().isBlank()) {
            try {
                RiskDecisionResponse response = objectMapper.readValue(row.getOutputJson(), RiskDecisionResponse.class);
                return withDecisionRunId(response, String.valueOf(row.getId()));
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (JsonProcessingException error) {
                throw new IllegalStateException("Failed to parse risk decision output JSON", error);
            }
        }
        // 旧数据可能只有列级投影，这里重建最小响应以兼容回放。
        return new RiskDecisionResponse(
                row.getRequestId(),
                String.valueOf(row.getId()),
                row.getSceneKey(),
                row.getStrategyKey(),
                row.getStrategyVersion() == null ? 0 : row.getStrategyVersion(),
                row.getMode() == null ? null : org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode.valueOf(row.getMode()),
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
     * 缓存非敏感运行上下文，供随后写入规则命中明细时使用。
     */
    private void remember(RiskDecisionRunRecord run) {
        if (run == null || run.decisionRunId() == null || run.response() == null) {
            return;
        }
        // 只缓存追加规则命中证据所需的非敏感路由元数据。
        RiskDecisionResponse response = run.response();
        savedRunContexts.put(run.decisionRunId(), new RunContext(
                run.tenantId(),
                response.strategyKey(),
                response.strategyVersion(),
                response.mode() == null ? null : response.mode().name()));
    }

    /**
     * 用数据库运行编号替换响应中的决策运行编号。
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

    /**
     * 从数据库加载决策运行上下文，缓存缺失时兜底使用。
     */
    private RunContext loadContext(String decisionRunId) {
        RiskDecisionRunDO row = runMapper.selectById(parseDecisionRunId(decisionRunId));
        if (row == null) {
            return new RunContext(null, null, null, null);
        }
        return new RunContext(row.getTenantId(), row.getStrategyKey(), row.getStrategyVersion(), row.getMode());
    }

    /**
     * 将公开决策运行编号解析为数据库主键。
     */
    private Long parseDecisionRunId(String decisionRunId) {
        try {
            return Long.valueOf(decisionRunId);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("decisionRunId must be a numeric persistence id: " + decisionRunId,
                    error);
        }
    }

    /**
     * 将对象序列化为 JSON，失败时转换为运行时异常。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Failed to write risk decision ledger JSON", error);
        }
    }

    /**
     * 决策运行上下文，保存规则命中行需要复制的策略元数据。
     */
    private record RunContext(
            Long tenantId,
            String strategyKey,
            Integer strategyVersion,
            String mode
    ) {
    }
}
