package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;
import java.util.List;

public interface BiDatasourceHealthProvider {

    List<BiDatasourceHealth> health();

    default List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        LocalDateTime checkedAt = LocalDateTime.now();
        return health().stream()
                .map(item -> new BiDatasourceHealthSnapshot(
                        item.sourceKey(),
                        item.sourceType(),
                        item.available(),
                        item.message(),
                        checkedAt))
                .limit(boundedLimit)
                .toList();
    }

    default BiDatasourceHealthSloSummary healthSlo(int limit) {
        return BiDatasourceHealthSloSummary.from(healthHistory(limit));
    }

    static BiDatasourceHealthProvider empty() {
        return List::of;
    }
}
