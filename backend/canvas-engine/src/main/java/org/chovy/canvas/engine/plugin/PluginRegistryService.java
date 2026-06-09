package org.chovy.canvas.engine.plugin;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
/**
 * PluginRegistryService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class PluginRegistryService {

    private static final String DEFAULT_MIN_CANVAS_VERSION = "1.0.0";

    private final PluginRepository repository;

    /**
     * 初始化 PluginRegistryService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PluginRegistryService(PluginRepository repository) {
        this.repository = repository;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 grouped catalog 汇总后的集合、分页或映射视图。
     */
    public Map<String, List<Plugin>> groupedCatalog() {
        return repository.list().stream()
                .sorted(Comparator.comparing(Plugin::pluginKey))
                .collect(Collectors.groupingBy(
                        Plugin::extensionPoint,
                        java.util.TreeMap::new,
                        Collectors.toList()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param pluginKey 业务键，用于在同一租户下定位资源。
     * @param enabled enabled 参数，用于 setEnabled 流程中的校验、计算或对象转换。
     * @param currentCanvasVersion current canvas version 参数，用于 setEnabled 流程中的校验、计算或对象转换。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param pluginKey 业务键，用于在同一租户下定位资源。
     * @return 返回 require plugin key 生成的文本或业务键。
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param left left 参数，用于 compareVersion 流程中的校验、计算或对象转换。
     * @param right right 参数，用于 compareVersion 流程中的校验、计算或对象转换。
     * @return 返回 compare version 计算得到的数量、金额或指标值。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param version version 参数，用于 splitVersion 流程中的校验、计算或对象转换。
     * @return 返回 split version 生成的文本或业务键。
     */
    private static String[] splitVersion(String version) {
        String normalized = version == null || version.isBlank()
                ? DEFAULT_MIN_CANVAS_VERSION
                : version.trim();
        return normalized.split("\\.");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 parseVersionPart 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int parseVersionPart(String raw) {
        String digits = raw.replaceFirst("[^0-9].*$", "");
        return digits.isBlank() ? 0 : Integer.parseInt(digits);
    }

    /**
     * Plugin 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Plugin(
            String pluginKey,
            String extensionPoint,
            String displayName,
            boolean enabled,
            Map<String, Object> compatibility,
            Map<String, Object> configSchema) {

        /**
         * 初始化 Plugin 实例。
         *
         * @param pluginKey 业务键，用于在同一租户下定位资源。
         * @param extensionPoint extension point 参数，用于 Plugin 流程中的校验、计算或对象转换。
         * @param displayName 名称文本，用于展示或唯一性校验。
         * @param enabled enabled 参数，用于 Plugin 流程中的校验、计算或对象转换。
         * @param compatibility compatibility 参数，用于 Plugin 流程中的校验、计算或对象转换。
         */
        public Plugin(
                String pluginKey,
                String extensionPoint,
                String displayName,
                boolean enabled,
                Map<String, Object> compatibility) {
            this(pluginKey, extensionPoint, displayName, enabled, compatibility, Map.of());
        }
    }

    /**
     * EnableCommand 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record EnableCommand(String pluginKey, boolean enabled) {}

    /**
     * PluginRepository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface PluginRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @return 返回符合条件的数据列表或视图。
         */
        List<Plugin> list();

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param pluginKey 业务键，用于在同一租户下定位资源。
         * @return 返回 get 流程生成的业务结果。
         */
        Plugin get(String pluginKey);

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param command 命令对象，描述本次业务动作及其参数。
         */
        void setEnabled(EnableCommand command);
    }
}
