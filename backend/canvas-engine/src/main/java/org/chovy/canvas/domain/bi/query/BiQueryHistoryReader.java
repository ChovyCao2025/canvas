package org.chovy.canvas.domain.bi.query;

import java.util.List;

@FunctionalInterface
public interface BiQueryHistoryReader {

    List<BiQueryHistoryItem> recent(Long tenantId, int limit);

    static BiQueryHistoryReader empty() {
        return (tenantId, limit) -> List.of();
    }
}
