package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseTableContractDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseTableInspectionDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseTableContractMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseTableInspectionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CdpWarehouseTableGovernanceService {

    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String ENGINE_DORIS = "DORIS";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String LIVE_DDL_SOURCE = "LIVE:SHOW_CREATE_TABLE";
    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";

    private final CdpWarehouseTableContractMapper contractMapper;
    private final CdpWarehouseTableInspectionMapper inspectionMapper;
    private final DdlAssetReader ddlAssetReader;
    private final LiveDdlReader liveDdlReader;

    public CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                              CdpWarehouseTableInspectionMapper inspectionMapper) {
        this(contractMapper, inspectionMapper, new ClasspathDdlAssetReader(), null);
    }

    @Autowired
    public CdpWarehouseTableGovernanceService(
            CdpWarehouseTableContractMapper contractMapper,
            CdpWarehouseTableInspectionMapper inspectionMapper,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this(contractMapper, inspectionMapper, new ClasspathDdlAssetReader(),
                new DorisLiveDdlReader(dorisJdbcTemplate));
    }

    CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                       DdlAssetReader ddlAssetReader) {
        this(contractMapper, inspectionMapper, ddlAssetReader, null);
    }

    CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                       DdlAssetReader ddlAssetReader,
                                       LiveDdlReader liveDdlReader) {
        this.contractMapper = contractMapper;
        this.inspectionMapper = inspectionMapper;
        this.ddlAssetReader = ddlAssetReader;
        this.liveDdlReader = liveDdlReader;
    }

    public TableContractView upsertContract(Long tenantId, TableContractCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("table contract command is required");
        }
        CdpWarehouseTableContractDO row = new CdpWarehouseTableContractDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setTableKey(required(command.tableKey(), "tableKey"));
        row.setDatasetKey(required(command.datasetKey(), "datasetKey"));
        row.setLayer(upperRequired(command.layer(), "layer"));
        row.setPhysicalName(required(command.physicalName(), "physicalName"));
        row.setEngineType(upperDefault(command.engineType(), ENGINE_DORIS));
        row.setDdlAssetPath(blankToNull(command.ddlAssetPath()));
        row.setPartitionColumn(required(command.partitionColumn(), "partitionColumn"));
        row.setPartitionGranularity(upperDefault(command.partitionGranularity(), "DAY"));
        row.setRetentionDays(requirePositive(command.retentionDays(), "retentionDays"));
        row.setReplicaCount(requirePositive(command.replicaCount(), "replicaCount"));
        row.setBucketCount(requirePositive(command.bucketCount(), "bucketCount"));
        row.setDistributionColumns(required(command.distributionColumns(), "distributionColumns"));
        row.setStoragePolicy(blankToNull(command.storagePolicy()));
        row.setLifecycleStatus(upperDefault(command.lifecycleStatus(), STATUS_ACTIVE));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setDescription(blankToNull(command.description()));
        row.setExpectedPropertiesJson(blankToNull(command.expectedPropertiesJson()));
        contractMapper.upsert(row);
        return toView(row);
    }

    public List<TableContractView> listContracts(Long tenantId, String layer, String lifecycleStatus) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseTableContractDO> query = new LambdaQueryWrapper<CdpWarehouseTableContractDO>()
                .in(CdpWarehouseTableContractDO::getTenantId, tenantScope(scopedTenantId))
                .orderByAsc(CdpWarehouseTableContractDO::getTenantId)
                .orderByAsc(CdpWarehouseTableContractDO::getLayer)
                .orderByAsc(CdpWarehouseTableContractDO::getTableKey);
        if (hasText(layer)) {
            query.eq(CdpWarehouseTableContractDO::getLayer, layer.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(lifecycleStatus)) {
            query.eq(CdpWarehouseTableContractDO::getLifecycleStatus,
                    lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, TableContractView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseTableContractDO row : safeList(contractMapper.selectList(query))) {
            byKey.put(row.getTableKey(), toView(row));
        }
        return new ArrayList<>(byKey.values());
    }

    public InspectionReport inspectContract(Long tenantId, String tableKey, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseTableContractDO contract = findContract(scopedTenantId, tableKey);
        return inspect(scopedTenantId, contract, inspectedBy);
    }

    public InspectionSummary inspectAll(Long tenantId, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        List<InspectionReport> reports = listContracts(scopedTenantId, null, STATUS_ACTIVE).stream()
                .map(contract -> inspectContract(scopedTenantId, contract.tableKey(), inspectedBy))
                .toList();
        long passed = reports.stream().filter(report -> STATUS_PASS.equals(report.status())).count();
        long warned = reports.stream().filter(report -> STATUS_WARN.equals(report.status())).count();
        long failed = reports.stream().filter(report -> STATUS_FAIL.equals(report.status())).count();
        return new InspectionSummary(scopedTenantId, reports.size(), passed, warned, failed, reports);
    }

    public InspectionReport inspectLiveContract(Long tenantId, String tableKey, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseTableContractDO contract = findContract(scopedTenantId, tableKey);
        return inspectLive(scopedTenantId, contract, inspectedBy);
    }

    public InspectionSummary inspectLiveAll(Long tenantId, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        List<InspectionReport> reports = listContracts(scopedTenantId, null, STATUS_ACTIVE).stream()
                .map(contract -> inspectLiveContract(scopedTenantId, contract.tableKey(), inspectedBy))
                .toList();
        long passed = reports.stream().filter(report -> STATUS_PASS.equals(report.status())).count();
        long warned = reports.stream().filter(report -> STATUS_WARN.equals(report.status())).count();
        long failed = reports.stream().filter(report -> STATUS_FAIL.equals(report.status())).count();
        return new InspectionSummary(scopedTenantId, reports.size(), passed, warned, failed, reports);
    }

    public TableRemediationPlan planRemediation(Long tenantId,
                                                String tableKey,
                                                boolean live,
                                                String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseTableContractDO contract = findContract(scopedTenantId, tableKey);
        InspectionReport report = live
                ? inspectLive(scopedTenantId, contract, inspectedBy)
                : inspect(scopedTenantId, contract, inspectedBy);
        return remediationPlan(scopedTenantId, live, contract, report);
    }

    public RemediationSummary planAllRemediation(Long tenantId, boolean live, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        List<TableRemediationPlan> tables = listContracts(scopedTenantId, null, STATUS_ACTIVE).stream()
                .map(contract -> planRemediation(scopedTenantId, contract.tableKey(), live, inspectedBy))
                .toList();
        long executableSteps = tables.stream()
                .flatMap(table -> table.steps().stream())
                .filter(RemediationStep::executable)
                .count();
        long manualSteps = tables.stream()
                .flatMap(table -> table.steps().stream())
                .filter(step -> !step.executable())
                .count();
        long tablesWithRemediation = tables.stream()
                .filter(table -> !table.steps().isEmpty())
                .count();
        return new RemediationSummary(
                scopedTenantId,
                live,
                tables.size(),
                tablesWithRemediation,
                executableSteps,
                manualSteps,
                tables);
    }

    private CdpWarehouseTableContractDO findContract(Long tenantId, String tableKey) {
        String scopedTableKey = required(tableKey, "tableKey");
        LambdaQueryWrapper<CdpWarehouseTableContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseTableContractDO>()
                        .in(CdpWarehouseTableContractDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseTableContractDO::getTableKey, scopedTableKey)
                        .orderByAsc(CdpWarehouseTableContractDO::getTenantId);
        CdpWarehouseTableContractDO selected = null;
        for (CdpWarehouseTableContractDO row : safeList(contractMapper.selectList(query))) {
            selected = row;
        }
        if (selected == null) {
            throw new IllegalArgumentException("table contract not found: " + scopedTableKey);
        }
        return selected;
    }

    private InspectionReport inspect(Long scopedTenantId,
                                     CdpWarehouseTableContractDO contract,
                                     String inspectedBy) {
        LocalDateTime inspectedAt = LocalDateTime.now();
        CheckAccumulator check = new CheckAccumulator();
        String ddl = null;
        if (!hasText(contract.getDdlAssetPath())) {
            check.check(false, "ddlAssetPath is required", true);
        } else {
            try {
                ddl = ddlAssetReader.read(contract.getDdlAssetPath());
                if (!hasText(ddl)) {
                    check.check(false, "DDL asset is empty: " + contract.getDdlAssetPath(), true);
                }
            } catch (RuntimeException e) {
                check.check(false, "DDL asset could not be read: " + contract.getDdlAssetPath(), true);
            }
        }

        if (hasText(ddl)) {
            evaluateDdl(contract, ddl, check);
        }

        return persistInspection(scopedTenantId, contract, inspectedBy, inspectedAt, check, contract.getDdlAssetPath());
    }

    private InspectionReport inspectLive(Long scopedTenantId,
                                         CdpWarehouseTableContractDO contract,
                                         String inspectedBy) {
        LocalDateTime inspectedAt = LocalDateTime.now();
        CheckAccumulator check = new CheckAccumulator();
        String ddl = null;
        if (!ENGINE_DORIS.equalsIgnoreCase(contract.getEngineType())) {
            check.check(false, "live inspection supports DORIS contracts only", true);
        } else if (liveDdlReader == null) {
            check.check(false, "live Doris DDL reader is not configured", true);
        } else {
            try {
                ddl = liveDdlReader.read(contract.getPhysicalName());
                if (!hasText(ddl)) {
                    check.check(false, "live Doris DDL is empty: " + contract.getPhysicalName(), true);
                }
            } catch (RuntimeException e) {
                check.check(false, "live Doris DDL could not be read: "
                        + contract.getPhysicalName() + ": " + limitMessage(e.getMessage()), true);
            }
        }

        if (hasText(ddl)) {
            evaluateDdl(contract, ddl, check);
        }

        return persistInspection(scopedTenantId, contract, inspectedBy, inspectedAt, check, LIVE_DDL_SOURCE);
    }

    private TableRemediationPlan remediationPlan(Long scopedTenantId,
                                                 boolean live,
                                                 CdpWarehouseTableContractDO contract,
                                                 InspectionReport report) {
        List<RemediationStep> steps = report.violations() == null
                ? List.of()
                : report.violations().stream()
                .map(violation -> remediationStep(contract, violation))
                .toList();
        return new TableRemediationPlan(
                scopedTenantId,
                live,
                contract.getTableKey(),
                contract.getPhysicalName(),
                report.status(),
                report.id(),
                report.ddlAssetPath(),
                report.inspectedAt(),
                report.violationCount(),
                report.violations() == null ? List.of() : report.violations(),
                steps);
    }

    private RemediationStep remediationStep(CdpWarehouseTableContractDO contract, String violation) {
        String value = violation == null ? "" : violation;
        if (value.startsWith("DDL does not define RANGE partition")) {
            return manualStep(
                    "PARTITION_REBUILD_REQUIRED",
                    RISK_HIGH,
                    value,
                    "Plan a table rebuild or migration with RANGE partition on "
                            + contract.getPartitionColumn() + " before production cutover.");
        }
        if (value.startsWith("DDL distribution columns do not match")) {
            return manualStep(
                    "DISTRIBUTION_REBUILD_REQUIRED",
                    RISK_HIGH,
                    value,
                    "Plan a table rebuild or migration with HASH distribution columns "
                            + contract.getDistributionColumns() + ".");
        }
        if (value.startsWith("DDL bucket count is not")) {
            return manualStep(
                    "BUCKET_REBUILD_REQUIRED",
                    RISK_HIGH,
                    value,
                    "Plan a table rebuild or migration with " + contract.getBucketCount()
                            + " buckets and validate load distribution.");
        }
        if (value.startsWith("DDL does not contain table")) {
            return manualStep(
                    "TABLE_NAME_OR_CONTRACT_REVIEW",
                    RISK_HIGH,
                    value,
                    "Verify the physical table name in the contract and confirm the live or asset DDL target.");
        }
        if (value.startsWith("DDL asset could not be read") || value.startsWith("DDL asset is empty")
                || value.startsWith("ddlAssetPath is required")) {
            return manualStep(
                    "DDL_ASSET_REPAIR",
                    RISK_HIGH,
                    value,
                    "Repair the contract DDL asset path or restore the missing asset before retrying inspection.");
        }
        if (value.startsWith("live Doris DDL could not be read") || value.startsWith("live Doris DDL is empty")
                || value.startsWith("live Doris DDL reader is not configured")
                || value.startsWith("live inspection supports DORIS contracts only")) {
            return manualStep(
                    "LIVE_DDL_ACCESS_REPAIR",
                    RISK_HIGH,
                    value,
                    "Repair Doris connectivity or contract engine settings before generating live remediation SQL.");
        }
        if (value.startsWith("DDL does not enable dynamic partitions")) {
            return propertyStep(
                    contract,
                    "DYNAMIC_PARTITION_ENABLE",
                    value,
                    "dynamic_partition.enable",
                    "true",
                    "Review and apply dynamic partition enablement.");
        }
        if (value.startsWith("DDL dynamic partition time unit is not")) {
            return propertyStep(
                    contract,
                    "DYNAMIC_PARTITION_TIME_UNIT",
                    value,
                    "dynamic_partition.time_unit",
                    contract.getPartitionGranularity(),
                    "Review and align dynamic partition time unit.");
        }
        if (value.startsWith("DDL retention start is not")) {
            return propertyStep(
                    contract,
                    "DYNAMIC_PARTITION_RETENTION",
                    value,
                    "dynamic_partition.start",
                    "-" + contract.getRetentionDays(),
                    "Review and align dynamic partition retention start.");
        }
        if (value.startsWith("DDL replication_num is not")) {
            return propertyStep(
                    contract,
                    "REPLICATION_NUM",
                    value,
                    "replication_num",
                    String.valueOf(contract.getReplicaCount()),
                    "Review and align Doris replica count.");
        }
        if (value.startsWith("DDL storage_policy is not")) {
            return propertyStep(
                    contract,
                    "STORAGE_POLICY",
                    value,
                    "storage_policy",
                    contract.getStoragePolicy(),
                    "Review and align Doris storage policy.");
        }
        return manualStep(
                "MANUAL_REVIEW_REQUIRED",
                RISK_MEDIUM,
                value,
                "Review the table contract, inspection evidence, and Doris DDL before choosing a remediation path.");
    }

    private RemediationStep propertyStep(CdpWarehouseTableContractDO contract,
                                         String code,
                                         String reason,
                                         String propertyName,
                                         String propertyValue,
                                         String action) {
        String tableName = quotedTableName(contract.getPhysicalName());
        if (!hasText(tableName) || !hasText(propertyValue)) {
            return manualStep(
                    code,
                    RISK_HIGH,
                    reason,
                    action + " Contract physical name or expected value is not safe for generated SQL.");
        }
        String sql = "ALTER TABLE " + tableName + " SET (\""
                + propertyName + "\" = \"" + escapeSqlPropertyValue(propertyValue) + "\");";
        return new RemediationStep(code, RISK_MEDIUM, true, reason, action, sql);
    }

    private RemediationStep manualStep(String code, String riskLevel, String reason, String action) {
        return new RemediationStep(code, riskLevel, false, reason, action, null);
    }

    private InspectionReport persistInspection(Long scopedTenantId,
                                               CdpWarehouseTableContractDO contract,
                                               String inspectedBy,
                                               LocalDateTime inspectedAt,
                                               CheckAccumulator check,
                                               String ddlSource) {
        String status = status(check);
        String message = message(status, check.violations);
        CdpWarehouseTableInspectionDO row = new CdpWarehouseTableInspectionDO();
        row.setTenantId(scopedTenantId);
        row.setTableKey(contract.getTableKey());
        row.setPhysicalName(contract.getPhysicalName());
        row.setStatus(status);
        row.setCheckedItems(check.checkedItems);
        row.setViolationCount(check.violations.size());
        row.setMessage(message);
        row.setViolationsJson(toJsonArray(check.violations));
        row.setDdlAssetPath(ddlSource);
        row.setInspectedBy(normalizeOperator(inspectedBy));
        row.setInspectedAt(inspectedAt);
        inspectionMapper.insert(row);
        contractMapper.updateInspection(contract.getTenantId(), contract.getTableKey(), inspectedAt, status, message);

        return new InspectionReport(
                row.getId(),
                scopedTenantId,
                contract.getTableKey(),
                contract.getPhysicalName(),
                status,
                check.checkedItems,
                check.violations.size(),
                List.copyOf(check.violations),
                message,
                ddlSource,
                inspectedAt);
    }

    private void evaluateDdl(CdpWarehouseTableContractDO contract, String ddl, CheckAccumulator check) {
        String normalized = normalizeDdl(ddl);
        String compact = normalized.replace(" ", "");
        String physicalName = contract.getPhysicalName().toLowerCase(Locale.ROOT);
        check.check(normalized.contains(physicalName),
                "DDL does not contain table " + contract.getPhysicalName(), true);
        check.check(partitionMatches(compact, contract.getPartitionColumn()),
                "DDL does not define RANGE partition on " + contract.getPartitionColumn(), false);
        check.check(hasProperty(compact, "dynamic_partition.enable", "true"),
                "DDL does not enable dynamic partitions", false);
        check.check(hasProperty(compact, "dynamic_partition.time_unit", contract.getPartitionGranularity()),
                "DDL dynamic partition time unit is not " + contract.getPartitionGranularity(), false);
        check.check(hasProperty(compact, "dynamic_partition.start", "-" + contract.getRetentionDays()),
                "DDL retention start is not -" + contract.getRetentionDays(), false);
        check.check(hasProperty(compact, "replication_num", String.valueOf(contract.getReplicaCount())),
                "DDL replication_num is not " + contract.getReplicaCount(), false);
        check.check(distributionMatches(compact, contract.getDistributionColumns()),
                "DDL distribution columns do not match " + contract.getDistributionColumns(), false);
        check.check(bucketMatches(compact, contract.getBucketCount()),
                "DDL bucket count is not " + contract.getBucketCount(), false);
        if (hasText(contract.getStoragePolicy())) {
            check.check(hasProperty(compact, "storage_policy", contract.getStoragePolicy()),
                    "DDL storage_policy is not " + contract.getStoragePolicy(), false);
        }
    }

    private String status(CheckAccumulator check) {
        if (check.violations.isEmpty()) {
            return STATUS_PASS;
        }
        return check.fatal ? STATUS_FAIL : STATUS_WARN;
    }

    private String message(String status, List<String> violations) {
        String value = violations.isEmpty()
                ? "Physical table contract passed"
                : status + " with " + violations.size() + " violation(s): " + String.join("; ", violations);
        return limitMessage(value);
    }

    private String limitMessage(String value) {
        if (!hasText(value)) {
            return "unknown";
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private boolean partitionMatches(String compactDdl, String partitionColumn) {
        if (!hasText(partitionColumn)) {
            return false;
        }
        String column = normalizeIdentifier(partitionColumn);
        return compactDdl.contains("partitionbyrange(" + column + ")");
    }

    private boolean distributionMatches(String compactDdl, String distributionColumns) {
        String clause = hashClause(compactDdl);
        if (!hasText(clause)) {
            return false;
        }
        for (String column : splitColumns(distributionColumns)) {
            if (!clause.contains(normalizeIdentifier(column))) {
                return false;
            }
        }
        return true;
    }

    private boolean bucketMatches(String compactDdl, Integer bucketCount) {
        if (bucketCount == null || bucketCount <= 0) {
            return false;
        }
        int distributedIndex = compactDdl.indexOf("distributedbyhash(");
        return distributedIndex >= 0 && compactDdl.indexOf("buckets" + bucketCount, distributedIndex) >= 0;
    }

    private String hashClause(String compactDdl) {
        int start = compactDdl.indexOf("distributedbyhash(");
        if (start < 0) {
            return "";
        }
        int contentStart = start + "distributedbyhash(".length();
        int end = compactDdl.indexOf(")", contentStart);
        if (end < 0) {
            return "";
        }
        return compactDdl.substring(contentStart, end);
    }

    private boolean hasProperty(String compactDdl, String propertyName, String propertyValue) {
        if (!hasText(propertyName) || propertyValue == null) {
            return false;
        }
        String key = propertyName.trim().toLowerCase(Locale.ROOT);
        String value = propertyValue.trim().toLowerCase(Locale.ROOT);
        return compactDdl.contains("\"" + key + "\"=\"" + value + "\"")
                || compactDdl.contains("'" + key + "'='" + value + "'");
    }

    private String normalizeDdl(String ddl) {
        return ddl.toLowerCase(Locale.ROOT)
                .replace('`', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> splitColumns(String columns) {
        if (!hasText(columns)) {
            return List.of();
        }
        return java.util.Arrays.stream(columns.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    private String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace("`", "");
    }

    private TableContractView toView(CdpWarehouseTableContractDO row) {
        return new TableContractView(
                row.getId(),
                row.getTenantId(),
                row.getTableKey(),
                row.getDatasetKey(),
                row.getLayer(),
                row.getPhysicalName(),
                row.getEngineType(),
                row.getDdlAssetPath(),
                row.getPartitionColumn(),
                row.getPartitionGranularity(),
                row.getRetentionDays(),
                row.getReplicaCount(),
                row.getBucketCount(),
                row.getDistributionColumns(),
                row.getStoragePolicy(),
                row.getLifecycleStatus(),
                row.getOwnerName(),
                row.getDescription(),
                row.getExpectedPropertiesJson(),
                row.getLastInspectedAt(),
                row.getLastInspectionStatus(),
                row.getLastInspectionMessage());
    }

    private String quotedTableName(String physicalName) {
        if (!hasText(physicalName)) {
            return null;
        }
        List<String> parts = java.util.Arrays.stream(physicalName.split("\\."))
                .map(String::trim)
                .filter(this::hasText)
                .toList();
        if (parts.isEmpty()) {
            return null;
        }
        for (String part : parts) {
            if (!part.matches("[A-Za-z0-9_]+")) {
                return null;
            }
        }
        return parts.stream()
                .map(part -> "`" + part + "`")
                .collect(Collectors.joining("."));
    }

    private String escapeSqlPropertyValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    private Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
                .map(this::jsonQuote)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonQuote(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            if (ch == '"' || ch == '\\') {
                builder.append('\\').append(ch);
            } else if (ch == '\n') {
                builder.append("\\n");
            } else if (ch == '\r') {
                builder.append("\\r");
            } else if (ch == '\t') {
                builder.append("\\t");
            } else {
                builder.append(ch);
            }
        }
        return builder.append('"').toString();
    }

    interface DdlAssetReader {
        String read(String assetPath);
    }

    interface LiveDdlReader {
        String read(String physicalName);
    }

    private static final class DorisLiveDdlReader implements LiveDdlReader {

        private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;

        private DorisLiveDdlReader(ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
            this.dorisJdbcTemplate = dorisJdbcTemplate;
        }

        @Override
        public String read(String physicalName) {
            JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
            if (jdbcTemplate == null) {
                throw new IllegalStateException("Doris is disabled");
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW CREATE TABLE " + quotedName(physicalName));
            if (rows == null || rows.isEmpty()) {
                throw new IllegalStateException("Doris returned no DDL");
            }
            for (Object value : rows.get(0).values()) {
                if (value != null && value.toString().toLowerCase(Locale.ROOT).contains("create table")) {
                    return value.toString();
                }
            }
            throw new IllegalStateException("Doris SHOW CREATE TABLE returned no CREATE TABLE text");
        }

        private String quotedName(String physicalName) {
            if (physicalName == null || physicalName.isBlank()) {
                throw new IllegalArgumentException("physicalName is required");
            }
            return java.util.Arrays.stream(physicalName.split("\\."))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .map(this::quotePart)
                    .collect(Collectors.joining("."));
        }

        private String quotePart(String value) {
            if (!value.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("invalid Doris table identifier: " + value);
            }
            return "`" + value + "`";
        }
    }

    private static final class ClasspathDdlAssetReader implements DdlAssetReader {
        @Override
        public String read(String assetPath) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classLoader == null ? null : classLoader.getResourceAsStream(assetPath);
            if (stream == null) {
                stream = CdpWarehouseTableGovernanceService.class.getClassLoader()
                        .getResourceAsStream(assetPath);
            }
            if (stream == null) {
                throw new IllegalArgumentException("DDL asset not found: " + assetPath);
            }
            try (InputStream input = stream) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class CheckAccumulator {
        private final List<String> violations = new ArrayList<>();
        private int checkedItems;
        private boolean fatal;

        private void check(boolean passed, String violation, boolean fatalViolation) {
            checkedItems++;
            if (!passed) {
                fail(violation, fatalViolation);
            }
        }

        private void fail(String violation, boolean fatalViolation) {
            violations.add(violation);
            fatal = fatal || fatalViolation;
        }
    }

    public record TableContractCommand(
            String tableKey,
            String datasetKey,
            String layer,
            String physicalName,
            String engineType,
            String ddlAssetPath,
            String partitionColumn,
            String partitionGranularity,
            Integer retentionDays,
            Integer replicaCount,
            Integer bucketCount,
            String distributionColumns,
            String storagePolicy,
            String lifecycleStatus,
            String ownerName,
            String description,
            String expectedPropertiesJson) {
    }

    public record TableContractView(
            Long id,
            Long tenantId,
            String tableKey,
            String datasetKey,
            String layer,
            String physicalName,
            String engineType,
            String ddlAssetPath,
            String partitionColumn,
            String partitionGranularity,
            Integer retentionDays,
            Integer replicaCount,
            Integer bucketCount,
            String distributionColumns,
            String storagePolicy,
            String lifecycleStatus,
            String ownerName,
            String description,
            String expectedPropertiesJson,
            LocalDateTime lastInspectedAt,
            String lastInspectionStatus,
            String lastInspectionMessage) {
    }

    public record InspectionReport(
            Long id,
            Long tenantId,
            String tableKey,
            String physicalName,
            String status,
            int checkedItems,
            int violationCount,
            List<String> violations,
            String message,
            String ddlAssetPath,
            LocalDateTime inspectedAt) {
    }

    public record InspectionSummary(
            Long tenantId,
            int total,
            long passed,
            long warned,
            long failed,
            List<InspectionReport> reports) {
    }

    public record RemediationSummary(
            Long tenantId,
            boolean live,
            int total,
            long tablesWithRemediation,
            long executableSteps,
            long manualSteps,
            List<TableRemediationPlan> tables) {
    }

    public record TableRemediationPlan(
            Long tenantId,
            boolean live,
            String tableKey,
            String physicalName,
            String status,
            Long inspectionId,
            String ddlSource,
            LocalDateTime inspectedAt,
            int violationCount,
            List<String> violations,
            List<RemediationStep> steps) {
    }

    public record RemediationStep(
            String code,
            String riskLevel,
            boolean executable,
            String reason,
            String operatorAction,
            String recommendedSql) {
    }
}
