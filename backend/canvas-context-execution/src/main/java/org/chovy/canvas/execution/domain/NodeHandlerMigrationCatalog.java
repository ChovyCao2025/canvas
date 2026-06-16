package org.chovy.canvas.execution.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定义 NodeHandlerMigrationCatalog 的执行上下文数据结构或业务契约。
 */
public final class NodeHandlerMigrationCatalog {

    private static final Set<String> MIGRATED_CONTROL_TYPES = Set.of(
            "START",
            "END",
            "IF_CONDITION",
            "WAIT",
            "USER_INPUT",
            "DIRECT_CALL",
            "DIRECT_RETURN",
            "SPLIT",
            "AGGREGATE");

    private static final Map<String, DependencyGate> DEPENDENCY_GATED_TYPES = Map.of(
            "RISK_DECISION", new DependencyGate(
                    "canvas-context-risk",
                    List.of(
                            "org.chovy.canvas.risk.api.RiskDecisionFacade",
                            "org.chovy.canvas.risk.api.RiskDecisionCommand",
                            "org.chovy.canvas.risk.api.RiskDecisionView")),
            "TAGGER", new DependencyGate(
                    "canvas-context-cdp",
                    List.of(
                            "org.chovy.canvas.cdp.api.AudienceSnapshotFacade",
                            "org.chovy.canvas.cdp.api.AudienceSnapshotLockCommand",
                            "org.chovy.canvas.cdp.api.AudienceSnapshotView",
                            "org.chovy.canvas.cdp.api.CdpTagFacade",
                            "org.chovy.canvas.cdp.api.CdpTagWriteCommand",
                            "org.chovy.canvas.cdp.api.CdpUserTagView",
                            "org.chovy.canvas.cdp.api.CustomerProfileLookupPort",
                            "org.chovy.canvas.cdp.api.CdpCustomerProfileView")));

    /**
     * 执行 NodeHandlerMigrationCatalog 对应的业务处理。
     */
    private NodeHandlerMigrationCatalog() {
    }

    /**
     * 执行 migratedControlTypes 对应的业务处理。
     * @return 处理后的结果
     */
    public static Set<String> migratedControlTypes() {
        return MIGRATED_CONTROL_TYPES;
    }

    /**
     * 执行 dependencyGatedTypes 对应的业务处理。
     * @return 处理后的结果
     */
    public static Map<String, DependencyGate> dependencyGatedTypes() {
        return DEPENDENCY_GATED_TYPES;
    }

    /**
     * 定义 DependencyGate 的执行上下文数据结构或业务契约。
     * @param moduleArtifactId moduleArtifactId 对应的数据字段
     * @param apiTypes apiTypes 对应的数据字段
     */
    public record DependencyGate(String moduleArtifactId, List<String> apiTypes) {
        public DependencyGate {
            if (moduleArtifactId == null || moduleArtifactId.isBlank()) {
                throw new IllegalArgumentException("moduleArtifactId is required");
            }
            apiTypes = List.copyOf(apiTypes == null ? List.of() : apiTypes);
        }
    }
}
