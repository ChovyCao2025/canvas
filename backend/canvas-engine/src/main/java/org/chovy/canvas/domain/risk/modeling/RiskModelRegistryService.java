package org.chovy.canvas.domain.risk.modeling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 风控模型注册表服务，维护模型版本并查找最新活跃版本。
 */
public class RiskModelRegistryService {

    private final List<RiskModelDefinition> models = new ArrayList<>();

    /**
     * 注册一个模型定义。
     */
    public void register(RiskModelDefinition definition) {
        models.add(definition);
    }

    /**
     * 查找指定模型键的最新活跃版本。
     */
    public Optional<RiskModelDefinition> latestActive(String modelKey) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return models.stream()
                .filter(model -> model.modelKey().equals(modelKey))
                .filter(RiskModelDefinition::active)
                .max(Comparator.comparingInt(RiskModelDefinition::version));
    }
}
