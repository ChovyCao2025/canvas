package org.chovy.canvas.bi.api;

public record BiQueryCancelResult(
        String sqlHash,
        boolean cancelled,
        String status) {
}
