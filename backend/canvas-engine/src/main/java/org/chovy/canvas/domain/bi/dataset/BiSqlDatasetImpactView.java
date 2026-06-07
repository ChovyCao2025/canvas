package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiSqlDatasetImpactView(
        List<String> impactedAssetTypes,
        List<String> governanceGates,
        List<String> warnings
) {
    public BiSqlDatasetImpactView {
        impactedAssetTypes = impactedAssetTypes == null ? List.of() : List.copyOf(impactedAssetTypes);
        governanceGates = governanceGates == null ? List.of() : List.copyOf(governanceGates);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
