package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiDatasetFromDatasourceGraphCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param layoutMode layoutMode 字段。
 * @param nodes nodes 字段。
 */
public record BiDatasetFromDatasourceGraphCommand(
        String layoutMode,
        List<BiDatasetFromDatasourceGraphNodeCommand> nodes
) {
    public BiDatasetFromDatasourceGraphCommand {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }
}
