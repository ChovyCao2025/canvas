package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeHandlerSetGovernanceTest 的执行上下文数据结构或业务契约。
 */
class NodeHandlerSetGovernanceTest {

    /**
     * 执行 executionRegistersMigratedPureControlHandlers 对应的业务处理。
     */
    @Test
    void executionRegistersMigratedPureControlHandlers() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(
                new StartNodeHandler(),
                new EndNodeHandler(),
                new IfConditionNodeHandler(),
                new WaitNodeHandler(),
                new UserInputNodeHandler(),
                new DirectCallNodeHandler(),
                new DirectReturnNodeHandler(),
                new SplitNodeHandler(),
                new AggregateNodeHandler()));

        assertThat(NodeHandlerMigrationCatalog.migratedControlTypes())
                .containsExactlyInAnyOrder(
                        "START",
                        "END",
                        "IF_CONDITION",
                        "WAIT",
                        "USER_INPUT",
                        "DIRECT_CALL",
                        "DIRECT_RETURN",
                        "SPLIT",
                        "AGGREGATE");
        assertThat(NodeHandlerMigrationCatalog.migratedControlTypes())
                .allSatisfy(type -> assertThat(registry.has(type)).as(type).isTrue());
    }

    /**
     * 执行 externalProviderHandlersDeclareMissingAuthorizedApiDependencies 对应的业务处理。
     */
    @Test
    void externalProviderHandlersDeclareMissingAuthorizedApiDependencies() {
        assertThat(NodeHandlerMigrationCatalog.dependencyGatedTypes())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "RISK_DECISION", new NodeHandlerMigrationCatalog.DependencyGate(
                                "canvas-context-risk",
                                List.of(
                                        "org.chovy.canvas.risk.api.RiskDecisionFacade",
                                        "org.chovy.canvas.risk.api.RiskDecisionCommand",
                                        "org.chovy.canvas.risk.api.RiskDecisionView")),
                        "TAGGER", new NodeHandlerMigrationCatalog.DependencyGate(
                                "canvas-context-cdp",
                                List.of(
                                        "org.chovy.canvas.cdp.api.AudienceSnapshotFacade",
                                        "org.chovy.canvas.cdp.api.AudienceSnapshotLockCommand",
                                        "org.chovy.canvas.cdp.api.AudienceSnapshotView",
                                        "org.chovy.canvas.cdp.api.CdpTagFacade",
                                        "org.chovy.canvas.cdp.api.CdpTagWriteCommand",
                                        "org.chovy.canvas.cdp.api.CdpUserTagView",
                                        "org.chovy.canvas.cdp.api.CustomerProfileLookupPort",
                                        "org.chovy.canvas.cdp.api.CdpCustomerProfileView"))));
    }
}
