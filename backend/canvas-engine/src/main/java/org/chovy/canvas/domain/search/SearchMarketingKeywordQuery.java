package org.chovy.canvas.domain.search;

public record SearchMarketingKeywordQuery(
        String channel,
        String status,
        Integer limit) {
}
