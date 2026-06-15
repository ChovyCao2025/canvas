package org.chovy.canvas.risk.api;

import java.util.List;
import java.util.Map;

public interface RiskGovernanceFacade {

    List<Map<String, Object>> decisionTraces(Long tenantId, String sceneKey, Integer limit);

    Map<String, Object> createList(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> addListEntry(Long tenantId, String listKey, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listEntries(Long tenantId, String listKey);

    Map<String, Object> removeListEntry(Long tenantId, String listKey, Long entryId, String actor);

    Map<String, Object> importListEntries(Long tenantId, String listKey, Map<String, Object> payload, String actor);

    Map<String, Object> createStrategyDraft(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> getStrategy(Long tenantId, String strategyKey);

    List<Map<String, Object>> listStrategyVersions(Long tenantId, String strategyKey);

    Map<String, Object> validateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    Map<String, Object> simulateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    Map<String, Object> submitStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    Map<String, Object> approveStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    Map<String, Object> activateStrategyVersion(Long tenantId, String strategyKey, Integer version, String actor);

    Map<String, Object> rollbackStrategy(Long tenantId, String strategyKey, Map<String, Object> payload,
                                         String actor);

    Map<String, Object> pauseStrategy(Long tenantId, String strategyKey, String actor);

    Map<String, Object> diffStrategyVersions(Long tenantId, String strategyKey, Integer left, Integer right);

    Map<String, Object> startSimulation(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listSimulations(Long tenantId, String sceneKey, Integer limit);
}
