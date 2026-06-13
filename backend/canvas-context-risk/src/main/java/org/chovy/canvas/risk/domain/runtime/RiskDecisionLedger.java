package org.chovy.canvas.risk.domain.runtime;

import java.util.List;
import java.util.Optional;

/**
 * 风控决策账本接口，负责幂等查找、运行保存和规则命中证据保存。
 */
public interface RiskDecisionLedger {

    /**
     * 按租户和请求编号查找已有决策运行。
     */
    Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId);

    /**
     * 保存一次决策运行。
     */
    RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run);

    /**
     * 保存决策运行关联的规则命中证据。
     */
    void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits);
}
