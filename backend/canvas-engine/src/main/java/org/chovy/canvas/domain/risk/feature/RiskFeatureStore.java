package org.chovy.canvas.domain.risk.feature;

import java.time.Duration;
import java.util.Optional;

/**
 * 风控在线特征存储接口。
 */
public interface RiskFeatureStore {

    /**
     * 写入主体特征值。
     */
    void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl);

    /**
     * 读取主体特征值。
     */
    Optional<Object> get(Long tenantId, String featureKey, String subjectHash);
}
