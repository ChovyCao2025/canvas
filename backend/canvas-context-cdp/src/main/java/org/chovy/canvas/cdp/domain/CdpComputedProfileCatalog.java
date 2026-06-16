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
 * 维护 CdpComputedProfile 的内存目录和查询视图。
 */
public class CdpComputedProfileCatalog {

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong profileIds = new AtomicLong();
    private final Map<String, ComputedProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, List<ProfileRun>> runs = new ConcurrentHashMap<>();
    private final Map<String, List<ProfileChange>> changes = new ConcurrentHashMap<>();

    /**
     * 创建当前组件实例。
     */
    public CdpComputedProfileCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 查询list列表。
     */
    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = profiles.values().stream()
                .filter(profile -> Objects.equals(profile.tenantId, tenantId))
                .sorted(Comparator.comparing(profile -> profile.id))
                .map(this::profileView)
                .toList();
        return page(records);
    }

    /**
     * 创建create。
     */
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        String profileCode = requiredText(payload, "attributeCode");
        ComputedProfile profile = new ComputedProfile(
                profileIds.incrementAndGet(),
                tenantId,
                profileCode,
                text(payload, "attributeName", profileCode),
                text(payload, "valueType", "STRING"),
                text(payload, "computeType", "EXPRESSION"),
                text(payload, "expression", text(payload, "expressionJson", "{}")),
                text(payload, "refreshMode", "MANUAL"),
                dependencies(payload),
                "DRAFT",
                actor,
                now());
        profiles.put(key(tenantId, profile.id), profile);
        return profileView(profile);
    }

    /**
     * 执行 preview 对应的 CDP 业务操作。
     */
    public Map<String, Object> preview(Long tenantId, Long id) {
        ComputedProfile profile = find(tenantId, id);
        Map<String, Object> view = baseProfileOperation(profile, "preview");
        view.put("sampleProfileCount", 3L);
        view.put("matchedProfileCount", 42L);
        view.put("sampleValues", List.of(sampleValue(profile, 1), sampleValue(profile, 2), sampleValue(profile, 3)));
        return view;
    }

    /**
     * 执行 activate 对应的 CDP 业务操作。
     */
    public Map<String, Object> activate(Long tenantId, Long id, String actor) {
        return transition(tenantId, id, "ACTIVE", actor);
    }

    /**
     * 执行 pause 对应的 CDP 业务操作。
     */
    public Map<String, Object> pause(Long tenantId, Long id, String actor) {
        return transition(tenantId, id, "PAUSED", actor);
    }

    /**
     * 执行 runNow 对应的 CDP 业务操作。
     */
    public Map<String, Object> runNow(Long tenantId, Long id, String actor) {
        ComputedProfile profile = find(tenantId, id);
        List<ProfileRun> profileRuns = runs.computeIfAbsent(key(tenantId, id), ignored -> new ArrayList<>());
        long sequence = profileRuns.size() + 1L;
        ProfileRun run = new ProfileRun(sequence, profile, actor, "SUCCESS", now());
        profileRuns.add(run);

        List<ProfileChange> profileChanges = changes.computeIfAbsent(key(tenantId, id), ignored -> new ArrayList<>());
        profileChanges.add(new ProfileChange(sequence, profile, "user-1", sampleValue(profile, 0),
                sampleValue(profile, 1), actor, run.startedAt));

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenantId);
        view.put("id", id);
        view.put("profileCode", profile.profileCode);
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
    public Map<String, Object> listRuns(Long tenantId, Long id, Integer limit) {
        ComputedProfile profile = find(tenantId, id);
        List<Map<String, Object>> records = runs.getOrDefault(key(tenantId, id), List.of()).stream()
                .limit(limit)
                .map(this::runView)
                .toList();
        return page(tenantId, profile.id, profile.profileCode, limit, records);
    }

    /**
     * 查询Changes列表。
     */
    public Map<String, Object> listChanges(Long tenantId, Long id, String userId, Integer limit) {
        ComputedProfile profile = find(tenantId, id);
        List<Map<String, Object>> records = changes.getOrDefault(key(tenantId, id), List.of()).stream()
                .filter(change -> userId == null || userId.equals(change.userId))
                .limit(limit)
                .map(this::changeView)
                .toList();
        Map<String, Object> page = page(tenantId, profile.id, profile.profileCode, limit, records);
        page.put("userId", userId);
        return page;
    }

    /**
     * 执行 transition 对应的 CDP 业务操作。
     */
    private Map<String, Object> transition(Long tenantId, Long id, String status, String actor) {
        ComputedProfile profile = find(tenantId, id);
        ComputedProfile updated = profile.withStatus(status, actor, now());
        profiles.put(key(tenantId, id), updated);
        return profileView(updated);
    }

    /**
     * 查找find。
     */
    private ComputedProfile find(Long tenantId, Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        ComputedProfile profile = profiles.get(key(tenantId, id));
        if (profile == null) {
            throw new IllegalArgumentException("Computed profile attribute not found: " + id);
        }
        return profile;
    }

    /**
     * 执行 profileView 对应的 CDP 业务操作。
     */
    private Map<String, Object> profileView(ComputedProfile profile) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", profile.id);
        view.put("tenantId", profile.tenantId);
        view.put("profileCode", profile.profileCode);
        view.put("attributeCode", profile.profileCode);
        view.put("displayName", profile.displayName);
        view.put("attributeName", profile.displayName);
        view.put("valueType", profile.valueType);
        view.put("computeType", profile.computeType);
        view.put("expressionJson", profile.expressionJson);
        view.put("expression", profile.expressionJson);
        view.put("refreshMode", profile.refreshMode);
        view.put("dependencies", List.copyOf(profile.dependencies));
        view.put("status", profile.status);
        view.put("updatedBy", profile.updatedBy);
        view.put("updatedAt", profile.updatedAt);
        return view;
    }

    /**
     * 执行 baseProfileOperation 对应的 CDP 业务操作。
     */
    private Map<String, Object> baseProfileOperation(ComputedProfile profile, String operation) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", profile.tenantId);
        view.put("id", profile.id);
        view.put("profileCode", profile.profileCode);
        view.put("operation", operation);
        return view;
    }

    /**
     * 执行 runView 对应的 CDP 业务操作。
     */
    private Map<String, Object> runView(ProfileRun run) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", run.runId());
        view.put("tenantId", run.profile.tenantId);
        view.put("id", run.profile.id);
        view.put("profileCode", run.profile.profileCode);
        view.put("status", run.status);
        view.put("triggeredBy", run.actor);
        view.put("runBy", run.actor);
        view.put("startedAt", run.startedAt);
        view.put("finishedAt", run.startedAt);
        return view;
    }

    /**
     * 执行 changeView 对应的 CDP 业务操作。
     */
    private Map<String, Object> changeView(ProfileChange change) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("changeId", change.changeId());
        view.put("tenantId", change.profile.tenantId);
        view.put("id", change.profile.id);
        view.put("profileCode", change.profile.profileCode);
        view.put("userId", change.userId);
        view.put("oldValue", change.oldValue);
        view.put("newValue", change.newValue);
        view.put("changedBy", change.actor);
        view.put("changedAt", change.changedAt);
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
    private static Map<String, Object> page(Long tenantId, Long id, String profileCode, Integer limit,
            List<Map<String, Object>> records) {
        Map<String, Object> page = page(records);
        page.put("tenantId", tenantId);
        page.put("id", id);
        page.put("profileCode", profileCode);
        page.put("limit", limit);
        return page;
    }

    /**
     * 执行 key 对应的 CDP 业务操作。
     */
    private static String key(Long tenantId, Long id) {
        return tenantId + ":" + id;
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
    private static String sampleValue(ComputedProfile profile, int index) {
        return profile.profileCode + "-sample-" + index;
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
     * 表示 ComputedProfile 的业务数据或处理组件。
     */
    private static final class ComputedProfile {

        /**
         * 唯一标识。
         */
        private final long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * profile Code。
         */
        private final String profileCode;

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
         * 使用记录字段创建 ComputedProfile。
         */
        private ComputedProfile(
                long id,
                Long tenantId,
                String profileCode,
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
            this.profileCode = profileCode;
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
ComputedProfile withStatus(String nextStatus, String actor, String time) {
            return new ComputedProfile(id, tenantId, profileCode, displayName, valueType, computeType,
                    expressionJson, refreshMode, dependencies, nextStatus, actor, time);
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
         * 返回profile Code。
         */
        public String profileCode() {
            return profileCode;
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
         * 按所有字段比较 ComputedProfile。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComputedProfile that = (ComputedProfile) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(profileCode, that.profileCode)
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
         * 根据所有字段计算 ComputedProfile 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, profileCode, displayName, valueType, computeType, expressionJson, refreshMode, dependencies, status, updatedBy, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "ComputedProfile[" + "id=" + id + ", tenantId=" + tenantId + ", profileCode=" + profileCode + ", displayName=" + displayName + ", valueType=" + valueType + ", computeType=" + computeType + ", expressionJson=" + expressionJson + ", refreshMode=" + refreshMode + ", dependencies=" + dependencies + ", status=" + status + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 表示 ProfileRun 的业务数据或处理组件。
     */
    private static final class ProfileRun {

        /**
         * sequence。
         */
        private final long sequence;

        /**
         * profile。
         */
        private final ComputedProfile profile;

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
         * 使用记录字段创建 ProfileRun。
         */
        private ProfileRun(
                long sequence,
                ComputedProfile profile,
                String actor,
                String status,
                String startedAt) {
            this.sequence = sequence;
            this.profile = profile;
            this.actor = actor;
            this.status = status;
            this.startedAt = startedAt;
        }

/**
 * 执行 runId 对应的 CDP 业务操作。
 */
String runId() {
            return "computed-profile-" + profile.id + "-run-" + sequence;
        }

        /**
         * 返回sequence。
         */
        public long sequence() {
            return sequence;
        }

        /**
         * 返回profile。
         */
        public ComputedProfile profile() {
            return profile;
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
         * 按所有字段比较 ProfileRun。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProfileRun that = (ProfileRun) o;
            return java.util.Objects.equals(sequence, that.sequence)
                    && java.util.Objects.equals(profile, that.profile)
                    && java.util.Objects.equals(actor, that.actor)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(startedAt, that.startedAt);
        }

        /**
         * 根据所有字段计算 ProfileRun 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(sequence, profile, actor, status, startedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "ProfileRun[" + "sequence=" + sequence + ", profile=" + profile + ", actor=" + actor + ", status=" + status + ", startedAt=" + startedAt + "]";
        }
    }

    /**
     * 表示 ProfileChange 的业务数据或处理组件。
     */
    private static final class ProfileChange {

        /**
         * sequence。
         */
        private final long sequence;

        /**
         * profile。
         */
        private final ComputedProfile profile;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * old Value。
         */
        private final String oldValue;

        /**
         * new Value。
         */
        private final String newValue;

        /**
         * 操作人。
         */
        private final String actor;

        /**
         * changed At。
         */
        private final String changedAt;

        /**
         * 使用记录字段创建 ProfileChange。
         */
        private ProfileChange(
                long sequence,
                ComputedProfile profile,
                String userId,
                String oldValue,
                String newValue,
                String actor,
                String changedAt) {
            this.sequence = sequence;
            this.profile = profile;
            this.userId = userId;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.actor = actor;
            this.changedAt = changedAt;
        }

/**
 * 执行 changeId 对应的 CDP 业务操作。
 */
String changeId() {
            return "computed-profile-" + profile.id + "-change-" + sequence;
        }

        /**
         * 返回sequence。
         */
        public long sequence() {
            return sequence;
        }

        /**
         * 返回profile。
         */
        public ComputedProfile profile() {
            return profile;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回old Value。
         */
        public String oldValue() {
            return oldValue;
        }

        /**
         * 返回new Value。
         */
        public String newValue() {
            return newValue;
        }

        /**
         * 返回操作人。
         */
        public String actor() {
            return actor;
        }

        /**
         * 返回changed At。
         */
        public String changedAt() {
            return changedAt;
        }

        /**
         * 按所有字段比较 ProfileChange。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProfileChange that = (ProfileChange) o;
            return java.util.Objects.equals(sequence, that.sequence)
                    && java.util.Objects.equals(profile, that.profile)
                    && java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(oldValue, that.oldValue)
                    && java.util.Objects.equals(newValue, that.newValue)
                    && java.util.Objects.equals(actor, that.actor)
                    && java.util.Objects.equals(changedAt, that.changedAt);
        }

        /**
         * 根据所有字段计算 ProfileChange 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(sequence, profile, userId, oldValue, newValue, actor, changedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "ProfileChange[" + "sequence=" + sequence + ", profile=" + profile + ", userId=" + userId + ", oldValue=" + oldValue + ", newValue=" + newValue + ", actor=" + actor + ", changedAt=" + changedAt + "]";
        }
    }
}
