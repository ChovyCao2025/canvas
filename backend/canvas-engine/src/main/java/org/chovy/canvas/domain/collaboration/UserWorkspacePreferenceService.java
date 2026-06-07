package org.chovy.canvas.domain.collaboration;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserWorkspacePreferenceService {

    private static final String EDITOR_KEY = "canvas-editor";
    private static final Set<String> ALLOWED_EDITOR_KEYS = Set.of(
            "theme", "sidebarCollapsed", "notificationLevel", "recentNodeTypes", "editorLayout", "listDefaults");

    private final PreferenceRepository repository;

    public UserWorkspacePreferenceService() {
        this(new InMemoryPreferenceRepository());
    }

    public UserWorkspacePreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    public Preference getEditorPreference(Long tenantId, String userId) {
        return repository.find(tenantId, userId, EDITOR_KEY)
                .orElseGet(() -> new Preference(EDITOR_KEY, defaultEditorPreferences()));
    }

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

    private static Map<String, Object> defaultEditorPreferences() {
        return Map.of(
                "theme", "system",
                "sidebarCollapsed", false,
                "notificationLevel", "mentions",
                "recentNodeTypes", List.of(),
                "editorLayout", "default",
                "listDefaults", Map.of("pageSize", 20));
    }

    public record Preference(String preferenceKey, Map<String, Object> preferenceJson) {
        public Preference {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }

    public record StoredPreference(Long tenantId,
                                   String userId,
                                   String preferenceKey,
                                   Map<String, Object> preferenceJson) {
        public StoredPreference {
            preferenceJson = preferenceJson == null ? Map.of() : Map.copyOf(preferenceJson);
        }
    }

    public interface PreferenceRepository {
        Optional<Preference> find(Long tenantId, String userId, String preferenceKey);

        void upsert(StoredPreference preference);
    }

    private static final class InMemoryPreferenceRepository implements PreferenceRepository {

        private final Map<Key, Preference> preferences = new ConcurrentHashMap<>();

        @Override
        public Optional<Preference> find(Long tenantId, String userId, String preferenceKey) {
            return Optional.ofNullable(preferences.get(new Key(tenantId, userId, preferenceKey)));
        }

        @Override
        public void upsert(StoredPreference preference) {
            preferences.put(new Key(preference.tenantId(), preference.userId(), preference.preferenceKey()),
                    new Preference(preference.preferenceKey(), preference.preferenceJson()));
        }
    }

    private record Key(Long tenantId, String userId, String preferenceKey) {
    }
}
