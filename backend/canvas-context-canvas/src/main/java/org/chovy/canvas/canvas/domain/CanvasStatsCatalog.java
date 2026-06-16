package org.chovy.canvas.canvas.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装CanvasStatsCatalog相关的业务逻辑。
 */
public class CanvasStatsCatalog {

    /**
     * 保存时钟。
     */
    private final Clock clock;

    /**
     * 创建当前对象实例。
     */
    public CanvasStatsCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 处理trace。
     */
    public List<Map<String, Object>> trace(Long canvasId, String executionId) {
        requireCanvasId(canvasId);
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        return List.of(row(
                "executionId", executionId,
                "nodeId", "start",
                "nodeType", "TRIGGER",
                "nodeName", "Entry",
                "status", 2,
                "errorMsg", null,
                "outputData", "{}",
                "durationMs", 0L));
    }

    /**
     * 处理recentExecutions。
     */
    public List<Map<String, Object>> recentExecutions(Long canvasId, int size) {
        requireCanvasId(canvasId);
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        return List.of(row(
                "id", "exec-" + canvasId,
                "triggerType", "MANUAL",
                "status", 2,
                "userId", "system",
                "createdAt", LocalDate.now(clock).atStartOfDay().toString()));
    }

    /**
     * 处理stats。
     */
    public Map<String, Object> stats(Long canvasId, int days, String since, String until) {
        requireCanvasId(canvasId);
        validateRange(days, since, until);
        return row(
                "total", 1L,
                "success", 1L,
                "failed", 0L,
                "paused", 0L,
                "successRate", "100.0%",
                "uniqueUsers", 1L);
    }

    /**
     * 处理funnel。
     */
    public List<Map<String, Object>> funnel(Long canvasId) {
        requireCanvasId(canvasId);
        return List.of(row(
                "nodeId", "start",
                "nodeType", "TRIGGER",
                "nodeName", "Entry",
                "totalEntered", 1L,
                "totalSuccess", 1L,
                "totalFailed", 0L,
                "totalSkipped", 0L,
                "avgDurationMs", 0L,
                "avgDurationSec", 0.0));
    }

    /**
     * 处理trend。
     */
    public List<Map<String, Object>> trend(Long canvasId, int days, String since, String until) {
        requireCanvasId(canvasId);
        DateRange range = validateRange(days, since, until);
        return range.since().datesUntil(range.until().plusDays(1))
                .map(date -> row("date", date.toString(), "count", date.equals(range.until()) ? 1L : 0L))
                .toList();
    }

    /**
     * 处理receipts。
     */
    public Map<String, Object> receipts(Long canvasId) {
        requireCanvasId(canvasId);
        return row("delivered", 1L, "failed", 0L);
    }

    /**
     * 处理attributionSummary。
     */
    public Map<String, Object> attributionSummary(Long canvasId) {
        requireCanvasId(canvasId);
        return row(
                "conversions", 0L,
                "conversionAmount", "0",
                "attributedSends", 0L,
                "model", "LAST_TOUCH",
                "models", "LAST_TOUCH");
    }

    /**
     * 处理validateRange。
     */
    private DateRange validateRange(int days, String since, String until) {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("days must be between 1 and 365");
        }
        LocalDate untilDate = until == null || until.isBlank() ? LocalDate.now(clock) : LocalDate.parse(until);
        LocalDate sinceDate = since == null || since.isBlank() ? untilDate.minusDays(days - 1L) : LocalDate.parse(since);
        if (sinceDate.isAfter(untilDate)) {
            throw new IllegalArgumentException("since must be on or before until");
        }
        return new DateRange(sinceDate, untilDate);
    }

    /**
     * 校验并返回画布标识。
     */
    private static void requireCanvasId(Long canvasId) {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId must be positive");
        }
    }

    /**
     * 处理row。
     */
    private static Map<String, Object> row(Object... keysAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }

    /**
     * 承载DateRange的数据快照。
     */
    private record DateRange(LocalDate since, LocalDate until) {
    }
}
