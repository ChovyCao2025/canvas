package org.chovy.canvas.bi.api;

import java.util.List;

import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewView;
import org.chovy.canvas.bi.api.AnalyticsViews.AttributeDistributionView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventCountView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventTotalView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportJobView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.FunnelView;
import org.chovy.canvas.bi.api.AnalyticsViews.UserTimelineView;

public interface AnalyticsFacade {

    List<EventCountView> eventCounts(Long tenantId, String startDate, String endDate);

    EventTotalView countEvents(Long tenantId, String startDate, String endDate, String eventCode);

    UserTimelineView userTimeline(Long tenantId, String userId, String startDate, String endDate, Integer page, Integer size);

    List<AttributeDistributionView> attributeDistribution(Long tenantId, String attribute, String startDate, String endDate);

    FunnelView funnelResult(Long tenantId, String funnelKey, String startDate, String endDate);

    AlertPreviewView alertPreview(Long tenantId, AlertPreviewRequest request);

    ExportJobView createExport(Long tenantId, ExportRequest request);

    ExportJobView exportStatus(Long tenantId, Long exportId);
}
