package org.chovy.canvas.domain.monitoring;

import java.util.List;

public record MarketingMonitorIngestResult(MarketingMonitorItemView item,
                                           MarketingSentimentAnalysisView sentiment,
                                           List<MarketingCompetitorMentionView> competitorMentions,
                                           List<MarketingMonitorAlertView> alerts) {
}
