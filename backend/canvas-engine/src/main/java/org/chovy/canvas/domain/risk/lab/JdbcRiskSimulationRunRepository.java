package org.chovy.canvas.domain.risk.lab;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskSimulationRunDO;
import org.chovy.canvas.dal.mapper.RiskSimulationRunMapper;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * JDBC 风控仿真运行仓储。
 */
public class JdbcRiskSimulationRunRepository implements RiskSimulationRunRepository {

    private static final TypeReference<Map<RiskDecisionAction, Integer>> ACTION_DISTRIBUTION =
            new TypeReference<>() {
            };
    private static final TypeReference<Map<String, Integer>> ACTION_CHANGES = new TypeReference<>() {
    };

    private final RiskSimulationRunMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 JDBC 仿真运行仓储。
     */
    public JdbcRiskSimulationRunRepository(RiskSimulationRunMapper mapper,
                                           ObjectMapper objectMapper,
                                           Clock clock) {
        this.mapper = mapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void save(RiskSimulationRequest request, RiskSimulationResult result) {
        RiskSimulationRunDO existing = mapper.selectOne(new LambdaQueryWrapper<RiskSimulationRunDO>()
                .eq(RiskSimulationRunDO::getTenantId, request.tenantId())
                .eq(RiskSimulationRunDO::getSimulationId, result.simulationId()));
        RiskSimulationRunDO row = toRow(request, result);
        if (existing == null) {
            mapper.insert(row);
        } else {
            row.setId(existing.getId());
            mapper.updateById(row);
        }
    }

    @Override
    public List<RiskSimulationHistoryView> list(Long tenantId, String sceneKey, int limit) {
        LambdaQueryWrapper<RiskSimulationRunDO> query = new LambdaQueryWrapper<RiskSimulationRunDO>()
                .eq(RiskSimulationRunDO::getTenantId, tenantId)
                .orderByDesc(RiskSimulationRunDO::getCreatedAt)
                .last("LIMIT " + normalizedLimit(limit));
        if (sceneKey != null && !sceneKey.isBlank()) {
            query.eq(RiskSimulationRunDO::getSceneKey, sceneKey);
        }
        return mapper.selectList(query).stream()
                .map(this::toView)
                .toList();
    }

    private RiskSimulationRunDO toRow(RiskSimulationRequest request, RiskSimulationResult result) {
        RiskSimulationRunDO row = new RiskSimulationRunDO();
        row.setTenantId(request.tenantId());
        row.setSimulationId(result.simulationId());
        row.setSceneKey(request.sceneKey());
        row.setStrategyKey(request.strategyKey());
        row.setBaselineVersion(request.baselineVersion());
        row.setCandidateVersion(request.candidateVersion());
        row.setStatus(result.status().name());
        row.setSampleSize(result.sampleSize());
        row.setChangedActionCount(result.changedActionCount());
        row.setActionDistributionJson(writeJson(result.actionDistribution()));
        row.setActionChangesJson(writeJson(result.actionChanges()));
        row.setCreatedAt(LocalDateTime.now(clock));
        return row;
    }

    private RiskSimulationHistoryView toView(RiskSimulationRunDO row) {
        return new RiskSimulationHistoryView(
                row.getSimulationId(),
                row.getSceneKey(),
                row.getStrategyKey(),
                row.getBaselineVersion(),
                row.getCandidateVersion(),
                RiskSimulationStatus.valueOf(row.getStatus()),
                row.getSampleSize(),
                read(row.getActionDistributionJson(), ACTION_DISTRIBUTION),
                row.getChangedActionCount(),
                read(row.getActionChangesJson(), ACTION_CHANGES),
                row.getCreatedAt() == null ? null : row.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Failed to serialize risk simulation run", error);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Failed to parse risk simulation run", error);
        }
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
