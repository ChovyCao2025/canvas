package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.risk.api.RiskGovernanceFacade;
import org.chovy.canvas.risk.domain.RiskGovernanceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定义 RiskGovernanceApplicationService 的风控模块职责和数据契约。
 */
@Service
public class RiskGovernanceApplicationService implements RiskGovernanceFacade {

    private final RiskGovernanceCatalog catalog = new RiskGovernanceCatalog();


    /**
     * 执行 decisionTraces 相关的风控处理逻辑。
     */
    @Override
    public List<Map<String, Object>> decisionTraces(Long tenantId, String sceneKey, Integer limit) {
        return catalog.decisionTraces(safeTenantId(tenantId), sceneKey, normalizedLimit(limit));
    }

    /**
     * 执行 createList 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createList(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createList(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 addListEntry 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addListEntry(Long tenantId, String listKey, Map<String, Object> payload,
                                            String actor) {
        return catalog.addListEntry(safeTenantId(tenantId), listKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 listEntries 相关的风控处理逻辑。
     */
    @Override
    public List<Map<String, Object>> listEntries(Long tenantId, String listKey) {
        return catalog.listEntries(safeTenantId(tenantId), listKey);
    }

    /**
     * 执行 removeListEntry 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeListEntry(Long tenantId, String listKey, Long entryId, String actor) {
        return catalog.removeListEntry(safeTenantId(tenantId), listKey, entryId, actorOrDefault(actor));
    }

    /**
     * 执行 importListEntries 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importListEntries(Long tenantId, String listKey, Map<String, Object> payload,
                                                 String actor) {
        return catalog.importListEntries(safeTenantId(tenantId), listKey, safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 执行 createStrategyDraft 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createStrategyDraft(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createStrategyDraft(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 getStrategy 相关的风控处理逻辑。
     */
    @Override
    public Map<String, Object> getStrategy(Long tenantId, String strategyKey) {
        return catalog.getStrategy(safeTenantId(tenantId), strategyKey);
    }

    /**
     * 执行 listStrategyVersions 相关的风控处理逻辑。
     */
    @Override
    public List<Map<String, Object>> listStrategyVersions(Long tenantId, String strategyKey) {
        return catalog.listStrategyVersions(safeTenantId(tenantId), strategyKey);
    }

    /**
     * 执行 validateStrategyVersion 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> validateStrategyVersion(Long tenantId, String strategyKey, Integer version,
                                                       String actor) {
        return catalog.validateStrategyVersion(safeTenantId(tenantId), strategyKey, safeVersion(version),
                actorOrDefault(actor));
    }

    /**
     * 执行 simulateStrategyVersion 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> simulateStrategyVersion(Long tenantId, String strategyKey, Integer version,
                                                       String actor) {
        return catalog.simulateStrategyVersion(safeTenantId(tenantId), strategyKey, safeVersion(version),
                actorOrDefault(actor));
    }

    /**
     * 执行 submitStrategyVersion 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitStrategyVersion(Long tenantId, String strategyKey, Integer version,
                                                     String actor) {
        return catalog.submitStrategyVersion(safeTenantId(tenantId), strategyKey, safeVersion(version),
                actorOrDefault(actor));
    }

    /**
     * 执行 approveStrategyVersion 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveStrategyVersion(Long tenantId, String strategyKey, Integer version,
                                                      String actor) {
        return catalog.approveStrategyVersion(safeTenantId(tenantId), strategyKey, safeVersion(version),
                actorOrDefault(actor));
    }

    /**
     * 执行 activateStrategyVersion 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activateStrategyVersion(Long tenantId, String strategyKey, Integer version,
                                                       String actor) {
        return catalog.activateStrategyVersion(safeTenantId(tenantId), strategyKey, safeVersion(version),
                actorOrDefault(actor));
    }

    /**
     * 执行 rollbackStrategy 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollbackStrategy(Long tenantId, String strategyKey, Map<String, Object> payload,
                                                String actor) {
        return catalog.rollbackStrategy(safeTenantId(tenantId), strategyKey, safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 执行 pauseStrategy 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pauseStrategy(Long tenantId, String strategyKey, String actor) {
        return catalog.pauseStrategy(safeTenantId(tenantId), strategyKey, actorOrDefault(actor));
    }

    /**
     * 执行 diffStrategyVersions 相关的风控处理逻辑。
     */
    @Override
    public Map<String, Object> diffStrategyVersions(Long tenantId, String strategyKey, Integer left, Integer right) {
        return catalog.diffStrategyVersions(safeTenantId(tenantId), strategyKey, safeVersion(left),
                safeVersion(right));
    }

    /**
     * 执行 startSimulation 相关的风控处理逻辑。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startSimulation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.startSimulation(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行 listSimulations 相关的风控处理逻辑。
     */
    @Override
    public List<Map<String, Object>> listSimulations(Long tenantId, String sceneKey, Integer limit) {
        return catalog.listSimulations(safeTenantId(tenantId), sceneKey, normalizedLimit(limit));
    }

    /**
     * 执行 safeTenantId 相关的风控处理逻辑。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行 safePayload 相关的风控处理逻辑。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行 actorOrDefault 相关的风控处理逻辑。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 执行 normalizedLimit 相关的风控处理逻辑。
     */
    private static int normalizedLimit(Integer limit) {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }

    /**
     * 执行 safeVersion 相关的风控处理逻辑。
     */
    private static int safeVersion(Integer version) {
        return version == null || version <= 0 ? 1 : version;
    }
}
