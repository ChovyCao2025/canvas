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

/**
 * 维护 CdpComputedTag 的内存目录和查询视图。
 */
public class CdpComputedTagCatalog {

    /**
     * 时间源。
     */
    private final Clock clock;
    private final Map<String, ComputedTag> tags = new ConcurrentHashMap<>();
    private final Map<String, List<TagRun>> runs = new ConcurrentHashMap<>();

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong tagIds = new AtomicLong();

    /**
     * 创建当前组件实例。
     */
    public CdpComputedTagCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 查询list列表。
     */
    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = tags.values().stream()
                .filter(tag -> Objects.equals(tag.tenantId, tenantId))
                .sorted(Comparator.comparing(tag -> tag.tagCode))
                .map(this::tagView)
                .toList();
        return page(records);
    }

    /**
     * 创建create。
     */
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

    /**
     * 执行 preview 对应的 CDP 业务操作。
     */
    public Map<String, Object> preview(Long tenantId, String tagCode) {
        ComputedTag tag = find(tenantId, tagCode);
        Map<String, Object> view = baseTagOperation(tag, "preview");
        view.put("matchedProfileCount", 42L);
        view.put("sampleValues", List.of(sampleValue(tag, 1), sampleValue(tag, 2), sampleValue(tag, 3)));
        return view;
    }

    /**
     * 执行 activate 对应的 CDP 业务操作。
     */
    public Map<String, Object> activate(Long tenantId, String tagCode, String actor) {
        return transition(tenantId, tagCode, "ACTIVE", actor);
    }

    /**
     * 执行 pause 对应的 CDP 业务操作。
     */
    public Map<String, Object> pause(Long tenantId, String tagCode, String actor) {
        return transition(tenantId, tagCode, "PAUSED", actor);
    }

    /**
     * 执行 runNow 对应的 CDP 业务操作。
     */
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

    /**
     * 查询Runs列表。
     */
    public Map<String, Object> listRuns(Long tenantId, String tagCode, Integer limit) {
        find(tenantId, tagCode);
        List<Map<String, Object>> records = runs.getOrDefault(key(tenantId, tagCode), List.of()).stream()
                .limit(limit)
                .map(this::runView)
                .toList();
        return page(tenantId, tagCode, limit, records);
    }

    /**
     * 执行 lineage 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 impactCheck 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 transition 对应的 CDP 业务操作。
     */
    private Map<String, Object> transition(Long tenantId, String tagCode, String status, String actor) {
        ComputedTag tag = find(tenantId, tagCode);
        ComputedTag updated = tag.withStatus(status, actor, now());
        tags.put(key(tenantId, tagCode), updated);
        return tagView(updated);
    }

    /**
     * 查找find。
     */
    private ComputedTag find(Long tenantId, String tagCode) {
        ComputedTag tag = tags.get(key(tenantId, tagCode));
        if (tag == null) {
            throw new IllegalArgumentException("Computed tag not found: " + tagCode);
        }
        return tag;
    }

    /**
     * 执行 tagView 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 baseTagOperation 对应的 CDP 业务操作。
     */
    private Map<String, Object> baseTagOperation(ComputedTag tag, String operation) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tag.tenantId);
        view.put("tagCode", tag.tagCode);
        view.put("operation", operation);
        return view;
    }

    /**
     * 执行 runView 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 page 对应的 CDP 业务操作。
     */
    private static Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    /**
     * 执行 page 对应的 CDP 业务操作。
     */
    private static Map<String, Object> page(Long tenantId, String tagCode, Integer limit,
                                            List<Map<String, Object>> records) {
        Map<String, Object> page = page(records);
        page.put("tenantId", tenantId);
        page.put("tagCode", tagCode);
        page.put("limit", limit);
        return page;
    }

    /**
     * 执行 key 对应的 CDP 业务操作。
     */
    private static String key(Long tenantId, String tagCode) {
        return tenantId + ":" + tagCode;
    }

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private String now() {
        return OffsetDateTime.now(clock).toString();
    }

    /**
     * 执行 sampleValue 对应的 CDP 业务操作。
     */
    private static String sampleValue(ComputedTag tag, int index) {
        return tag.tagCode + "-sample-" + index;
    }

    /**
     * 读取并校验必填的d Text。
     */
    private static String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    /**
     * 执行 text 对应的 CDP 业务操作。
     */
    private static String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    /**
     * 执行 dependencies 对应的 CDP 业务操作。
     */
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

    /**
     * 表示 ComputedTag 的业务数据或处理组件。
     */
    private static final class ComputedTag {

        /**
         * 唯一标识。
         */
        private final long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 标签编码。
         */
        private final String tagCode;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * 值类型。
         */
        private final String valueType;

        /**
         * compute Type。
         */
        private final String computeType;

        /**
         * expression Json。
         */
        private final String expressionJson;

        /**
         * refresh Mode。
         */
        private final String refreshMode;

        /**
         * dependencies。
         */
        private final List<String> dependencies;

        /**
         * 状态。
         */
        private final String status;

        /**
         * updated By。
         */
        private final String updatedBy;

        /**
         * 更新时间。
         */
        private final String updatedAt;

        /**
         * 使用记录字段创建 ComputedTag。
         */
        private ComputedTag(
                long id,
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
            this.id = id;
            this.tenantId = tenantId;
            this.tagCode = tagCode;
            this.displayName = displayName;
            this.valueType = valueType;
            this.computeType = computeType;
            this.expressionJson = expressionJson;
            this.refreshMode = refreshMode;
            this.dependencies = dependencies;
            this.status = status;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }

/**
 * 返回替换Status后的副本。
 */
ComputedTag withStatus(String nextStatus, String actor, String time) {
            return new ComputedTag(id, tenantId, tagCode, displayName, valueType, computeType, expressionJson,
                    refreshMode, dependencies, nextStatus, actor, time);
        }

        /**
         * 返回唯一标识。
         */
        public long id() {
            return id;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回标签编码。
         */
        public String tagCode() {
            return tagCode;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回值类型。
         */
        public String valueType() {
            return valueType;
        }

        /**
         * 返回compute Type。
         */
        public String computeType() {
            return computeType;
        }

        /**
         * 返回expression Json。
         */
        public String expressionJson() {
            return expressionJson;
        }

        /**
         * 返回refresh Mode。
         */
        public String refreshMode() {
            return refreshMode;
        }

        /**
         * 返回dependencies。
         */
        public List<String> dependencies() {
            return dependencies;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回updated By。
         */
        public String updatedBy() {
            return updatedBy;
        }

        /**
         * 返回更新时间。
         */
        public String updatedAt() {
            return updatedAt;
        }

        /**
         * 按所有字段比较 ComputedTag。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComputedTag that = (ComputedTag) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(tagCode, that.tagCode)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(valueType, that.valueType)
                    && java.util.Objects.equals(computeType, that.computeType)
                    && java.util.Objects.equals(expressionJson, that.expressionJson)
                    && java.util.Objects.equals(refreshMode, that.refreshMode)
                    && java.util.Objects.equals(dependencies, that.dependencies)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(updatedBy, that.updatedBy)
                    && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据所有字段计算 ComputedTag 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, tagCode, displayName, valueType, computeType, expressionJson, refreshMode, dependencies, status, updatedBy, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "ComputedTag[" + "id=" + id + ", tenantId=" + tenantId + ", tagCode=" + tagCode + ", displayName=" + displayName + ", valueType=" + valueType + ", computeType=" + computeType + ", expressionJson=" + expressionJson + ", refreshMode=" + refreshMode + ", dependencies=" + dependencies + ", status=" + status + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 表示 TagRun 的业务数据或处理组件。
     */
    private static final class TagRun {

        /**
         * sequence。
         */
        private final long sequence;

        /**
         * tag。
         */
        private final ComputedTag tag;

        /**
         * 操作人。
         */
        private final String actor;

        /**
         * 状态。
         */
        private final String status;

        /**
         * 开始时间。
         */
        private final String startedAt;

        /**
         * 使用记录字段创建 TagRun。
         */
        private TagRun(
                long sequence,
                ComputedTag tag,
                String actor,
                String status,
                String startedAt) {
            this.sequence = sequence;
            this.tag = tag;
            this.actor = actor;
            this.status = status;
            this.startedAt = startedAt;
        }

/**
 * 执行 runId 对应的 CDP 业务操作。
 */
String runId() {
            return tag.tagCode + "-run-" + sequence;
        }

        /**
         * 返回sequence。
         */
        public long sequence() {
            return sequence;
        }

        /**
         * 返回tag。
         */
        public ComputedTag tag() {
            return tag;
        }

        /**
         * 返回操作人。
         */
        public String actor() {
            return actor;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回开始时间。
         */
        public String startedAt() {
            return startedAt;
        }

        /**
         * 按所有字段比较 TagRun。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TagRun that = (TagRun) o;
            return java.util.Objects.equals(sequence, that.sequence)
                    && java.util.Objects.equals(tag, that.tag)
                    && java.util.Objects.equals(actor, that.actor)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(startedAt, that.startedAt);
        }

        /**
         * 根据所有字段计算 TagRun 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(sequence, tag, actor, status, startedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "TagRun[" + "sequence=" + sequence + ", tag=" + tag + ", actor=" + actor + ", status=" + status + ", startedAt=" + startedAt + "]";
        }
    }
}
