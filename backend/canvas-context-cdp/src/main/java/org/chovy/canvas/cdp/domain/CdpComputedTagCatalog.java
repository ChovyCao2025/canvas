package org.chovy.canvas.cdp.domain;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpComputedTagCatalog {

    private final Clock clock;
    private final Map<String, ComputedTag> tags = new ConcurrentHashMap<>();
    private final Map<String, List<TagRun>> runs = new ConcurrentHashMap<>();
    private final AtomicLong tagIds = new AtomicLong();

    public CdpComputedTagCatalog(Clock clock) {
        this.clock = clock;
    }

    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = tags.values().stream()
                .filter(tag -> Objects.equals(tag.tenantId, tenantId))
                .sorted(Comparator.comparing(tag -> tag.tagCode))
                .map(this::tagView)
                .toList();
        return page(records);
    }

    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        String tagCode = requiredText(payload, "tagCode");
        String key = key(tenantId, tagCode);
        ComputedTag existing = tags.get(key);
        ComputedTag tag = new ComputedTag(
                existing == null ? tagIds.incrementAndGet() : existing.id,
                tenantId,
                tagCode,
                text(payload, "displayName", tagCode),
                text(payload, "valueType", "STRING"),
                text(payload, "computeType", "EXPRESSION"),
                text(payload, "expressionJson", "{}"),
                text(payload, "refreshMode", "MANUAL"),
                dependencies(payload),
                existing == null ? "DRAFT" : existing.status,
                actor,
                now());
        tags.put(key, tag);
        return tagView(tag);
    }

    public Map<String, Object> preview(Long tenantId, String tagCode) {
        ComputedTag tag = find(tenantId, tagCode);
        Map<String, Object> view = baseTagOperation(tag, "preview");
        view.put("matchedProfileCount", 42L);
        view.put("sampleValues", List.of(sampleValue(tag, 1), sampleValue(tag, 2), sampleValue(tag, 3)));
        return view;
    }

    public Map<String, Object> activate(Long tenantId, String tagCode, String actor) {
        return transition(tenantId, tagCode, "ACTIVE", actor);
    }

    public Map<String, Object> pause(Long tenantId, String tagCode, String actor) {
        return transition(tenantId, tagCode, "PAUSED", actor);
    }

    public Map<String, Object> runNow(Long tenantId, String tagCode, String actor) {
        ComputedTag tag = find(tenantId, tagCode);
        List<TagRun> tenantRuns = runs.computeIfAbsent(key(tenantId, tagCode), ignored -> new ArrayList<>());
        long sequence = tenantRuns.size() + 1L;
        TagRun run = new TagRun(sequence, tag, actor, "SUCCESS", now());
        tenantRuns.add(run);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("runId", run.runId());
        view.put("status", run.status);
        view.put("triggeredBy", actor);
        view.put("runBy", actor);
        view.put("startedAt", run.startedAt);
        view.put("finishedAt", run.startedAt);
        view.put("affectedProfileCount", 42L);
        return view;
    }

    public Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit) {
        find(tenantId, tagCode);
        List<Map<String, Object>> records = runs.getOrDefault(key(tenantId, tagCode), List.of()).stream()
                .limit(limit)
                .map(this::runView)
                .toList();
        return page(tenantId, tagCode, limit, records);
    }

    public Map<String, Object> lineage(Long tenantId, String tagCode) {
        ComputedTag tag = find(tenantId, tagCode);
        List<Map<String, Object>> records = tag.dependencies.stream()
                .map(dependency -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("tenantId", tenantId);
                    row.put("tagCode", tag.tagCode);
                    row.put("dependency", dependency);
                    row.put("impact", "READ");
                    return row;
                })
                .toList();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("dependencies", List.copyOf(tag.dependencies));
        view.put("records", records);
        view.put("total", (long) records.size());
        return view;
    }

    public Map<String, Object> impactCheck(Long tenantId, String tagCode, Map<String, Object> payload, String actor) {
        ComputedTag tag = find(tenantId, tagCode);
        String oldValueType = requiredText(payload, "oldValueType");
        String newValueType = requiredText(payload, "newValueType");
        boolean compatible = oldValueType.equalsIgnoreCase(newValueType);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("oldValueType", oldValueType);
        view.put("newValueType", newValueType);
        view.put("compatible", compatible);
        view.put("checkedBy", actor);
        view.put("checkedAt", now());
        view.put("impactedDependencies", compatible ? List.of() : List.copyOf(tag.dependencies));
        return view;
    }

    private Map<String, Object> transition(Long tenantId, String tagCode, String status, String actor) {
        ComputedTag tag = find(tenantId, tagCode);
        ComputedTag updated = tag.withStatus(status, actor, now());
        tags.put(key(tenantId, tagCode), updated);
        return tagView(updated);
    }

    private ComputedTag find(Long tenantId, String tagCode) {
        ComputedTag tag = tags.get(key(tenantId, tagCode));
        if (tag == null) {
            throw new IllegalArgumentException("Computed tag not found: " + tagCode);
        }
        return tag;
    }

    private Map<String, Object> tagView(ComputedTag tag) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", tag.id);
        view.put("tenantId", tag.tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("displayName", tag.displayName);
        view.put("valueType", tag.valueType);
        view.put("computeType", tag.computeType);
        view.put("expressionJson", tag.expressionJson);
        view.put("refreshMode", tag.refreshMode);
        view.put("dependencies", List.copyOf(tag.dependencies));
        view.put("status", tag.status);
        view.put("updatedBy", tag.updatedBy);
        view.put("updatedAt", tag.updatedAt);
        return view;
    }

    private Map<String, Object> baseTagOperation(ComputedTag tag, String operation) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tag.tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("operation", operation);
        return view;
    }

    private Map<String, Object> runView(TagRun run) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", run.runId());
        view.put("tenantId", run.tag.tenantId);
        view.put("tagCode", run.tag.tagCode);
        view.put("status", run.status);
        view.put("triggeredBy", run.actor);
        view.put("runBy", run.actor);
        view.put("startedAt", run.startedAt);
        view.put("finishedAt", run.startedAt);
        return view;
    }

    private static Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    private static Map<String, Object> page(Long tenantId, String tagCode, Integer limit,
                                            List<Map<String, Object>> records) {
        Map<String, Object> page = page(records);
        page.put("tenantId", tenantId);
        page.put("tagCode", tagCode);
        page.put("limit", limit);
        return page;
    }

    private static String key(Long tenantId, String tagCode) {
        return tenantId + ":" + tagCode;
    }

    private String now() {
        return OffsetDateTime.now(clock).toString();
    }

    private static String sampleValue(ComputedTag tag, int index) {
        return tag.tagCode + "-sample-" + index;
    }

    private static String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    private static List<String> dependencies(Map<String, Object> payload) {
        Object value = payload.get("dependencies");
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of();
    }

    private record ComputedTag(long id,
                               Long tenantId,
                               String tagCode,
                               String displayName,
                               String valueType,
                               String computeType,
                               String expressionJson,
                               String refreshMode,
                               List<String> dependencies,
                               String status,
                               String updatedBy,
                               String updatedAt) {

        ComputedTag withStatus(String nextStatus, String actor, String time) {
            return new ComputedTag(id, tenantId, tagCode, displayName, valueType, computeType, expressionJson,
                    refreshMode, dependencies, nextStatus, actor, time);
        }
    }

    private record TagRun(long sequence,
                          ComputedTag tag,
                          String actor,
                          String status,
                          String startedAt) {

        String runId() {
            return tag.tagCode + "-run-" + sequence;
        }
    }
}
