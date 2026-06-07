package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

public record BiDatasetFromDatasourceGraphCommand(
        String layoutMode,
        List<BiDatasetFromDatasourceGraphNodeCommand> nodes
) {
    public BiDatasetFromDatasourceGraphCommand {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }
}
