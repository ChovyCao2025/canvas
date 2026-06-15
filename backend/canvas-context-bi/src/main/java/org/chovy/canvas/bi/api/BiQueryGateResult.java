package org.chovy.canvas.bi.api;

public record BiQueryGateResult(
        boolean allowed,
        String status,
        String reason,
        BiQueryResultView queryResult) {
}
