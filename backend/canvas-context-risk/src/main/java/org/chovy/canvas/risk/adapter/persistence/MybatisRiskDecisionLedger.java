package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.chovy.canvas.risk.domain.runtime.RiskBand;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionAction;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionLedger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionResponse;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRuleHit;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRunRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis-backed risk decision ledger with idempotent request replay support.
 */
public class MybatisRiskDecisionLedger implements RiskDecisionLedger {

    private static final String STATUS_SUCCEEDED = "SUCCEEDED";

    private final RiskDecisionRunMapper runMapper;
    private final RiskRuleHitMapper hitMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, RunContext> savedRunContexts = new ConcurrentHashMap<>();

    public MybatisRiskDecisionLedger(RiskDecisionRunMapper runMapper,
                                     RiskRuleHitMapper hitMapper,
                                     ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.hitMapper = hitMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
        RiskDecisionRunDO row = runMapper.selectOne(new QueryWrapper<RiskDecisionRunDO>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId)
                .last("LIMIT 1"));
        return Optional.ofNullable(row).map(this::toRecord);
    }

    @Override
    public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
        RiskDecisionRunDO row = toRow(run);
        runMapper.insert(row);
        RiskDecisionRunRecord saved = run.withDecisionRunId(String.valueOf(row.getId()));
        row.setOutputJson(writeJson(saved.response()));
        runMapper.updateById(row);
        remember(saved);
        return saved;
    }

    @Override
    public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
        if (decisionRunId == null || decisionRunId.isBlank() || hits == null || hits.isEmpty()) {
            return;
        }
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

    private RiskDecisionRunDO toRow(RiskDecisionRunRecord run) {
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
        row.setMode(response == null || response.mode() == null
                ? RiskRuntimeMode.ENFORCE.name()
                : response.mode().name());
        row.setLatencyMs(response == null ? null : response.latencyMs());
        row.setStatus(STATUS_SUCCEEDED);
        row.setInputSnapshotJson(run.inputSnapshotJson());
        row.setOutputJson(writeJson(response));
        row.setCreatedAt(LocalDateTime.now());
        return row;
    }

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

    private RiskDecisionResponse readResponse(RiskDecisionRunDO row) {
        if (row.getOutputJson() != null && !row.getOutputJson().isBlank()) {
            try {
                RiskDecisionResponse response = objectMapper.readValue(row.getOutputJson(), RiskDecisionResponse.class);
                return withDecisionRunId(response, String.valueOf(row.getId()));
            } catch (JsonProcessingException error) {
                throw new IllegalStateException("Failed to parse risk decision output JSON", error);
            }
        }
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

    private void remember(RiskDecisionRunRecord run) {
        if (run == null || run.decisionRunId() == null || run.response() == null) {
            return;
        }
        RiskDecisionResponse response = run.response();
        savedRunContexts.put(run.decisionRunId(), new RunContext(
                run.tenantId(),
                response.strategyKey(),
                response.strategyVersion(),
                response.mode() == null ? null : response.mode().name()));
    }

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

    private RunContext loadContext(String decisionRunId) {
        RiskDecisionRunDO row = runMapper.selectById(parseDecisionRunId(decisionRunId));
        if (row == null) {
            return new RunContext(null, null, null, null);
        }
        return new RunContext(row.getTenantId(), row.getStrategyKey(), row.getStrategyVersion(), row.getMode());
    }

    private Long parseDecisionRunId(String decisionRunId) {
        try {
            return Long.valueOf(decisionRunId);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("decisionRunId must be a numeric persistence id: " + decisionRunId,
                    error);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Failed to write risk decision ledger JSON", error);
        }
    }

    private record RunContext(Long tenantId, String strategyKey, Integer strategyVersion, String mode) {
    }
}
