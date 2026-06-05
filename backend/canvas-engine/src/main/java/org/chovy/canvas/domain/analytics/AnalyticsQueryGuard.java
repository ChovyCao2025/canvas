package org.chovy.canvas.domain.analytics;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Component
public class AnalyticsQueryGuard {

    static final int MAX_RANGE_DAYS = 366;
    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_PAGE_SIZE = 200;

    private static final Pattern ATTRIBUTE_PATH =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    public DateRange validateDateRange(String startDate, String endDate) {
        if (isBlank(startDate) || isBlank(endDate)) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        LocalDate start = parseDate("startDate", startDate);
        LocalDate end = parseDate("endDate", endDate);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("analytics date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
        return new DateRange(start.toString(), end.toString());
    }

    public Long requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new IllegalArgumentException("tenantId must be non-negative");
        }
        return tenantId;
    }

    public String requireUserId(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    public String requireEventCode(String eventCode) {
        if (isBlank(eventCode)) {
            throw new IllegalArgumentException("eventCode is required");
        }
        String value = eventCode.trim();
        if (value.length() > 128) {
            throw new IllegalArgumentException("eventCode cannot exceed 128 characters");
        }
        return value;
    }

    public String requireAttributePath(String attribute) {
        if (isBlank(attribute)) {
            throw new IllegalArgumentException("attribute is required");
        }
        String value = attribute.trim();
        if (value.length() > 128 || !ATTRIBUTE_PATH.matcher(value).matches()) {
            throw new IllegalArgumentException("attribute must be a dot-separated identifier path");
        }
        return value;
    }

    public PageRequest normalizePageRequest(Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = Math.multiplyExact(normalizedPage - 1, normalizedSize);
        return new PageRequest(normalizedPage, normalizedSize, offset);
    }

    private LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must use yyyy-MM-dd", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DateRange(String startDate, String endDate) {
    }

    public record PageRequest(int page, int size, int offset) {
    }
}
