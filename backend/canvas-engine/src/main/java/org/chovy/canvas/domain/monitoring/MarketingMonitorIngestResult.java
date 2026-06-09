package org.chovy.canvas.domain.monitoring;

import java.util.List;

/**
 * MarketingMonitorIngestResult 承载 domain.monitoring 场景中的不可变数据快照。
 * @param item item 字段。
 * @param sentiment sentiment 字段。
 * @param competitorMentions competitorMentions 字段。
 * @param alerts alerts 字段。
 */
public record MarketingMonitorIngestResult(MarketingMonitorItemView item,
                                           MarketingSentimentAnalysisView sentiment,
                                           List<MarketingCompetitorMentionView> competitorMentions,
                                           List<MarketingMonitorAlertView> alerts) {
}
