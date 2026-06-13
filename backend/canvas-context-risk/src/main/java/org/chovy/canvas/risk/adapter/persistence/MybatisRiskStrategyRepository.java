package org.chovy.canvas.risk.adapter.persistence;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.chovy.canvas.risk.domain.governance.RiskStrategyRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRiskStrategyRepository implements RiskStrategyRepository {

    private final RiskStrategyMapper mapper;

    public MybatisRiskStrategyRepository(RiskStrategyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
        return mapper.selectList(new LambdaQueryWrapper<RiskStrategyDO>()
                        .eq(RiskStrategyDO::getTenantId, tenantId)
                        .eq(sceneKey != null && !sceneKey.isBlank(), RiskStrategyDO::getSceneKey, sceneKey)
                        .orderByAsc(RiskStrategyDO::getStrategyKey))
                .stream()
                .map(this::toView)
                .toList();
    }

    private RiskStrategyView toView(RiskStrategyDO row) {
        return new RiskStrategyView(
                row.getTenantId(),
                row.getSceneKey(),
                row.getStrategyKey(),
                row.getName(),
                row.getStatus(),
                row.getActiveVersion(),
                row.getDraftVersion(),
                row.getRiskLevel(),
                row.getOwner());
    }
}
