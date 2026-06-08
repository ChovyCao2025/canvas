package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryCachePolicy record.
 * @param defaultEnabled 默认是否启用查询结果缓存.
 * @param defaultTtlSeconds 默认缓存 TTL，单位秒；小于等于 0 时会回退到平台默认值.
 * @param defaultCacheMode 默认查询模式，区分使用缓存或直连查询.
 * @param resources 数据集或看板级覆盖策略，按资源类型和资源 key 匹配.
 */
public record BiQueryCachePolicy(
        boolean defaultEnabled,
        long defaultTtlSeconds,
        String defaultCacheMode,
        List<ResourcePolicy> resources) {

    public static final String TYPE_DEFAULT = "DEFAULT";
    public static final String TYPE_DATASET = "DATASET";
    public static final String TYPE_DASHBOARD = "DASHBOARD";
    public static final String DEFAULT_RESOURCE_KEY = "__DEFAULT__";
    public static final String MODE_CACHE = "CACHE";
    public static final String MODE_DIRECT_QUERY = "DIRECT_QUERY";

    public BiQueryCachePolicy {
        defaultTtlSeconds = defaultTtlSeconds <= 0 ? 300L : defaultTtlSeconds;
        defaultCacheMode = normalizeCacheMode(defaultCacheMode);
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    /**
     * 构造只包含默认值的查询缓存策略。
     *
     * @param enabled 默认是否启用缓存
     * @param ttlSeconds 默认 TTL 秒数，非法值会在构造器中归一化
     * @return 默认策略实例，不包含资源级覆盖
     */
    public static BiQueryCachePolicy defaults(boolean enabled, long ttlSeconds) {
        return new BiQueryCachePolicy(enabled, ttlSeconds, MODE_CACHE, List.of());
    }

    /**
     * 构造平台出厂查询缓存策略。
     *
     * @return 启用缓存、TTL 为 300 秒且无资源覆盖的策略
     */
    public static BiQueryCachePolicy defaults() {
        return defaults(true, 300L);
    }

    /**
     * 将默认策略转换为统一的资源策略视图。
     *
     * @return 使用 {@link #TYPE_DEFAULT} 和 {@link #DEFAULT_RESOURCE_KEY} 标识的默认策略
     */
    public ResourcePolicy defaultPolicy() {
        return new ResourcePolicy(TYPE_DEFAULT, DEFAULT_RESOURCE_KEY, defaultEnabled, defaultTtlSeconds, defaultCacheMode);
    }

    /**
     * 计算指定数据集的生效缓存策略。
     *
     * @param datasetKey 数据集 key；为空时返回继承默认值的数据集策略
     * @return 数据集级覆盖策略，未配置时以默认开关、TTL 和查询模式补齐
     */
    public ResourcePolicy effectiveForDataset(String datasetKey) {
        return effective(TYPE_DATASET, datasetKey);
    }

    /**
     * 计算指定看板的生效缓存策略。
     *
     * @param dashboardKey 看板 key；为空时返回继承默认值的看板策略
     * @return 看板级覆盖策略，未配置时以默认开关、TTL 和查询模式补齐
     */
    public ResourcePolicy effectiveForDashboard(String dashboardKey) {
        return effective(TYPE_DASHBOARD, dashboardKey);
    }

    /**
     * 按资源类型和 key 查找覆盖策略，并在缺失字段上继承默认缓存口径。
     */
    private ResourcePolicy effective(String type, String key) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (key != null && !key.isBlank()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (ResourcePolicy resource : resources) {
                if (type.equals(resource.resourceType()) && key.trim().equals(resource.resourceKey())) {
                    return resource.withDefaults(defaultTtlSeconds, defaultCacheMode);
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ResourcePolicy(type, key, defaultEnabled, defaultTtlSeconds, defaultCacheMode);
    }

    /**
     * 规范化输入值。
     *
     * @param cacheMode 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeCacheMode(String cacheMode) {
        if (cacheMode == null || cacheMode.isBlank()) {
            return MODE_CACHE;
        }
        return cacheMode.trim().toUpperCase();
    }

    /**
     * ResourcePolicy record.
     * @param resourceType 资源类型，支持默认、数据集和看板.
     * @param resourceKey 资源稳定 key，默认策略使用 {@link BiQueryCachePolicy#DEFAULT_RESOURCE_KEY}.
     * @param enabled 是否允许该资源使用查询缓存.
     * @param ttlSeconds 资源级 TTL 秒数，非法值会继承默认 TTL.
     * @param cacheMode 资源级查询模式，控制缓存命中或直连执行.
     */
    public record ResourcePolicy(
        String resourceType,
        String resourceKey,
        boolean enabled,
        long ttlSeconds,
        String cacheMode) {

        public ResourcePolicy {
            resourceType = resourceType == null || resourceType.isBlank()
                    ? TYPE_DATASET
                    : resourceType.trim().toUpperCase();
            resourceKey = resourceKey == null ? "" : resourceKey.trim();
            ttlSeconds = ttlSeconds <= 0 ? 300L : ttlSeconds;
            cacheMode = normalizeCacheMode(cacheMode);
        }

        /**
         * 执行 withDefaults 流程，围绕 with defaults 完成校验、计算或结果组装。
         *
         * @param defaultTtlSeconds default ttl seconds 参数，用于 withDefaults 流程中的校验、计算或对象转换。
         * @param defaultCacheMode 依赖组件，用于完成数据访问、计算或外部能力调用。
         * @return 返回 withDefaults 流程生成的业务结果。
         */
        private ResourcePolicy withDefaults(long defaultTtlSeconds, String defaultCacheMode) {
            return new ResourcePolicy(resourceType, resourceKey, enabled,
                    ttlSeconds <= 0 ? defaultTtlSeconds : ttlSeconds,
                    cacheMode == null || cacheMode.isBlank() ? defaultCacheMode : cacheMode);
        }
    }
}
