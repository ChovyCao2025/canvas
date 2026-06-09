package org.chovy.canvas.domain.risk.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskStrategyDO;
import org.chovy.canvas.dal.dataobject.RiskStrategyVersionDO;
import org.chovy.canvas.dal.mapper.RiskStrategyMapper;
import org.chovy.canvas.dal.mapper.RiskStrategyVersionMapper;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService.StateStore;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService.StrategyState;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService.StrategyVersion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed 风控策略状态仓储，使策略生命周期和活跃版本可跨进程重启恢复。
 */
public class JdbcRiskStrategyStateStore implements StateStore {

    private final RiskStrategyMapper strategyMapper;
    private final RiskStrategyVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 JDBC 策略状态仓储。
     */
    public JdbcRiskStrategyStateStore(RiskStrategyMapper strategyMapper,
                                      RiskStrategyVersionMapper versionMapper,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.strategyMapper = strategyMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public Optional<StrategyState> find(Long tenantId, String strategyKey) {
        RiskStrategyDO strategy = strategyMapper.selectOne(strategyQuery(tenantId, strategyKey));
        return Optional.ofNullable(strategy).map(this::toState);
    }

    @Override
    public Collection<StrategyState> findByTenant(Long tenantId) {
        return strategyMapper.selectList(new LambdaQueryWrapper<RiskStrategyDO>()
                        .eq(RiskStrategyDO::getTenantId, tenantId))
                .stream()
                .map(this::toState)
                .toList();
    }

    @Override
    public void save(StrategyState state) {
        LocalDateTime now = LocalDateTime.now(clock);
        RiskStrategyDO existing = strategyMapper.selectOne(strategyQuery(state.tenantId(), state.strategyKey()));
        RiskStrategyDO row = toStrategyRow(state, now);
        if (existing == null) {
            row.setCreatedAt(now);
            strategyMapper.insert(row);
        } else {
            row.setId(existing.getId());
            row.setCreatedAt(existing.getCreatedAt());
            strategyMapper.updateById(row);
        }
        for (StrategyVersion version : state.versions().values()) {
            saveVersion(state, version, now);
        }
    }

    private void saveVersion(StrategyState state, StrategyVersion version, LocalDateTime now) {
        RiskStrategyVersionDO existing = versionMapper.selectOne(versionQuery(state.tenantId(),
                state.strategyKey(), version.version()));
        RiskStrategyVersionDO row = toVersionRow(state, version, now);
        if (existing == null) {
            row.setCreatedAt(now);
            versionMapper.insert(row);
        } else {
            row.setId(existing.getId());
            row.setCreatedAt(existing.getCreatedAt());
            versionMapper.updateById(row);
        }
    }

    private StrategyState toState(RiskStrategyDO row) {
        StrategyState state = new StrategyState(row.getTenantId(), row.getSceneKey(), row.getStrategyKey(),
                row.getName(), row.getRiskLevel(), row.getOwner());
        state.status(status(row.getStatus()));
        state.activeVersion(row.getActiveVersion());
        state.draftVersion(row.getDraftVersion());
        state.riskLevel(row.getRiskLevel());
        List<RiskStrategyVersionDO> versions = versionMapper.selectList(new LambdaQueryWrapper<RiskStrategyVersionDO>()
                .eq(RiskStrategyVersionDO::getTenantId, row.getTenantId())
                .eq(RiskStrategyVersionDO::getStrategyKey, row.getStrategyKey()))
                .stream()
                .sorted(Comparator.comparing(RiskStrategyVersionDO::getVersion))
                .toList();
        for (RiskStrategyVersionDO version : versions) {
            state.versions().put(version.getVersion(), toVersion(version));
        }
        return state;
    }

    private StrategyVersion toVersion(RiskStrategyVersionDO row) {
        return new StrategyVersion(
                row.getVersion(),
                status(row.getStatus()),
                row.getDefinitionJson(),
                row.getValidationJson(),
                row.getCreatedBy(),
                row.getSubmittedBy(),
                row.getApprovedBy());
    }

    private RiskStrategyDO toStrategyRow(StrategyState state, LocalDateTime now) {
        RiskStrategyDO row = new RiskStrategyDO();
        row.setTenantId(state.tenantId());
        row.setSceneKey(state.sceneKey());
        row.setStrategyKey(state.strategyKey());
        row.setName(state.name());
        row.setStatus(state.status().name());
        row.setActiveVersion(state.activeVersion());
        row.setDraftVersion(state.draftVersion());
        row.setRiskLevel(state.riskLevel());
        row.setOwner(state.owner());
        row.setUpdatedAt(now);
        return row;
    }

    private RiskStrategyVersionDO toVersionRow(StrategyState state, StrategyVersion version, LocalDateTime now) {
        RiskStrategyVersionDO row = new RiskStrategyVersionDO();
        row.setTenantId(state.tenantId());
        row.setStrategyKey(state.strategyKey());
        row.setVersion(version.version());
        row.setStatus(version.status().name());
        row.setMode(text(version.definitionJson(), "mode", "ENFORCE"));
        row.setTrafficPercent(new BigDecimal(text(version.definitionJson(), "trafficPercent", "100")));
        row.setCompiledHash(hash(version.definitionJson()));
        row.setDefinitionJson(version.definitionJson());
        row.setValidationJson(version.validationJson());
        row.setCreatedBy(version.createdBy());
        row.setSubmittedBy(version.submittedBy());
        row.setSubmittedAt(version.submittedBy() == null ? null : now);
        row.setApprovedBy(version.approvedBy());
        row.setApprovedAt(version.approvedBy() == null ? null : now);
        row.setUpdatedAt(now);
        return row;
    }

    private LambdaQueryWrapper<RiskStrategyDO> strategyQuery(Long tenantId, String strategyKey) {
        return new LambdaQueryWrapper<RiskStrategyDO>()
                .eq(RiskStrategyDO::getTenantId, tenantId)
                .eq(RiskStrategyDO::getStrategyKey, strategyKey);
    }

    private LambdaQueryWrapper<RiskStrategyVersionDO> versionQuery(Long tenantId, String strategyKey, int version) {
        return new LambdaQueryWrapper<RiskStrategyVersionDO>()
                .eq(RiskStrategyVersionDO::getTenantId, tenantId)
                .eq(RiskStrategyVersionDO::getStrategyKey, strategyKey)
                .eq(RiskStrategyVersionDO::getVersion, version);
    }

    private RiskStrategyLifecycleStatus status(String status) {
        return status == null || status.isBlank()
                ? RiskStrategyLifecycleStatus.DRAFT
                : RiskStrategyLifecycleStatus.valueOf(status);
    }

    private String text(String definitionJson, String field, String fallback) {
        try {
            JsonNode value = objectMapper.readTree(definitionJson).get(field);
            return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(
                    String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
