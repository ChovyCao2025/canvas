package org.chovy.canvas.execution.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PluginRegistryCatalog {

    private static final String DEFAULT_MIN_CANVAS_VERSION = "1.0.0";

    private final List<Plugin> plugins = new ArrayList<>();

    public PluginRegistryCatalog() {
        plugins.add(new Plugin(
                "canvas-plugin-message",
                "action",
                "Message",
                true,
                Map.of("minCanvasVersion", "1.0.0"),
                Map.of("required", List.of("templateId"))));
        plugins.add(new Plugin(
                "canvas-plugin-approval",
                "action",
                "Approval",
                true,
                Map.of("minCanvasVersion", "1.1.0"),
                Map.of("required", List.of("approvalCode"))));
        plugins.add(new Plugin(
                "canvas-plugin-risk",
                "condition",
                "Risk",
                true,
                Map.of("minCanvasVersion", "1.0.0"),
                Map.of("required", List.of("scene"))));
    }

    public synchronized Map<String, List<Plugin>> groupedCatalog() {
        return plugins.stream()
                .sorted(Comparator.comparing(Plugin::pluginKey))
                .collect(Collectors.groupingBy(
                        Plugin::extensionPoint,
                        TreeMap::new,
                        Collectors.toList()));
    }

    public synchronized void setEnabled(String pluginKey, boolean enabled, String canvasVersion) {
        String normalizedKey = requirePluginKey(pluginKey);
        for (int index = 0; index < plugins.size(); index++) {
            Plugin plugin = plugins.get(index);
            if (plugin.pluginKey().equals(normalizedKey)) {
                String minVersion = String.valueOf(plugin.compatibility()
                        .getOrDefault("minCanvasVersion", DEFAULT_MIN_CANVAS_VERSION));
                if (compareVersion(canvasVersion, minVersion) < 0) {
                    throw new IllegalStateException("plugin " + normalizedKey
                            + " requires canvas version " + minVersion);
                }
                plugins.set(index, plugin.withEnabled(enabled));
                return;
            }
        }
        throw new IllegalArgumentException("plugin " + normalizedKey + " does not exist");
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
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            int leftValue = index < leftParts.length ? parseVersionPart(leftParts[index]) : 0;
            int rightValue = index < rightParts.length ? parseVersionPart(rightParts[index]) : 0;
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
        public Plugin {
            compatibility = Map.copyOf(compatibility == null ? Map.of() : compatibility);
            configSchema = Map.copyOf(configSchema == null ? Map.of() : configSchema);
        }

        private Plugin withEnabled(boolean enabled) {
            return new Plugin(pluginKey, extensionPoint, displayName, enabled, compatibility, configSchema);
        }
    }
}
