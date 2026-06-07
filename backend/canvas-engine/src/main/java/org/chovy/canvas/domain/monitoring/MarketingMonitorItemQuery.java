package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorItemQuery(String sentimentLabel,
                                        String competitorKey,
                                        int limit) {
}
