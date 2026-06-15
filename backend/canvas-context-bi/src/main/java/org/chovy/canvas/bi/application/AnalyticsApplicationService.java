package org.chovy.canvas.bi.application;

import java.util.List;

import org.chovy.canvas.bi.api.AnalyticsFacade;
import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.AlertPreviewView;
import org.chovy.canvas.bi.api.AnalyticsViews.AttributeDistributionView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventCountView;
import org.chovy.canvas.bi.api.AnalyticsViews.EventTotalView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportJobView;
import org.chovy.canvas.bi.api.AnalyticsViews.ExportRequest;
import org.chovy.canvas.bi.api.AnalyticsViews.FunnelStepView;
import org.chovy.canvas.bi.api.AnalyticsViews.FunnelView;
import org.chovy.canvas.bi.api.AnalyticsViews.UserTimelineItemView;
import org.chovy.canvas.bi.api.AnalyticsViews.UserTimelineView;
import org.chovy.canvas.bi.domain.AnalyticsCatalog;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsApplicationService implements AnalyticsFacade {

    private static final Long EXPORT_ID = 9001L;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 50;
    private static final int DEFAULT_ROW_LIMIT = 100_000;

    private final AnalyticsCatalog catalog;

    public AnalyticsApplicationService() {
        this(new AnalyticsCatalog());
    }

    AnalyticsApplicationService(AnalyticsCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<EventCountView> eventCounts(Long tenantId, String startDate, String endDate) {
        Long scopedTenantId = requireTenantId(tenantId);
        requireDateRange(startDate, endDate);
        return catalog.eventCounts().stream()
                .map(row -> new EventCountView(scopedTenantId, row.eventCode(), row.count()))
                .toList();
    }

    @Override
    public EventTotalView countEvents(Long tenantId, String startDate, String endDate, String eventCode) {
        Long scopedTenantId = requireTenantId(tenantId);
        requireDateRange(startDate, endDate);
        return new EventTotalView(scopedTenantId, eventCode, catalog.countFor(eventCode));
    }

    @Override
    public UserTimelineView userTimeline(Long tenantId,
                                         String userId,
                                         String startDate,
                                         String endDate,
                                         Integer page,
                                         Integer size) {
        Long scopedTenantId = requireTenantId(tenantId);
        String scopedUserId = requireText("userId", userId);
        requireDateRange(startDate, endDate);
        int normalizedPage = normalizePositive(page, DEFAULT_PAGE);
        int normalizedSize = Math.min(normalizePositive(size, DEFAULT_SIZE), DEFAULT_SIZE);
        List<UserTimelineItemView> records = catalog.timeline(scopedUserId).stream()
                .skip((long) (normalizedPage - 1) * normalizedSize)
                .limit(normalizedSize)
                .map(row -> new UserTimelineItemView(row.userId(), row.eventCode(), row.eventTime()))
                .toList();
        return new UserTimelineView(scopedTenantId, scopedUserId, normalizedPage, normalizedSize, 3L, records);
    }

    @Override
    public List<AttributeDistributionView> attributeDistribution(Long tenantId,
                                                                 String attribute,
                                                                 String startDate,
                                                                 String endDate) {
        Long scopedTenantId = requireTenantId(tenantId);
        String scopedAttribute = requireText("attribute", attribute);
        requireDateRange(startDate, endDate);
        return catalog.distribution(scopedAttribute).stream()
                .map(row -> new AttributeDistributionView(scopedTenantId, row.attribute(), row.value(), row.count()))
                .toList();
    }

    @Override
    public FunnelView funnelResult(Long tenantId, String funnelKey, String startDate, String endDate) {
        Long scopedTenantId = requireTenantId(tenantId);
        String scopedFunnelKey = requireText("funnelKey", funnelKey);
        requireDateRange(startDate, endDate);
        List<FunnelStepView> steps = catalog.funnelSteps().stream()
                .map(step -> new FunnelStepView(step.stepKey(), step.name(), step.users(), step.conversionRate()))
                .toList();
        return new FunnelView(scopedTenantId, scopedFunnelKey, startDate, endDate, steps);
    }

    @Override
    public AlertPreviewView alertPreview(Long tenantId, AlertPreviewRequest request) {
        Long scopedTenantId = requireTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String ruleKey = requireText("ruleKey", request.ruleKey());
        String eventCode = requireText("eventCode", request.eventCode());
        requireDateRange(request.startDate(), request.endDate());
        long threshold = request.threshold() == null || request.threshold() <= 0 ? 1L : request.threshold();
        long count = catalog.countFor(eventCode);
        return new AlertPreviewView(
                scopedTenantId,
                ruleKey,
                eventCode,
                request.startDate(),
                request.endDate(),
                count,
                threshold,
                count >= threshold);
    }

    @Override
    public ExportJobView createExport(Long tenantId, ExportRequest request) {
        Long scopedTenantId = requireTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String reportType = requireText("reportType", request.reportType());
        requireDateRange(request.startDate(), request.endDate());
        int rowLimit = normalizePositive(request.rowLimit(), DEFAULT_ROW_LIMIT);
        long estimatedRows = catalog.countFor(request.eventCode());
        return new ExportJobView(
                EXPORT_ID,
                scopedTenantId,
                reportType,
                request.eventCode(),
                "QUEUED",
                rowLimit,
                estimatedRows,
                catalog.query(reportType, request.eventCode(), request.startDate(), request.endDate(), estimatedRows));
    }

    @Override
    public ExportJobView exportStatus(Long tenantId, Long exportId) {
        Long scopedTenantId = requireTenantId(tenantId);
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        return new ExportJobView(
                exportId,
                scopedTenantId,
                "events",
                "purchase",
                "QUEUED",
                1000,
                catalog.countFor("purchase"),
                catalog.query("events", "purchase", "2026-06-01", "2026-06-07", catalog.countFor("purchase")));
    }

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return tenantId;
    }

    private void requireDateRange(String startDate, String endDate) {
        requireText("startDate", startDate);
        requireText("endDate", endDate);
    }

    private String requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private int normalizePositive(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value < 1 ? defaultValue : value;
    }
}
