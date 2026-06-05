package org.chovy.canvas.domain.bi.query;

@FunctionalInterface
public interface BiQueryHistoryRecorder {

    void record(BiQueryHistoryEntry entry);

    static BiQueryHistoryRecorder noop() {
        return ignored -> {
        };
    }
}
