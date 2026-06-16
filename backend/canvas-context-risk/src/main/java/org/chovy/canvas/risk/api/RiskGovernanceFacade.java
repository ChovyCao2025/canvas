package org.chovy.canvas.risk.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 RiskGovernanceFacade 的风控模块职责和数据契约。
 */
public interface RiskGovernanceFacade {

    /**
     * 执行 decisionTraces 相关的风控处理逻辑。
     */
    List<Map<String, Object>> decisionTraces(Long tenantId, String sceneKey, Integer limit);

    /**
     * 执行 createList 相关的风控处理逻辑。
     */
    Map<String, Object> createList(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行 addListEntry 相关的风控处理逻辑。
     */
    Map<String, Object> addListEntry(Long tenantId, String listKey, Map<String, Object> payload, String actor);

    /**
     * 执行 listEntries 相关的风控处理逻辑。
     */
    List<Map<String, Object>> listEntries(Long tenantId, String listKey);

    /**
     * 执行 removeListEntry 相关的风控处理逻辑。
     */
    Map<String, Object> removeListEntry(Long tenantId, String listKey, Long entryId, String actor);

    /**
     * 执行 importListEntries 相关的风控处理逻辑。
     */
    Map<String, Object> importListEntries(Long tenantId, String listKey, Map<String, Object> payload, String actor);

    /**
     * 执行 createStrategyDraft 相关的风控处理逻辑。
     */
    Map<String, Object> createStrategyDraft(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行 getStrategy 相关的风控处理逻辑。
     */
    Map<String, Object> getStrategy(Long tenantId, String strategyKey);

    /**
     * 执行 listStrategyVersions 相关的风控处理逻辑。
     */
    List<Map<String, Object>> listStrategyVersions(Long tenantId, String strategyKey);

    /**
     * 执行 validateStrategyVersion 相关的风控处理逻辑。
     */
    Map<String, Object> validateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    /**
     * 执行 simulateStrategyVersion 相关的风控处理逻辑。
     */
    Map<String, Object> simulateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    /**
     * 执行 submitStrategyVersion 相关的风控处理逻辑。
     */
    Map<String, Object> submitStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    /**
     * 执行 approveStrategyVersion 相关的风控处理逻辑。
     */
    Map<String, Object> approveStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    /**
     * 执行 activateStrategyVersion 相关的风控处理逻辑。
     */
    Map<String, Object> activateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    /**
     * 执行 rollbackStrategy 相关的风控处理逻辑。
     */
    Map<String, Object> rollbackStrategy(Long tenantId, String strategyKey, Map<String, Object> payload,
                                         String actor);

    /**
     * 执行 pauseStrategy 相关的风控处理逻辑。
     */
    Map<String, Object> pauseStrategy(Long tenantId, String strategyKey, String actor);

    /**
     * 执行 diffStrategyVersions 相关的风控处理逻辑。
     */
    Map<String, Object> diffStrategyVersions(Long tenantId, String strategyKey, Integer left, Integer right);

    /**
     * 执行 startSimulation 相关的风控处理逻辑。
     */
    Map<String, Object> startSimulation(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行 listSimulations 相关的风控处理逻辑。
     */
    List<Map<String, Object>> listSimulations(Long tenantId, String sceneKey, Integer limit);
}
