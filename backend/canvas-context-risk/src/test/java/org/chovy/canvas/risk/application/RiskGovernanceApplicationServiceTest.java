package org.chovy.canvas.risk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 定义 RiskGovernanceApplicationServiceTest 的风控模块职责和数据契约。
 */
class RiskGovernanceApplicationServiceTest {

    /**
     * 执行 managesListsStrategiesDecisionTracesAndLabSimulationsPerTenant 相关的风控处理逻辑。
     */
    @Test
    void managesListsStrategiesDecisionTracesAndLabSimulationsPerTenant() {
        RiskGovernanceApplicationService service = new RiskGovernanceApplicationService();

        Map<String, Object> list = service.createList(42L, Map.of(
                "listKey", "coupon_abuse",
                "listType", "black",
                "subjectType", "user_id"),
                "ada");
        assertThat(list)
                .containsEntry("tenantId", 42L)
                .containsEntry("listKey", "coupon_abuse")
                .containsEntry("listType", "BLACK")
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "ada");

        Map<String, Object> entry = service.addListEntry(42L, "coupon_abuse", Map.of(
                "subjectValue", "user-1",
                "reason", "chargeback"),
                "ada");
        assertThat(entry).containsEntry("entryId", 1L).containsEntry("listKey", "coupon_abuse");
        assertThat(service.listEntries(42L, "coupon_abuse")).hasSize(1);
        assertThat(service.importListEntries(42L, "coupon_abuse", Map.of("values", "user-2,user-3"), "ada"))
                .containsEntry("importedCount", 2);
        assertThat(service.removeListEntry(42L, "coupon_abuse", 1L, "ada"))
                .containsEntry("removed", true);

        Map<String, Object> strategy = service.createStrategyDraft(42L, Map.of(
                "strategyKey", "benefit_default",
                "sceneKey", "MARKETING_BENEFIT_ISSUE",
                "name", "Benefit Default"),
                "ada");
        assertThat(strategy)
                .containsEntry("strategyKey", "benefit_default")
                .containsEntry("draftVersion", 1)
                .containsEntry("status", "DRAFT");
        assertThat(service.getStrategy(42L, "benefit_default")).containsEntry("sceneKey", "MARKETING_BENEFIT_ISSUE");
        assertThat(service.listStrategyVersions(42L, "benefit_default")).hasSize(1);
        assertThat(service.validateStrategyVersion(42L, "benefit_default", 1, "ada"))
                .containsEntry("validationStatus", "PASSED");
        assertThat(service.simulateStrategyVersion(42L, "benefit_default", 1, "ada"))
                .containsEntry("simulationStatus", "PASSED");
        assertThat(service.submitStrategyVersion(42L, "benefit_default", 1, "ada"))
                .containsEntry("status", "SUBMITTED");
        assertThat(service.approveStrategyVersion(42L, "benefit_default", 1, "ada"))
                .containsEntry("status", "APPROVED");
        assertThat(service.activateStrategyVersion(42L, "benefit_default", 1, "ada"))
                .containsEntry("activeVersion", 1)
                .containsEntry("status", "ACTIVE");
        assertThat(service.pauseStrategy(42L, "benefit_default", "ada")).containsEntry("status", "PAUSED");
        assertThat(service.rollbackStrategy(42L, "benefit_default", Map.of("targetVersion", 1), "ada"))
                .containsEntry("activeVersion", 1);
        assertThat(service.diffStrategyVersions(42L, "benefit_default", 1, 1))
                .containsEntry("changeCount", 0);

        assertThat(service.startSimulation(42L, Map.of(
                "sceneKey", "MARKETING_BENEFIT_ISSUE",
                "strategyKey", "benefit_default",
                "candidateVersion", 1),
                "ada"))
                .containsEntry("simulationId", 1L)
                .containsEntry("status", "COMPLETED");
        assertThat(service.listSimulations(42L, "MARKETING_BENEFIT_ISSUE", 10)).hasSize(1);
        assertThat(service.decisionTraces(42L, "MARKETING_BENEFIT_ISSUE", 10))
                .singleElement()
                .extracting(trace -> trace.get("sceneKey"))
                .isEqualTo("MARKETING_BENEFIT_ISSUE");
    }

    /**
     * 执行 validatesRequiredKeysAndDefaultsActor 相关的风控处理逻辑。
     */
    @Test
    void validatesRequiredKeysAndDefaultsActor() {
        RiskGovernanceApplicationService service = new RiskGovernanceApplicationService();

        assertThatThrownBy(() -> service.createList(7L, Map.of("listType", "black"), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("listKey is required");

        Map<String, Object> strategy = service.createStrategyDraft(null, Map.of("strategyKey", "s1"), "");
        assertThat(strategy).containsEntry("tenantId", 0L).containsEntry("updatedBy", "system");
    }
}
