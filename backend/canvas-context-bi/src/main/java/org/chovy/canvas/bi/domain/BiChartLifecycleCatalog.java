package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiResourceVersionView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BiChartLifecycleCatalog {

    private final Map<ResourceRef, List<VersionState>> versions = new LinkedHashMap<>();

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

    private static ResourceRef ref(Long tenantId, String chartKey) {
        return new ResourceRef(tenant(tenantId), BiResourceKey.of(chartKey, "chartKey").value());
    }

    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private record ResourceRef(Long tenantId, String chartKey) {
    }

    private record VersionState(
            Integer version,
            String status,
            Map<String, Object> snapshot,
            String createdBy,
            LocalDateTime createdAt) {
    }
}
