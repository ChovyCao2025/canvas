package org.chovy.canvas.risk.domain.runtime;

import java.time.Duration;
import java.util.Optional;

/**
 * Online risk feature storage port.
 */
public interface RiskFeatureStore {

    void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl);

    Optional<Object> get(Long tenantId, String featureKey, String subjectHash);
}
