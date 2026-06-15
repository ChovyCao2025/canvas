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

public class CdpComputedProfileCatalog {

    private final Clock clock;
    private final AtomicLong profileIds = new AtomicLong();
    private final Map<String, ComputedProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, List<ProfileRun>> runs = new ConcurrentHashMap<>();
    private final Map<String, List<ProfileChange>> changes = new ConcurrentHashMap<>();

    public CdpComputedProfileCatalog(Clock clock) {
        this.clock = clock;
    }

    public Map<String, Object> list(Long tenantId) {
        List<Map<String, Object>> records = profiles.values().stream()
                .filter(profile -> Objects.equals(profile.tenantId, tenantId))
                .sorted(Comparator.comparing(profile -> profile.id))
                .map(this::profileView)
                .toList();
        return page(records);
    }

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

    public Map<String, Object> preview(Long tenantId, Long id) {
        ComputedProfile profile = find(tenantId, id);
        Map<String, Object> view = baseProfileOperation(profile, "preview");
        view.put("sampleProfileCount", 3L);
        view.put("matchedProfileCount", 42L);
        view.put("sampleValues", List.of(sampleValue(profile, 1), sampleValue(profile, 2), sampleValue(profile, 3)));
        return view;
    }

    public Map<String, Object> activate(Long tenantId, Long id, String actor) {
        return transition(tenantId, id, "ACTIVE", actor);
    }

    public Map<String, Object> pause(Long tenantId, Long id, String actor) {
        return transition(tenantId, id, "PAUSED", actor);
    }

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

    public Map<String, Object> listRuns(Long tenantId, Long id, Integer limit) {
        ComputedProfile profile = find(tenantId, id);
        List<Map<String, Object>> records = runs.getOrDefault(key(tenantId, id), List.of()).stream()
                .limit(limit)
                .map(this::runView)
                .toList();
        return page(tenantId, profile.id, profile.profileCode, limit, records);
    }

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

    private Map<String, Object> transition(Long tenantId, Long id, String status, String actor) {
        ComputedProfile profile = find(tenantId, id);
        ComputedProfile updated = profile.withStatus(status, actor, now());
        profiles.put(key(tenantId, id), updated);
        return profileView(updated);
    }

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

    private Map<String, Object> baseProfileOperation(ComputedProfile profile, String operation) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", profile.tenantId);
        view.put("id", profile.id);
        view.put("profileCode", profile.profileCode);
        view.put("operation", operation);
        return view;
    }

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

    private static Map<String, Object> page(List<Map<String, Object>> records) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("total", (long) records.size());
        page.put("records", records);
        return page;
    }

    private static Map<String, Object> page(Long tenantId, Long id, String profileCode, Integer limit,
            List<Map<String, Object>> records) {
        Map<String, Object> page = page(records);
        page.put("tenantId", tenantId);
        page.put("id", id);
        page.put("profileCode", profileCode);
        page.put("limit", limit);
        return page;
    }

    private static String key(Long tenantId, Long id) {
        return tenantId + ":" + id;
    }

    private String now() {
        return OffsetDateTime.now(clock).toString();
    }

    private static String sampleValue(ComputedProfile profile, int index) {
        return profile.profileCode + "-sample-" + index;
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

    private record ComputedProfile(long id,
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

        ComputedProfile withStatus(String nextStatus, String actor, String time) {
            return new ComputedProfile(id, tenantId, profileCode, displayName, valueType, computeType,
                    expressionJson, refreshMode, dependencies, nextStatus, actor, time);
        }
    }

    private record ProfileRun(long sequence,
                              ComputedProfile profile,
                              String actor,
                              String status,
                              String startedAt) {

        String runId() {
            return "computed-profile-" + profile.id + "-run-" + sequence;
        }
    }

    private record ProfileChange(long sequence,
                                 ComputedProfile profile,
                                 String userId,
                                 String oldValue,
                                 String newValue,
                                 String actor,
                                 String changedAt) {

        String changeId() {
            return "computed-profile-" + profile.id + "-change-" + sequence;
        }
    }
}
