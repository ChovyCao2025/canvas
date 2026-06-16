package org.chovy.canvas.execution.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 定义 PluginRegistryCatalog 的执行上下文数据结构或业务契约。
 */
public class PluginRegistryCatalog {

    /**
     * 保存 DEFAULT_MIN_CANVAS_VERSION 对应的状态或配置。
     */
    private static final String DEFAULT_MIN_CANVAS_VERSION = "1.0.0";

    private final List<Plugin> plugins = new ArrayList<>();

    /**
     * 执行 PluginRegistryCatalog 对应的业务处理。
     */
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

    /**
     * 执行 groupedCatalog 对应的业务处理。
     * @return 处理后的结果
     */
    public synchronized Map<String, List<Plugin>> groupedCatalog() {
        return plugins.stream()
                .sorted(Comparator.comparing(Plugin::pluginKey))
                .collect(Collectors.groupingBy(
                        Plugin::extensionPoint,
                        TreeMap::new,
                        Collectors.toList()));
    }

    /**
     * 执行 setEnabled 对应的业务处理。
     * @param pluginKey pluginKey 参数
     * @param enabled enabled 参数
     * @param canvasVersion canvasVersion 参数
     */
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

    /**
     * 执行 requirePluginKey 对应的业务处理。
     * @param pluginKey pluginKey 参数
     */
    private static String requirePluginKey(String pluginKey) {
        String normalized = Objects.requireNonNull(pluginKey, "pluginKey")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid plugin key: " + pluginKey);
        }
        return normalized;
    }

    /**
     * 执行 compareVersion 对应的业务处理。
     * @param left left 参数
     * @param right right 参数
     */
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

    /**
     * 执行 splitVersion 对应的业务处理。
     * @param version version 参数
     * @return 处理后的结果
     */
    private static String[] splitVersion(String version) {
        String normalized = version == null || version.isBlank()
                ? DEFAULT_MIN_CANVAS_VERSION
                : version.trim();
        return normalized.split("\\.");
    }

    /**
     * 执行 parseVersionPart 对应的业务处理。
     * @param raw raw 参数
     */
    private static int parseVersionPart(String raw) {
        String digits = raw.replaceFirst("[^0-9].*$", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    /**
     * 定义 Plugin 的执行上下文数据结构或业务契约。
     * @param pluginKey pluginKey 对应的数据字段
     * @param extensionPoint extensionPoint 对应的数据字段
     * @param displayName displayName 对应的数据字段
     * @param enabled enabled 对应的数据字段
     * @param compatibility compatibility 对应的数据字段
     * @param configSchema configSchema 对应的数据字段
     */
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
