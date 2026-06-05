package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsQueryService {

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsQueryGuard guard;

    public AnalyticsQueryService(AnalyticsEventMapper eventMapper, AnalyticsQueryGuard guard) {
        this.eventMapper = eventMapper;
        this.guard = guard;
    }

    public List<EventCountRow> eventCounts(Long tenantId, String startDate, String endDate) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        return eventMapper.selectEventCounts(scopedTenantId, range.startDate(), range.endDate())
                .stream()
                .map(row -> new EventCountRow(asString(row.get("eventCode")), asLong(row.get("count"))))
                .toList();
    }

    public EventTotal countEvents(Long tenantId, String startDate, String endDate, String eventCode) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        long count = eventCode == null || eventCode.isBlank()
                ? eventMapper.countEvents(scopedTenantId, range.startDate(), range.endDate())
                : eventMapper.countByEventCode(scopedTenantId, guard.requireEventCode(eventCode), range.startDate(), range.endDate());
        return new EventTotal(count);
    }

    public PageResult<UserTimelineRow> userTimeline(Long tenantId,
                                                    String userId,
                                                    String startDate,
                                                    String endDate,
                                                    Integer page,
                                                    Integer size) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        String scopedUserId = guard.requireUserId(userId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        AnalyticsQueryGuard.PageRequest pageRequest = guard.normalizePageRequest(page, size);
        List<UserTimelineRow> rows = eventMapper.selectUserTimeline(
                        scopedTenantId,
                        scopedUserId,
                        range.startDate(),
                        range.endDate(),
                        pageRequest.offset(),
                        pageRequest.size())
                .stream()
                .map(row -> new UserTimelineRow(asString(row.get("eventCode")), asString(row.get("eventTime"))))
                .toList();
        long total = eventMapper.countUserTimeline(scopedTenantId, scopedUserId, range.startDate(), range.endDate());
        return PageResult.of(total, rows);
    }

    public List<AttributeDistributionRow> attributeDistribution(Long tenantId,
                                                                String attribute,
                                                                String startDate,
                                                                String endDate) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        String attributePath = guard.requireAttributePath(attribute);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        return eventMapper.selectAttributeDistribution(scopedTenantId, attributePath, range.startDate(), range.endDate())
                .stream()
                .map(row -> new AttributeDistributionRow(asString(row.get("value")), asLong(row.get("count"))))
                .toList();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    public record EventCountRow(String eventCode, long count) {
    }

    public record EventTotal(long count) {
    }

    public record UserTimelineRow(String eventCode, String eventTime) {
    }

    public record AttributeDistributionRow(String value, long count) {
    }
}
