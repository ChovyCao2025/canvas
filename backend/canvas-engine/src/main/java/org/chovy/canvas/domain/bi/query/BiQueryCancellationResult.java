package org.chovy.canvas.domain.bi.query;

public record BiQueryCancellationResult(
        String sqlHash,
        boolean cancelled,
        String message
) {
}
