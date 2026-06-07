package org.chovy.canvas.domain.search;

public record SearchMarketingSourceQuery(
        String provider,
        String channel,
        Boolean enabled,
        Integer limit) {
}
