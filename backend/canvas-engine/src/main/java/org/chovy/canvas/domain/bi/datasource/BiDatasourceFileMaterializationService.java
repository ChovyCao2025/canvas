package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class BiDatasourceFileMaterializationService {

    private static final int DEFAULT_SCHEMA_LIMIT = 200;
    private static final long DEFAULT_MAX_ROWS = 100_000L;

    private final BiDatasourceFileUploadService uploadService;
    private final BiDatasourceRuntimeService runtimeService;
    private final BiDatasetFromDatasourceService datasetService;
    private final BiDatasetAccelerationService accelerationService;

    @Autowired
    public BiDatasourceFileMaterializationService(BiDatasourceFileUploadService uploadService,
                                                  BiDatasourceRuntimeService runtimeService,
                                                  BiDatasetFromDatasourceService datasetService,
                                                  BiDatasetAccelerationService accelerationService) {
        this.uploadService = uploadService;
        this.runtimeService = runtimeService;
        this.datasetService = datasetService;
        this.accelerationService = accelerationService;
    }

    public BiDatasourceFileMaterializationResult uploadAndMaterialize(Long tenantId,
                                                                      String username,
                                                                      String role,
                                                                      String originalFileName,
                                                                      byte[] content,
                                                                      BiDatasourceFileMaterializationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI datasource file materialization command is required");
        }
        BiDatasourceOnboardingView source = uploadService.upload(
                tenantId,
                username,
                originalFileName,
                content,
                new BiDatasourceFileUploadCommand(
                        command.name(),
                        command.description(),
                        command.sheetName(),
                        command.delimiter(),
                        command.headerRow(),
                        command.encoding()));
        Long sourceId = required(source == null ? null : source.id(), "sourceId");
        int schemaLimit = boundedSchemaLimit(command.schemaLimit());
        BiDatasourceSchemaSnapshotView snapshot = runtimeService.syncSchema(
                sourceId,
                normalizeTenant(tenantId),
                actor(username),
                schemaLimit,
                null);
        if (snapshot == null || !"SUCCESS".equalsIgnoreCase(defaultString(snapshot.syncStatus()))) {
            throw new IllegalStateException("BI uploaded file schema sync must succeed before materialization");
        }
        BiDatasourceTablePreview table = firstTable(snapshot);
        String datasetKey = defaultString(command.datasetKey());
        if (datasetKey.isBlank()) {
            datasetKey = defaultDatasetKey(sourceId, table.name());
        }
        String datasetName = defaultString(command.datasetName());
        if (datasetName.isBlank()) {
            datasetName = defaultString(source.name());
            datasetName = (datasetName.isBlank() ? "Uploaded File" : datasetName) + " " + table.name();
        }
        String tenantColumn = defaultString(command.tenantColumn());
        if (tenantColumn.isBlank()) {
            tenantColumn = "tenant_id";
        }
        BiDatasetResource dataset = datasetService.createTableDataset(
                normalizeTenant(tenantId),
                actor(username),
                role,
                new BiDatasetFromDatasourceCommand(
                        sourceId,
                        table.name(),
                        datasetKey,
                        datasetName,
                        tenantColumn,
                        selectedColumns(table)));
        Long maxRows = positive(command.maxRows(), DEFAULT_MAX_ROWS);
        var policy = accelerationService.upsertPolicy(
                normalizeTenant(tenantId),
                dataset.datasetKey(),
                new BiDatasetAccelerationPolicyCommand(
                        true,
                        BiDatasetAccelerationService.MODE_EXTRACT,
                        BiDatasetAccelerationService.REFRESH_MANUAL,
                        60L,
                        300L,
                        maxRows,
                        null),
                actor(username));
        var refreshRun = accelerationService.refreshNow(normalizeTenant(tenantId), dataset.datasetKey(), actor(username));
        return new BiDatasourceFileMaterializationResult(source, snapshot, dataset, policy, refreshRun);
    }

    private static BiDatasourceTablePreview firstTable(BiDatasourceSchemaSnapshotView snapshot) {
        if (snapshot.tables() == null || snapshot.tables().isEmpty()) {
            throw new IllegalStateException("BI uploaded file schema snapshot has no tables");
        }
        return snapshot.tables().get(0);
    }

    private static List<String> selectedColumns(BiDatasourceTablePreview table) {
        return table.columns() == null
                ? List.of()
                : table.columns().stream()
                .map(BiDatasourceColumnPreview::name)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private static String defaultDatasetKey(Long sourceId, String tableName) {
        String safeTable = safeIdentifier(tableName == null || tableName.isBlank() ? "file_upload" : tableName);
        return "file_" + sourceId + "_" + safeTable;
    }

    private static String safeIdentifier(String value) {
        String normalized = defaultString(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "file_upload" : normalized;
    }

    private static Long required(Long value, String field) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("BI uploaded file " + field + " is required");
        }
        return value;
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String actor(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private static int boundedSchemaLimit(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_SCHEMA_LIMIT;
        }
        return Math.min(value, 1_000);
    }

    private static Long positive(Long value, long fallback) {
        return value == null || value <= 0L ? fallback : value;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }
}
