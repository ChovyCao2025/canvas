package org.chovy.canvas.engine.plugin;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PluginRegistryService {

    private static final String DEFAULT_MIN_CANVAS_VERSION = "1.0.0";

    private final PluginRepository repository;

    public PluginRegistryService(PluginRepository repository) {
        this.repository = repository;
    }

    public Map<String, List<Plugin>> groupedCatalog() {
        return repository.list().stream()
                .sorted(Comparator.comparing(Plugin::pluginKey))
                .collect(Collectors.groupingBy(
                        Plugin::extensionPoint,
                        java.util.TreeMap::new,
                        Collectors.toList()));
    }

    public void setEnabled(String pluginKey, boolean enabled, String currentCanvasVersion) {
        String normalizedKey = requirePluginKey(pluginKey);
        Plugin plugin = repository.get(normalizedKey);
        if (plugin == null) {
            throw new IllegalArgumentException("plugin " + normalizedKey + " does not exist");
        }

        String minVersion = String.valueOf(plugin.compatibility()
                .getOrDefault("minCanvasVersion", DEFAULT_MIN_CANVAS_VERSION));
        if (compareVersion(currentCanvasVersion, minVersion) < 0) {
            throw new IllegalStateException("plugin " + normalizedKey
                    + " requires canvas version " + minVersion);
        }
        repository.setEnabled(new EnableCommand(normalizedKey, enabled));
    }

    private static String requirePluginKey(String pluginKey) {
        String normalized = Objects.requireNonNull(pluginKey, "pluginKey")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid plugin key: " + pluginKey);
        }
        return normalized;
    }

    static int compareVersion(String left, String right) {
        String[] leftParts = splitVersion(left);
        String[] rightParts = splitVersion(right);
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int leftValue = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static String[] splitVersion(String version) {
        String normalized = version == null || version.isBlank()
                ? DEFAULT_MIN_CANVAS_VERSION
                : version.trim();
        return normalized.split("\\.");
    }

    private static int parseVersionPart(String raw) {
        String digits = raw.replaceFirst("[^0-9].*$", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    public record Plugin(
            String pluginKey,
            String extensionPoint,
            String displayName,
            boolean enabled,
            Map<String, Object> compatibility,
            Map<String, Object> configSchema) {

        public Plugin(
                String pluginKey,
                String extensionPoint,
                String displayName,
                boolean enabled,
                Map<String, Object> compatibility) {
            this(pluginKey, extensionPoint, displayName, enabled, compatibility, Map.of());
        }
    }

    public record EnableCommand(String pluginKey, boolean enabled) {}

    public interface PluginRepository {
        List<Plugin> list();

        Plugin get(String pluginKey);

        void setEnabled(EnableCommand command);
    }
}
