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
/**
 * AnalyticsFacade 门面接口。
 */
public interface AnalyticsFacade {
    /**
     * 执行 event Counts 相关处理。
     */

    List<EventCountView> eventCounts(Long tenantId, String startDate, String endDate);
    /**
     * 执行 count Events 相关处理。
     */

    EventTotalView countEvents(Long tenantId, String startDate, String endDate, String eventCode);
    /**
     * 执行 user Timeline 相关处理。
     */

    UserTimelineView userTimeline(Long tenantId, String userId, String startDate, String endDate, Integer page, Integer size);
    /**
     * 执行 attribute Distribution 相关处理。
     */

    List<AttributeDistributionView> attributeDistribution(Long tenantId, String attribute, String startDate, String endDate);
    /**
     * 执行 funnel Result 相关处理。
     */

    FunnelView funnelResult(Long tenantId, String funnelKey, String startDate, String endDate);
    /**
     * 执行 alert Preview 相关处理。
     */

    AlertPreviewView alertPreview(Long tenantId, AlertPreviewRequest request);
    /**
     * 执行 create Export 相关处理。
     */

    ExportJobView createExport(Long tenantId, ExportRequest request);
    /**
     * 执行 export Status 相关处理。
     */

    ExportJobView exportStatus(Long tenantId, Long exportId);
}
