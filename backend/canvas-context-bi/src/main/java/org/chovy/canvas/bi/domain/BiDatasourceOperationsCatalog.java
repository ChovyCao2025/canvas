package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiDatasourceApiPreviewCommand;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceConnectionTestResult;
import org.chovy.canvas.bi.api.BiDatasourceConnectorView;
import org.chovy.canvas.bi.api.BiDatasourceCredentialRotationView;
import org.chovy.canvas.bi.api.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingCommand;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaPreviewView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaSnapshotView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * BiDatasourceOperationsCatalog 目录服务。
 */
public class BiDatasourceOperationsCatalog {
    /**
     * sourceIds 对应的数据集合。
     */
    private final AtomicLong sourceIds = new AtomicLong();

    /**
     * snapshotIds 对应的数据集合。
     */
    private final AtomicLong snapshotIds = new AtomicLong();

    /**
     * sources 对应的数据集合。
     */
    private final Map<Long, List<BiDatasourceOnboardingView>> sources = new ConcurrentHashMap<>();

    /**
     * snapshots 对应的数据集合。
     */
    private final Map<Long, List<BiDatasourceSchemaSnapshotView>> snapshots = new ConcurrentHashMap<>();

    /**
     * 执行 connectors 相关处理。
     */
    public List<BiDatasourceConnectorView> connectors() {
        return List.of(
                new BiDatasourceConnectorView("API_JSON", "API JSON", List.of("baseUrl", "token"), true, true),
                new BiDatasourceConnectorView("FILE_CSV", "CSV File", List.of(), true, false),
                new BiDatasourceConnectorView("MYSQL", "MySQL", List.of("url", "username", "password"), true, false),
                new BiDatasourceConnectorView("POSTGRESQL", "PostgreSQL",
                        List.of("url", "username", "password"), true, false));
    }
    /**
     * 查询列表数据。
     */
    public List<BiDatasourceOnboardingView> list(Long tenantId) {
        return sources.getOrDefault(safeTenantId(tenantId), List.of()).stream()
                .sorted(Comparator.comparing(BiDatasourceOnboardingView::id))
                .toList();
    }
    /**
     * 执行 create 相关处理。
     */
    public BiDatasourceOnboardingView create(Long tenantId, BiDatasourceOnboardingCommand command, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDatasourceOnboardingView view = toView(
                sourceIds.incrementAndGet(),
                scopedTenantId,
                command,
                defaultActor(actor),
                defaultActor(actor));
        sources.computeIfAbsent(scopedTenantId, ignored -> new ArrayList<>()).add(view);
        return view;
    }
    /**
     * 执行 update 相关处理。
     */
    public BiDatasourceOnboardingView update(
            Long tenantId,
            Long id,
            BiDatasourceOnboardingCommand command,
            String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDatasourceOnboardingView existing = require(scopedTenantId, id);
        BiDatasourceOnboardingCommand merged = new BiDatasourceOnboardingCommand(
                fallback(command == null ? null : command.connectorType(), existing.connectorType()),
                fallback(command == null ? null : command.name(), existing.name()),
                command == null ? null : command.url(),
                command == null ? null : command.username(),
                command == null ? null : command.password(),
                existing.sourceKey(),
                fallback(command == null ? null : command.description(), existing.description()),
                command == null || command.enabled() == null ? existing.enabled() : command.enabled(),
                fallback(command == null ? null : command.status(), existing.status()),
                command == null ? existing.connectorConfig() : command.connectorConfig());
        BiDatasourceOnboardingView updated = toView(id, scopedTenantId, merged, existing.createdBy(), defaultActor(actor));
        List<BiDatasourceOnboardingView> values = sources.computeIfAbsent(scopedTenantId, ignored -> new ArrayList<>());
        values.removeIf(value -> value.id().equals(id));
        values.add(updated);
        return updated;
    }
    /**
     * 执行 upload File 相关处理。
     */
    public BiDatasourceOnboardingView uploadFile(
            Long tenantId,
            String actor,
            String filename,
            String name,
            String description,
            String sheetName,
            String delimiter,
            boolean headerRow,
            String encoding) {
        return create(tenantId, new BiDatasourceOnboardingCommand(
                "FILE_CSV",
                fallback(name, filename),
                "file://" + normalizeFilename(filename),
                null,
                null,
                null,
                description,
                true,
                "READY",
                Map.of("sheetName", sheetName == null ? "" : sheetName,
                        "delimiter", delimiter == null ? "," : delimiter,
                        "headerRow", headerRow,
                        "encoding", encoding == null ? "UTF-8" : encoding)), actor);
    }
    /**
     * 执行 materialize File 相关处理。
     */
    public BiDatasourceFileMaterializationResult materializeFile(
            Long tenantId,
            String actor,
            String filename,
            String name,
            String description,
            String sheetName,
            String delimiter,
            boolean headerRow,
            String encoding,
            String datasetKey,
            String datasetName,
            String tenantColumn,
            int schemaLimit,
            long maxRows,
            LocalDateTime now) {
        BiDatasourceOnboardingView source = uploadFile(
                tenantId, actor, filename, name, description, sheetName, delimiter, headerRow, encoding);
        BiDatasourceSchemaSnapshotView snapshot = syncSchema(tenantId, source.id(), schemaLimit, actor, now);
        String normalizedDatasetKey = normalizeKey(fallback(datasetKey, source.sourceKey()), "datasetKey");
        return new BiDatasourceFileMaterializationResult(
                source,
                snapshot,
                Map.of("datasetKey", normalizedDatasetKey,
                        "datasetName", fallback(datasetName, source.name()),
                        "tenantColumn", fallback(tenantColumn, "tenant_id"),
                        "maxRows", maxRows),
                Map.of("status", "SUCCESS", "importedRows", maxRows));
    }
    /**
     * 执行 test Connection 相关处理。
     */
    public BiDatasourceConnectionTestResult testConnection(Long tenantId, Long id) {
        BiDatasourceOnboardingView source = require(safeTenantId(tenantId), id);
        return new BiDatasourceConnectionTestResult(id, source.sourceKey(), true, "connection ok", 12L);
    }
    /**
     * 执行 rotate Credential 相关处理。
     */
    public BiDatasourceCredentialRotationView rotateCredential(Long tenantId, Long id, String actor) {
        BiDatasourceOnboardingView source = require(safeTenantId(tenantId), id);
        return new BiDatasourceCredentialRotationView(id, source.sourceKey(), defaultActor(actor));
    }
    /**
     * 执行 preview Schema 相关处理。
     */
    public BiDatasourceSchemaPreviewView previewSchema(Long tenantId, Long id, int limit) {
        BiDatasourceOnboardingView source = require(safeTenantId(tenantId), id);
        return new BiDatasourceSchemaPreviewView(id, source.sourceKey(), sampleTables(source.sourceKey(), limit));
    }
    /**
     * 执行 preview Api 相关处理。
     */
    public BiDatasourceApiPreviewView previewApi(Long tenantId, Long id, BiDatasourceApiPreviewCommand command) {
        BiDatasourceOnboardingView source = require(safeTenantId(tenantId), id);
        return new BiDatasourceApiPreviewView(
                id,
                source.sourceKey(),
                List.of(Map.of("name", "sourceKey", "type", "STRING")),
                List.of(Map.of("sourceKey", source.sourceKey(), "path", command == null ? "/" : command.path())));
    }
    /**
     * 执行 sync Schema 相关处理。
     */
    public BiDatasourceSchemaSnapshotView syncSchema(
            Long tenantId,
            Long id,
            int limit,
            String actor,
            LocalDateTime now) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiDatasourceOnboardingView source = require(scopedTenantId, id);
        BiDatasourceSchemaSnapshotView snapshot = new BiDatasourceSchemaSnapshotView(
                snapshotIds.incrementAndGet(),
                id,
                source.sourceKey(),
                "SUCCESS",
                defaultActor(actor),
                1,
                sampleTables(source.sourceKey(), limit),
                now);
        snapshots.computeIfAbsent(scopedTenantId, ignored -> new ArrayList<>()).add(snapshot);
        return snapshot;
    }
    /**
     * 执行 latest Snapshot 相关处理。
     */
    public BiDatasourceSchemaSnapshotView latestSnapshot(Long tenantId, Long id) {
        return listSnapshots(tenantId, id, 1).stream()
                .findFirst()
                .orElseGet(() -> syncSchema(tenantId, id, 100, "system", LocalDateTime.now()));
    }
    /**
     * 查询列表数据。
     */
    public List<BiDatasourceSchemaSnapshotView> listSnapshots(Long tenantId, Long id, int limit) {
        List<BiDatasourceSchemaSnapshotView> values = snapshots.getOrDefault(safeTenantId(tenantId), List.of()).stream()
                .filter(snapshot -> snapshot.dataSourceConfigId().equals(id))
                .sorted(Comparator.comparing(BiDatasourceSchemaSnapshotView::id).reversed())
                .toList();
        return limit < 0 || limit >= values.size() ? values : values.subList(0, limit);
    }
    /**
     * 执行 require 相关处理。
     */
    private BiDatasourceOnboardingView require(Long tenantId, Long id) {
        return sources.getOrDefault(tenantId, List.of()).stream()
                .filter(source -> source.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("BI datasource not found: " + id));
    }
    /**
     * 转换为目标数据结构。
     */
    private static BiDatasourceOnboardingView toView(
            Long id,
            Long tenantId,
            BiDatasourceOnboardingCommand command,
            String createdBy,
            String updatedBy) {
        String name = fallback(command.name(), "Datasource " + id);
        String sourceKey = command.sourceKey() == null || command.sourceKey().isBlank()
                ? normalizeKey(name, "name")
                : normalizeKey(command.sourceKey(), "sourceKey");
        return new BiDatasourceOnboardingView(
                id,
                tenantId,
                sourceKey,
                normalizeType(command.connectorType(), "API_JSON"),
                name.trim(),
                maskUrl(command.url()),
                maskUsername(command.username()),
                command.description(),
                command.enabled() == null || command.enabled(),
                normalizeType(command.status(), "READY"),
                command.connectorConfig(),
                createdBy,
                updatedBy);
    }
    /**
     * 执行 sample Tables 相关处理。
     */
    private static List<Map<String, Object>> sampleTables(String sourceKey, int limit) {
        return List.of(Map.of(
                "tableName", sourceKey.replace('-', '_') + "_sample",
                "rowLimit", Math.max(1, limit),
                "columns", List.of(Map.of("name", "id", "type", "BIGINT"),
                        Map.of("name", "amount", "type", "DECIMAL"))));
    }
    /**
     * 执行 safe Tenant Id 相关处理。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
    /**
     * 生成默认值。
     */
    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
    /**
     * 执行 fallback 相关处理。
     */
    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    /**
     * 规范化输入值。
     */
    private static String normalizeKey(String value, String field) {
        String normalized = fallback(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }
    /**
     * 规范化输入值。
     */
    private static String normalizeType(String value, String fallback) {
        return fallback(value, fallback).trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
    /**
     * 执行 mask Url 相关处理。
     */
    private static String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.replaceAll(":[0-9]+/", ":****/");
    }
    /**
     * 执行 mask Username 相关处理。
     */
    private static String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        String trimmed = username.trim();
        if (trimmed.length() <= 2) {
            return "*".repeat(trimmed.length());
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
    }
    /**
     * 规范化输入值。
     */
    private static String normalizeFilename(String filename) {
        return fallback(filename, "upload.csv").replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
