package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskStrategyFacade;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.chovy.canvas.risk.domain.governance.RiskStrategyRepository;
import org.springframework.stereotype.Service;

/**
 * 定义 RiskStrategyApplicationService 的风控模块职责和数据契约。
 */
@Service
public class RiskStrategyApplicationService implements RiskStrategyFacade {

    /**
     * 保存 repository 对应的风控状态或配置。
     */
    private final RiskStrategyRepository repository;

    public RiskStrategyApplicationService(RiskStrategyRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行 listStrategies 相关的风控处理逻辑。
     */
    @Override
    public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
        Objects.requireNonNull(tenantId, "tenantId");
        return repository.listStrategies(tenantId, sceneKey);
    }
}
