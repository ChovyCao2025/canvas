package org.chovy.canvas.domain.risk.feature;

import java.util.Map;
import java.util.Optional;

/**
 * 风控特征目录服务，维护特征键与主体字段之间的映射。
 */
public class RiskFeatureCatalogService {

    private final Map<String, String> subjectFields = Map.of(
            "user.fail_count_1d", "userId",
            "user.has_chargeback", "userId",
            "user.segment", "userId"
    );

    /**
     * 查找特征解析所需的主体字段名。
     */
    public Optional<String> subjectFieldFor(String featureKey) {
        return Optional.ofNullable(subjectFields.get(featureKey));
    }
}
