package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiResourceVersionView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * BiChartLifecycleCatalog 目录服务。
 */
public class BiChartLifecycleCatalog {
    /**
     * versions 对应的数据集合。
     */
    private final Map<ResourceRef, List<VersionState>> versions = new LinkedHashMap<>();

    /**
     * 执行 append Version 相关处理。
     */
    public synchronized void appendVersion(BiChart chart, String actor, LocalDateTime now) {
        ResourceRef ref = ref(chart.tenantId(), chart.chartKey().value());
        List<VersionState> updated = new ArrayList<>(versions.getOrDefault(ref, List.of()));
        updated.add(new VersionState(
                updated.size() + 1,
                chart.status().name(),
                snapshot(chart),
                actor(actor),
                now));
        versions.put(ref, List.copyOf(updated));
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiResourceVersionView> listVersions(Long tenantId, String chartKey) {
        ResourceRef ref = ref(tenantId, chartKey);
        return versions.getOrDefault(ref, List.of()).stream()
                .sorted(Comparator.comparing(VersionState::version).reversed())
                .map(version -> new BiResourceVersionView(
                        "CHART",
                        ref.chartKey(),
                        version.version(),
                        version.status(),
                        version.snapshot(),
                        version.createdBy(),
                        version.createdAt()))
                .toList();
    }
    /**
     * 执行 snapshot 相关处理。
     */
    public synchronized Map<String, Object> snapshot(Long tenantId, String chartKey, Integer version) {
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version is required");
        }
        ResourceRef ref = ref(tenantId, chartKey);
        return versions.getOrDefault(ref, List.of()).stream()
                .filter(candidate -> version.equals(candidate.version()))
                .findFirst()
                .map(VersionState::snapshot)
                .orElseThrow(() -> new IllegalArgumentException("BI resource version not found"));
    }
    /**
     * 执行 snapshot 相关处理。
     */
    private static Map<String, Object> snapshot(BiChart chart) {
        return Map.of(
                "workspaceId", chart.workspaceId(),
                "name", chart.name(),
                "chartType", chart.chartType(),
                "datasetId", chart.datasetId(),
                "datasetKey", chart.datasetKey().value(),
                "query", chart.query(),
                "style", chart.style(),
                "interaction", chart.interaction());
    }
    /**
     * 执行 ref 相关处理。
     */
    private static ResourceRef ref(Long tenantId, String chartKey) {
        return new ResourceRef(tenant(tenantId), BiResourceKey.of(chartKey, "chartKey").value());
    }
    /**
     * 执行 tenant 相关处理。
     */
    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
    /**
     * 执行 actor 相关处理。
     */
    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
    /**
     * ResourceRef 不可变数据载体。
     */
    private record ResourceRef(Long tenantId, String chartKey) {
    }
    /**
     * VersionState 不可变数据载体。
     */
    private record VersionState(
            /**
             * 版本号。
             */
            Integer version,
            /**
             * 状态值。
             */
            String status,
            /**
             * snapshot 字段值。
             */
            Map<String, Object> snapshot,
            /**
             * 创建人。
             */
            String createdBy,
            LocalDateTime createdAt) {
    }
}
