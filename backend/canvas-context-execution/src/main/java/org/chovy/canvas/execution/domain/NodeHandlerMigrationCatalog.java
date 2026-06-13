package org.chovy.canvas.execution.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private NodeHandlerMigrationCatalog() {
    }

    public static Set<String> migratedControlTypes() {
        return MIGRATED_CONTROL_TYPES;
    }

    public static Map<String, DependencyGate> dependencyGatedTypes() {
        return DEPENDENCY_GATED_TYPES;
    }

    public record DependencyGate(String moduleArtifactId, List<String> apiTypes) {
        public DependencyGate {
            if (moduleArtifactId == null || moduleArtifactId.isBlank()) {
                throw new IllegalArgumentException("moduleArtifactId is required");
            }
            apiTypes = List.copyOf(apiTypes == null ? List.of() : apiTypes);
        }
    }
}
