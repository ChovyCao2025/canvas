package org.chovy.canvas.domain.search;

public record SearchMarketingSyncRunQuery(
        Long sourceId,
        String runType,
        String status,
        Integer limit) {
}
