package org.chovy.canvas.domain.analytics;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Component
/**
 * AnalyticsQueryGuard 承载对应领域的业务规则、流程编排和结果转换。
 */
public class AnalyticsQueryGuard {

    static final int MAX_RANGE_DAYS = 366;
    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_PAGE_SIZE = 200;

    private static final Pattern ATTRIBUTE_PATH =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    public DateRange validateDateRange(String startDate, String endDate) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new DateRange(start.toString(), end.toString());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 require tenant id 计算得到的数量、金额或指标值。
     */
    public Long requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new IllegalArgumentException("tenantId must be non-negative");
        }
        return tenantId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 require user id 生成的文本或业务键。
     */
    public String requireUserId(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 require event code 生成的文本或业务键。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param attribute attribute 参数，用于 requireAttributePath 流程中的校验、计算或对象转换。
     * @return 返回 require attribute path 生成的文本或业务键。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param page 分页或数量限制，避免一次处理过多数据。
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public PageRequest normalizePageRequest(Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = Math.multiplyExact(normalizedPage - 1, normalizedSize);
        return new PageRequest(normalizedPage, normalizedSize, offset);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must use yyyy-MM-dd", ex);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * DateRange 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DateRange(String startDate, String endDate) {
    }

    /**
     * PageRequest 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PageRequest(int page, int size, int offset) {
    }
}
