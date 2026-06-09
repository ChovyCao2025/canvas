package org.chovy.canvas.domain.collaboration;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * UserWorkspacePreferenceService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class UserWorkspacePreferenceService {

    private static final String EDITOR_KEY = "canvas-editor";
    private static final Set<String> ALLOWED_EDITOR_KEYS = Set.of(
            "theme", "sidebarCollapsed", "notificationLevel", "recentNodeTypes", "editorLayout", "listDefaults");

    private final PreferenceRepository repository;

    /**
     * 初始化 UserWorkspacePreferenceService 实例。
     */
    public UserWorkspacePreferenceService() {
        this(new InMemoryPreferenceRepository());
    }

    /**
     * 初始化 UserWorkspacePreferenceService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public UserWorkspacePreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 getEditorPreference 流程生成的业务结果。
     */
    public Preference getEditorPreference(Long tenantId, String userId) {
        return repository.find(tenantId, userId, EDITOR_KEY)
                .orElseGet(() -> new Preference(EDITOR_KEY, defaultEditorPreferences()));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param MapString map string 参数，用于 upsertEditorPreference 流程中的校验、计算或对象转换。
     * @param patch patch 参数，用于 upsertEditorPreference 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public Preference upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch) {
        Map<String, Object> scopedPatch = patch == null ? Map.of() : patch;
        for (String key : scopedPatch.keySet()) {
            if (!ALLOWED_EDITOR_KEYS.contains(key)) {
                throw new IllegalArgumentException("unsupported preference key " + key);
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>(getEditorPreference(tenantId, userId).preferenceJson());
        merged.putAll(scopedPatch);
        StoredPreference stored = new StoredPreference(tenantId, userId, EDITOR_KEY, merged);
        repository.upsert(stored);
        return new Preference(EDITOR_KEY, merged);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @return 返回 defaultEditorPreferences 流程生成的业务结果。
     */
    private static Map<String, Object> defaultEditorPreferences() {
        return Map.of(
                "theme", "system",
                "sidebarCollapsed", false,
                "notificationLevel", "mentions",
                "recentNodeTypes", List.of(),
                "editorLayout", "default",
                "listDefaults", Map.of("pageSize", 20));
    }

    /**
     * Preference 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Preference(String preferenceKey, Map<String, Object> preferenceJson) {
        public Preference {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }

    /**
     * StoredPreference 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record StoredPreference(Long tenantId,
                                   String userId,
                                   String preferenceKey,
                                   Map<String, Object> preferenceJson) {
        public StoredPreference {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }

    /**
     * PreferenceRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface PreferenceRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param userId 业务对象 ID，用于定位具体记录。
         * @param preferenceKey 业务键，用于在同一租户下定位资源。
         * @return 返回符合条件的数据列表或视图。
         */
        Optional<Preference> find(Long tenantId, String userId, String preferenceKey);

        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param preference preference 参数，用于 upsert 流程中的校验、计算或对象转换。
         */
        void upsert(StoredPreference preference);
    }

    /**
     * InMemoryPreferenceRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class InMemoryPreferenceRepository implements PreferenceRepository {

        private final Map<Key, Preference> preferences = new ConcurrentHashMap<>();

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param userId 业务对象 ID，用于定位具体记录。
         * @param preferenceKey 业务键，用于在同一租户下定位资源。
         * @return 返回符合条件的数据列表或视图。
         */
        public Optional<Preference> find(Long tenantId, String userId, String preferenceKey) {
            return Optional.ofNullable(preferences.get(new Key(tenantId, userId, preferenceKey)));
        }

        @Override
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param preference preference 参数，用于 upsert 流程中的校验、计算或对象转换。
         */
        public void upsert(StoredPreference preference) {
            preferences.put(new Key(preference.tenantId(), preference.userId(), preference.preferenceKey()),
                    new Preference(preference.preferenceKey(), preference.preferenceJson()));
        }
    }

    /**
     * Key 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record Key(Long tenantId, String userId, String preferenceKey) {
    }
}
