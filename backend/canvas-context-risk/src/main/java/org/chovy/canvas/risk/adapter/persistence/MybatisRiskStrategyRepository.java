package org.chovy.canvas.risk.adapter.persistence;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.chovy.canvas.risk.domain.governance.RiskStrategyRepository;
import org.springframework.stereotype.Repository;

/**
 * 定义 MybatisRiskStrategyRepository 的风控模块职责和数据契约。
 */
@Repository
public class MybatisRiskStrategyRepository implements RiskStrategyRepository {

    /**
     * 保存 mapper 对应的风控状态或配置。
     */
    private final RiskStrategyMapper mapper;

    public MybatisRiskStrategyRepository(RiskStrategyMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 执行 listStrategies 相关的风控处理逻辑。
     */
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

    /**
     * 执行 toView 相关的风控处理逻辑。
     */
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
