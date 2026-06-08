package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiSqlDatasetImpactView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param impactedAssetTypes impactedAssetTypes 字段。
 * @param governanceGates governanceGates 字段。
 * @param warnings warnings 字段。
 */
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
