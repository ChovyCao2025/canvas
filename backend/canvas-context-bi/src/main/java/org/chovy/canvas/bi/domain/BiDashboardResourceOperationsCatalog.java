package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiDashboardExportPackageView;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiResourceVersionView;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * BiDashboardResourceOperationsCatalog 目录服务。
 */
public class BiDashboardResourceOperationsCatalog {
    /**
     * versions 对应的数据集合。
     */
    private final Map<ResourceRef, List<VersionState>> versions = new LinkedHashMap<>();

    /**
     * runtimeStates 对应的数据集合。
     */
    private final Map<ResourceRef, BiDashboardRuntimeStateView> runtimeStates = new LinkedHashMap<>();

    /**
     * 执行 append Version 相关处理。
     */
    public synchronized void appendVersion(BiDashboardView dashboard, String actor, LocalDateTime now) {
        appendVersion(dashboard.tenantId(), dashboard.dashboardKey(), dashboard, actor, now);
    }
    /**
     * 执行 append Version 相关处理。
     */
    public synchronized void appendVersion(
            Long tenantId,
            String dashboardKey,
            BiDashboardView dashboard,
            String actor,
            LocalDateTime now) {
        ResourceRef ref = ref(tenantId, dashboardKey);
        List<VersionState> updated = new ArrayList<>(versions.getOrDefault(ref, List.of()));
        updated.add(new VersionState(
                updated.size() + 1,
                dashboard.status(),
                snapshot(dashboard),
                actor(actor),
                now));
        versions.put(ref, List.copyOf(updated));
    }
    /**
     * 查询列表数据。
     */
    public synchronized List<BiResourceVersionView> listVersions(Long tenantId, String dashboardKey, int limit) {
        ResourceRef ref = ref(tenantId, dashboardKey);
        int cappedLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        return versions.getOrDefault(ref, List.of()).stream()
                .sorted(Comparator.comparing(VersionState::version).reversed())
                .limit(cappedLimit)
                .map(version -> new BiResourceVersionView(
                        "DASHBOARD",
                        ref.dashboardKey(),
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
    public synchronized Map<String, Object> snapshot(Long tenantId, String dashboardKey, Integer version) {
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version is required");
        }
        ResourceRef ref = ref(tenantId, dashboardKey);
        return versions.getOrDefault(ref, List.of()).stream()
                .filter(candidate -> version.equals(candidate.version()))
                .findFirst()
                .map(VersionState::snapshot)
                .orElseThrow(() -> new IllegalArgumentException("BI resource version not found"));
    }
    /**
     * 执行 export Package 相关处理。
     */
    public BiDashboardExportPackageView exportPackage(BiDashboardView dashboard, String actor, LocalDateTime now) {
        return new BiDashboardExportPackageView(
                "DASHBOARD",
                dashboard.dashboardKey(),
                dashboard,
                Map.of(
                        "schema", "canvas.bi.dashboard.v1",
                        "version", dashboard.version()),
                now,
                actor(actor));
    }
    /**
     * 执行 export File 相关处理。
     */
    public DashboardPackageFile exportFile(BiDashboardExportPackageView packageView) {
        String dashboardKey = packageView.sourceDashboardKey();
        String body = "{\"resourceType\":\"DASHBOARD\",\"dashboardKey\":\"" + dashboardKey + "\"}";
        return new DashboardPackageFile(
                dashboardKey + "-v" + packageView.dashboard().version() + ".bi-dashboard.json",
                "application/json",
                body.getBytes(StandardCharsets.UTF_8));
    }
    /**
     * 获取 Runtime State。
     */
    public synchronized BiDashboardRuntimeStateView getRuntimeState(
            Long tenantId,
            String actor,
            String dashboardKey,
            LocalDateTime now) {
        ResourceRef ref = ref(tenantId, dashboardKey);
        return runtimeStates.getOrDefault(ref, new BiDashboardRuntimeStateView(
                ref.tenantId(),
                ref.dashboardKey(),
                Map.of(),
                actor(actor),
                now));
    }
    /**
     * 执行 save Runtime State 相关处理。
     */
    public synchronized BiDashboardRuntimeStateView saveRuntimeState(
            Long tenantId,
            String actor,
            String dashboardKey,
            BiDashboardRuntimeStateCommand command,
            LocalDateTime now) {
        ResourceRef ref = ref(tenantId, dashboardKey);
        BiDashboardRuntimeStateView saved = new BiDashboardRuntimeStateView(
                ref.tenantId(),
                ref.dashboardKey(),
                command == null ? Map.of() : command.parameters(),
                actor(actor),
                now);
        runtimeStates.put(ref, saved);
        return saved;
    }
    /**
     * 执行 snapshot 相关处理。
     */
    private static Map<String, Object> snapshot(BiDashboardView dashboard) {
        return Map.of(
                "workspaceId", dashboard.workspaceId(),
                "dashboardKey", dashboard.dashboardKey(),
                "name", dashboard.name(),
                "description", dashboard.description() == null ? "" : dashboard.description(),
                "theme", dashboard.theme(),
                "filters", dashboard.filters(),
                "chartKeys", dashboard.chartKeys());
    }
    /**
     * 执行 ref 相关处理。
     */
    private static ResourceRef ref(Long tenantId, String dashboardKey) {
        return new ResourceRef(tenant(tenantId), BiResourceKey.of(dashboardKey, "dashboardKey").value());
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
     * DashboardPackageFile 不可变数据载体。
     */
    public record DashboardPackageFile(String filename, String contentType, byte[] content) {
    }
    /**
     * ResourceRef 不可变数据载体。
     */
    private record ResourceRef(Long tenantId, String dashboardKey) {
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
