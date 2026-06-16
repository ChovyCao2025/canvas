package org.chovy.canvas.risk.domain.runtime;

import java.time.Duration;
import java.util.Optional;

/**
 * Online risk feature storage port.
 */
public interface RiskFeatureStore {

    /**
     * 执行 set 相关的风控处理逻辑。
     */
    void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl);

    /**
     * 执行 get 相关的风控处理逻辑。
     */
    Optional<Object> get(Long tenantId, String featureKey, String subjectHash);
}
