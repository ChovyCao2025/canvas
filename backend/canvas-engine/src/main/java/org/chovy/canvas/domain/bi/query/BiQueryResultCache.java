package org.chovy.canvas.domain.bi.query;

import java.time.Duration;
import java.util.Optional;

public interface BiQueryResultCache {

    Optional<BiQueryResult> get(String sqlHash);

    void put(String sqlHash, BiQueryResult result);

    default void put(String sqlHash, BiQueryResult result, Duration ttl) {
        put(sqlHash, result);
    }

    default boolean evict(String sqlHash) {
        return false;
    }

    default int evictDataset(String datasetKey) {
        return 0;
    }

    default int clear() {
        return 0;
    }

    default BiQueryCacheStats stats() {
        return new BiQueryCacheStats("noop", false, 0, 0, 0, 0, 0, 0, 0);
    }

    static BiQueryResultCache noop() {
        return new BiQueryResultCache() {
            @Override
            public Optional<BiQueryResult> get(String sqlHash) {
                return Optional.empty();
            }

            @Override
            public void put(String sqlHash, BiQueryResult result) {
            }
        };
    }
}
