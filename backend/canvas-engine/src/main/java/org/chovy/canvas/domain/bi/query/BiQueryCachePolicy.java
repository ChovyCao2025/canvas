package org.chovy.canvas.domain.bi.query;

import java.util.List;

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

    public static BiQueryCachePolicy defaults(boolean enabled, long ttlSeconds) {
        return new BiQueryCachePolicy(enabled, ttlSeconds, MODE_CACHE, List.of());
    }

    public static BiQueryCachePolicy defaults() {
        return defaults(true, 300L);
    }

    public ResourcePolicy defaultPolicy() {
        return new ResourcePolicy(TYPE_DEFAULT, DEFAULT_RESOURCE_KEY, defaultEnabled, defaultTtlSeconds, defaultCacheMode);
    }

    public ResourcePolicy effectiveForDataset(String datasetKey) {
        return effective(TYPE_DATASET, datasetKey);
    }

    public ResourcePolicy effectiveForDashboard(String dashboardKey) {
        return effective(TYPE_DASHBOARD, dashboardKey);
    }

    private ResourcePolicy effective(String type, String key) {
        if (key != null && !key.isBlank()) {
            for (ResourcePolicy resource : resources) {
                if (type.equals(resource.resourceType()) && key.trim().equals(resource.resourceKey())) {
                    return resource.withDefaults(defaultTtlSeconds, defaultCacheMode);
                }
            }
        }
        return new ResourcePolicy(type, key, defaultEnabled, defaultTtlSeconds, defaultCacheMode);
    }

    private static String normalizeCacheMode(String cacheMode) {
        if (cacheMode == null || cacheMode.isBlank()) {
            return MODE_CACHE;
        }
        return cacheMode.trim().toUpperCase();
    }

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

        private ResourcePolicy withDefaults(long defaultTtlSeconds, String defaultCacheMode) {
            return new ResourcePolicy(resourceType, resourceKey, enabled,
                    ttlSeconds <= 0 ? defaultTtlSeconds : ttlSeconds,
                    cacheMode == null || cacheMode.isBlank() ? defaultCacheMode : cacheMode);
        }
    }
}
