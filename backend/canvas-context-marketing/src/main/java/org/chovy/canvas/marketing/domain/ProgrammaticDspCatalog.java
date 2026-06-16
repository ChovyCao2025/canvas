package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护ProgrammaticDsp相关的内存业务目录。
 */
public class ProgrammaticDspCatalog {

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<Long, Map<String, Object>> seats = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> campaigns = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> lineItems = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> supplyPaths = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> snapshots = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> mutations = new LinkedHashMap<>();

    /**
     * 保存nextSeatId字段值。
     */
    private long nextSeatId = 1L;

    /**
     * 保存nextCampaignId字段值。
     */
    private long nextCampaignId = 1L;

    /**
     * 保存nextLineItemId字段值。
     */
    private long nextLineItemId = 1L;

    /**
     * 保存nextSupplyPathId字段值。
     */
    private long nextSupplyPathId = 1L;

    /**
     * 保存nextSnapshotId字段值。
     */
    private long nextSnapshotId = 1L;

    /**
     * 保存nextMutationId字段值。
     */
    private long nextMutationId = 1L;

    /**
     * 创建ProgrammaticDspCatalog实例。
     */
    public ProgrammaticDspCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 执行upsertSeat业务操作。
     */
    public synchronized Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long id = resolveId(safe, "id", nextSeatId);
        Map<String, Object> seat = seats.getOrDefault(id, baseRow(id, tenantId, actor));
        seat.put("tenantId", tenantId);
        seat.put("seatKey", text(safe.get("seatKey"), "seat-" + id));
        seat.put("name", text(safe.get("name"), "DSP Seat " + id));
        seat.put("status", text(safe.get("status"), "ACTIVE").toUpperCase());
        touch(seat, actor);
        seats.put(id, seat);
        nextSeatId = Math.max(nextSeatId, id + 1);
        return copy(seat);
    }

    /**
     * 执行upsertCampaign业务操作。
     */
    public synchronized Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long seatId = longValue(safe.get("seatId"), 1L);
        requireSeat(tenantId, seatId);
        Long id = resolveId(safe, "id", nextCampaignId);
        Map<String, Object> campaign = campaigns.getOrDefault(id, baseRow(id, tenantId, actor));
        campaign.put("tenantId", tenantId);
        campaign.put("seatId", seatId);
        campaign.put("campaignKey", text(safe.get("campaignKey"), "campaign-" + id));
        campaign.put("name", text(safe.get("name"), "DSP Campaign " + id));
        campaign.put("budget", doubleValue(safe.get("budget"), 0.0));
        campaign.put("status", text(safe.get("status"), "ACTIVE").toUpperCase());
        touch(campaign, actor);
        campaigns.put(id, campaign);
        nextCampaignId = Math.max(nextCampaignId, id + 1);
        return copy(campaign);
    }

    /**
     * 执行upsertLineItem业务操作。
     */
    public synchronized Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long campaignId = longValue(safe.get("campaignId"), 1L);
        requireCampaign(tenantId, campaignId);
        Long id = resolveId(safe, "id", nextLineItemId);
        Map<String, Object> lineItem = lineItems.getOrDefault(id, baseRow(id, tenantId, actor));
        lineItem.put("tenantId", tenantId);
        lineItem.put("campaignId", campaignId);
        lineItem.put("lineItemKey", text(safe.get("lineItemKey"), "line-item-" + id));
        lineItem.put("name", text(safe.get("name"), "DSP Line Item " + id));
        lineItem.put("dailyBudget", doubleValue(safe.get("dailyBudget"), 0.0));
        lineItem.put("bidCpm", doubleValue(safe.get("bidCpm"), 0.0));
        lineItem.put("status", text(safe.get("status"), "ACTIVE").toUpperCase());
        touch(lineItem, actor);
        lineItems.put(id, lineItem);
        nextLineItemId = Math.max(nextLineItemId, id + 1);
        return copy(lineItem);
    }

    /**
     * 执行upsertSupplyPath业务操作。
     */
    public synchronized Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long seatId = longValue(safe.get("seatId"), 1L);
        requireSeat(tenantId, seatId);
        Long id = resolveId(safe, "id", nextSupplyPathId);
        Map<String, Object> supplyPath = supplyPaths.getOrDefault(id, baseRow(id, tenantId, actor));
        supplyPath.put("tenantId", tenantId);
        supplyPath.put("seatId", seatId);
        supplyPath.put("supplyPathKey", text(safe.get("supplyPathKey"), "supply-path-" + id));
        supplyPath.put("exchange", text(safe.get("exchange"), "default-exchange"));
        supplyPath.put("priority", intValue(safe.get("priority"), 1));
        supplyPath.put("status", text(safe.get("status"), "ACTIVE").toUpperCase());
        touch(supplyPath, actor);
        supplyPaths.put(id, supplyPath);
        nextSupplyPathId = Math.max(nextSupplyPathId, id + 1);
        return copy(supplyPath);
    }

    /**
     * 执行recordSnapshot业务操作。
     */
    public synchronized Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long lineItemId = longValue(safe.get("lineItemId"), 1L);
        requireLineItem(tenantId, lineItemId);
        Long id = nextSnapshotId++;
        Map<String, Object> snapshot = mapOf(
                "id", id,
                "tenantId", tenantId,
                "lineItemId", lineItemId,
                "impressions", longValue(safe.get("impressions"), 0L),
                "clicks", longValue(safe.get("clicks"), 0L),
                "spend", doubleValue(safe.get("spend"), 0.0),
                "recordedBy", actor,
                "recordedAt", now());
        snapshots.put(id, snapshot);
        return copy(snapshot);
    }

    /**
     * 执行summary业务操作。
     */
    public synchronized Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        Map<String, Object> safe = safePayload(query);
        Long seatId = nullableLong(safe.get("seatId"));
        Long campaignId = nullableLong(safe.get("campaignId"));
        Long lineItemId = nullableLong(safe.get("lineItemId"));
        LocalDate startDate = nullableDate(safe.get("startDate"));
        LocalDate endDate = nullableDate(safe.get("endDate"));
        LocalDateTime evaluatedAt = nullableDateTime(safe.get("evaluatedAt"));

        List<Map<String, Object>> scopedLineItems = lineItems.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> campaignId == null || campaignId.equals(row.get("campaignId")))
                .filter(row -> lineItemId == null || lineItemId.equals(row.get("id")))
                .filter(row -> seatId == null || campaignBelongsToSeat(tenantId, (Long) row.get("campaignId"), seatId))
                .toList();
        List<Long> scopedLineItemIds = scopedLineItems.stream()
                .map(row -> (Long) row.get("id"))
                .toList();
        List<Map<String, Object>> scopedSnapshots = snapshots.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> lineItemId == null || lineItemId.equals(row.get("lineItemId")))
                .filter(row -> scopedLineItemIds.contains((Long) row.get("lineItemId")))
                .toList();

        return mapOf(
                "tenantId", tenantId,
                "seatId", seatId,
                "campaignId", campaignId,
                "lineItemId", lineItemId,
                "startDate", startDate,
                "endDate", endDate,
                "evaluatedAt", evaluatedAt,
                "seatCount", countByTenant(seats, tenantId),
                "campaignCount", countCampaigns(tenantId, seatId, campaignId),
                "lineItemCount", scopedLineItems.size(),
                "supplyPathCount", countSupplyPaths(tenantId, seatId),
                "snapshotCount", scopedSnapshots.size(),
                "impressions", scopedSnapshots.stream().mapToLong(row -> (Long) row.get("impressions")).sum(),
                "clicks", scopedSnapshots.stream().mapToLong(row -> (Long) row.get("clicks")).sum(),
                "spend", scopedSnapshots.stream().mapToDouble(row -> (Double) row.get("spend")).sum());
    }

    /**
     * 执行proposeMutation业务操作。
     */
    public synchronized Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long lineItemId = longValue(safe.get("lineItemId"), 1L);
        requireLineItem(tenantId, lineItemId);
        Long id = nextMutationId++;
        Map<String, Object> mutation = mapOf(
                "id", id,
                "tenantId", tenantId,
                "lineItemId", lineItemId,
                "mutationType", text(safe.get("mutationType"), "BID_UPDATE").toUpperCase(),
                "dryRun", boolValue(safe.get("dryRun"), true),
                "payload", safePayload(nestedMap(safe.get("payload"))),
                "status", "PROPOSED",
                "approvalStatus", "PENDING",
                "createdBy", actor,
                "createdAt", now(),
                "updatedBy", actor,
                "updatedAt", now());
        mutations.put(id, mutation);
        return copy(mutation);
    }

    /**
     * 执行approveMutation业务操作。
     */
    public synchronized Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                            String actor) {
        Map<String, Object> mutation = requireMutation(tenantId, mutationId);
        mutation.put("status", "APPROVED");
        mutation.put("approvalStatus", "APPROVED");
        mutation.put("approvalComment", text(safePayload(payload).get("comment"), ""));
        mutation.put("approvedBy", actor);
        mutation.put("approvedAt", now());
        touch(mutation, actor);
        return copy(mutation);
    }

    /**
     * 执行executeMutation业务操作。
     */
    public synchronized Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                                            String actor) {
        Map<String, Object> mutation = requireMutation(tenantId, mutationId);
        if (!"APPROVED".equals(mutation.get("approvalStatus"))) {
            mutation.put("approvalStatus", "APPROVED");
            mutation.put("approvedBy", actor);
            mutation.put("approvedAt", now());
        }
        mutation.put("status", "EXECUTED");
        mutation.put("providerRequestId", text(safePayload(payload).get("providerRequestId"), "local-" + mutationId));
        mutation.put("executedBy", actor);
        mutation.put("executedAt", now());
        touch(mutation, actor);
        return copy(mutation);
    }

    /**
     * 查询mutations列表。
     */
    public synchronized List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query) {
        Map<String, Object> safe = safePayload(query);
        Long lineItemId = nullableLong(safe.get("lineItemId"));
        String status = nullableUpper(safe.get("status"));
        String approvalStatus = nullableUpper(safe.get("approvalStatus"));
        int limit = Math.max(1, Math.min(intValue(safe.get("limit"), 50), 100));
        return mutations.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> lineItemId == null || lineItemId.equals(row.get("lineItemId")))
                .filter(row -> status == null || status.equals(row.get("status")))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.get("approvalStatus")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id"), Comparator.reverseOrder()))
                .limit(limit)
                .map(ProgrammaticDspCatalog::copy)
                .toList();
    }

    /**
     * 执行baseRow业务操作。
     */
    private Map<String, Object> baseRow(Long id, Long tenantId, String actor) {
        return mapOf(
                "id", id,
                "tenantId", tenantId,
                "createdBy", actor,
                "createdAt", now(),
                "updatedBy", actor,
                "updatedAt", now());
    }

    /**
     * 转换为uch对象。
     */
    private void touch(Map<String, Object> row, String actor) {
        row.putIfAbsent("createdBy", actor);
        row.putIfAbsent("createdAt", now());
        row.put("updatedBy", actor);
        row.put("updatedAt", now());
    }

    /**
     * 校验并返回seat必填值。
     */
    private Map<String, Object> requireSeat(Long tenantId, Long seatId) {
        Map<String, Object> seat = seats.get(seatId);
        if (seat == null || !tenantId.equals(seat.get("tenantId"))) {
            throw new IllegalArgumentException("DSP seat not found: " + seatId);
        }
        return seat;
    }

    /**
     * 校验并返回campaign必填值。
     */
    private Map<String, Object> requireCampaign(Long tenantId, Long campaignId) {
        Map<String, Object> campaign = campaigns.get(campaignId);
        if (campaign == null || !tenantId.equals(campaign.get("tenantId"))) {
            throw new IllegalArgumentException("DSP campaign not found: " + campaignId);
        }
        return campaign;
    }

    /**
     * 校验并返回lineItem必填值。
     */
    private Map<String, Object> requireLineItem(Long tenantId, Long lineItemId) {
        Map<String, Object> lineItem = lineItems.get(lineItemId);
        if (lineItem == null || !tenantId.equals(lineItem.get("tenantId"))) {
            throw new IllegalArgumentException("DSP line item not found: " + lineItemId);
        }
        return lineItem;
    }

    /**
     * 校验并返回mutation必填值。
     */
    private Map<String, Object> requireMutation(Long tenantId, Long mutationId) {
        Map<String, Object> mutation = mutations.get(mutationId);
        if (mutation == null || !tenantId.equals(mutation.get("tenantId"))) {
            throw new IllegalArgumentException("DSP mutation not found: " + mutationId);
        }
        return mutation;
    }

    /**
     * 执行campaignBelongsToSeat业务操作。
     */
    private boolean campaignBelongsToSeat(Long tenantId, Long campaignId, Long seatId) {
        Map<String, Object> campaign = campaigns.get(campaignId);
        return campaign != null && tenantId.equals(campaign.get("tenantId")) && seatId.equals(campaign.get("seatId"));
    }

    /**
     * 执行countByTenant业务操作。
     */
    private int countByTenant(Map<Long, Map<String, Object>> rows, Long tenantId) {
        return (int) rows.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .count();
    }

    /**
     * 执行countCampaigns业务操作。
     */
    private int countCampaigns(Long tenantId, Long seatId, Long campaignId) {
        return (int) campaigns.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> seatId == null || seatId.equals(row.get("seatId")))
                .filter(row -> campaignId == null || campaignId.equals(row.get("id")))
                .count();
    }

    /**
     * 执行countSupplyPaths业务操作。
     */
    private int countSupplyPaths(Long tenantId, Long seatId) {
        return (int) supplyPaths.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> seatId == null || seatId.equals(row.get("seatId")))
                .count();
    }

    /**
     * 执行now业务操作。
     */
    private String now() {
        return Instant.now(clock).toString();
    }

    /**
     * 执行resolveId业务操作。
     */
    private static Long resolveId(Map<String, Object> payload, String key, Long fallback) {
        return Math.max(1L, longValue(payload.get(key), fallback));
    }

    /**
     * 执行nullableLong业务操作。
     */
    private static Long nullableLong(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return longValue(value, null);
    }

    /**
     * 执行longValue业务操作。
     */
    private static Long longValue(Object value, Long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + value, ex);
        }
    }

    /**
     * 执行intValue业务操作。
     */
    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }

    /**
     * 执行doubleValue业务操作。
     */
    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return fallback;
    }

    /**
     * 执行boolValue业务操作。
     */
    private static boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    /**
     * 执行text业务操作。
     */
    private static String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 执行nullableUpper业务操作。
     */
    private static String nullableUpper(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim().toUpperCase();
    }

    /**
     * 执行nullableDate业务操作。
     */
    private static LocalDate nullableDate(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date: " + value, ex);
        }
    }

    /**
     * 执行nullableDateTime业务操作。
     */
    private static LocalDateTime nullableDateTime(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date-time: " + value, ex);
        }
    }

    /**
     * 执行nestedMap业务操作。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return Map.of();
    }

    /**
     * 执行safePayload业务操作。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    /**
     * 执行copy业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 执行mapOf业务操作。
     */
    private static Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }
}
