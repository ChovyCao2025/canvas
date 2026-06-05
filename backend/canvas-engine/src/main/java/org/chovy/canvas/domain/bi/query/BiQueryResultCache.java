package org.chovy.canvas.domain.bi.query;

import java.util.Optional;

public interface BiQueryResultCache {

    Optional<BiQueryResult> get(String sqlHash);

    void put(String sqlHash, BiQueryResult result);

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
