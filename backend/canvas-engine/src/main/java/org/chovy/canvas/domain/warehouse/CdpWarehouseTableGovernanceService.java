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
/**
 * CdpWarehouseTableGovernanceService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseTableGovernanceService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                              CdpWarehouseTableInspectionMapper inspectionMapper) {
        this(contractMapper, inspectionMapper, new ClasspathDdlAssetReader(), null);
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseTableGovernanceService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseTableGovernanceService 流程中的校验、计算或对象转换。
     */
    public CdpWarehouseTableGovernanceService(
            CdpWarehouseTableContractMapper contractMapper,
            CdpWarehouseTableInspectionMapper inspectionMapper,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this(contractMapper, inspectionMapper, new ClasspathDdlAssetReader(),
                new DorisLiveDdlReader(dorisJdbcTemplate));
    }

    /**
     * 初始化 CdpWarehouseTableGovernanceService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ddlAssetReader ddl asset reader 参数，用于 CdpWarehouseTableGovernanceService 流程中的校验、计算或对象转换。
     */
    CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                       DdlAssetReader ddlAssetReader) {
        this(contractMapper, inspectionMapper, ddlAssetReader, null);
    }

    /**
     * 初始化 CdpWarehouseTableGovernanceService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param inspectionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param ddlAssetReader ddl asset reader 参数，用于 CdpWarehouseTableGovernanceService 流程中的校验、计算或对象转换。
     * @param liveDdlReader live ddl reader 参数，用于 CdpWarehouseTableGovernanceService 流程中的校验、计算或对象转换。
     */
    CdpWarehouseTableGovernanceService(CdpWarehouseTableContractMapper contractMapper,
                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                       DdlAssetReader ddlAssetReader,
                                       LiveDdlReader liveDdlReader) {
        this.contractMapper = contractMapper;
        this.inspectionMapper = inspectionMapper;
        this.ddlAssetReader = ddlAssetReader;
        this.liveDdlReader = liveDdlReader;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public TableContractView upsertContract(Long tenantId, TableContractCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        contractMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param layer layer 参数，用于 listContracts 流程中的校验、计算或对象转换。
     * @param lifecycleStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<TableContractView> listContracts(Long tenantId, String layer, String lifecycleStatus) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseTableContractDO> query = new LambdaQueryWrapper<CdpWarehouseTableContractDO>()
                .in(CdpWarehouseTableContractDO::getTenantId, tenantScope(scopedTenantId))
                .orderByAsc(CdpWarehouseTableContractDO::getTenantId)
                .orderByAsc(CdpWarehouseTableContractDO::getLayer)
                .orderByAsc(CdpWarehouseTableContractDO::getTableKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(layer)) {
            query.eq(CdpWarehouseTableContractDO::getLayer, layer.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(lifecycleStatus)) {
            query.eq(CdpWarehouseTableContractDO::getLifecycleStatus,
                    lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, TableContractView> byKey = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseTableContractDO row : safeList(contractMapper.selectList(query))) {
            byKey.put(row.getTableKey(), toView(row));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(byKey.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tableKey 业务键，用于在同一租户下定位资源。
     * @param inspectedBy inspected by 参数，用于 inspectContract 流程中的校验、计算或对象转换。
     * @return 返回 inspectContract 流程生成的业务结果。
     */
    public InspectionReport inspectContract(Long tenantId, String tableKey, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseTableContractDO contract = findContract(scopedTenantId, tableKey);
        return inspect(scopedTenantId, contract, inspectedBy);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param inspectedBy inspected by 参数，用于 inspectAll 流程中的校验、计算或对象转换。
     * @return 返回 inspectAll 流程生成的业务结果。
     */
    public InspectionSummary inspectAll(Long tenantId, String inspectedBy) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<InspectionReport> reports = listContracts(scopedTenantId, null, STATUS_ACTIVE).stream()
                .map(contract -> inspectContract(scopedTenantId, contract.tableKey(), inspectedBy))
                .toList();
        long passed = reports.stream().filter(report -> STATUS_PASS.equals(report.status())).count();
        long warned = reports.stream().filter(report -> STATUS_WARN.equals(report.status())).count();
        long failed = reports.stream().filter(report -> STATUS_FAIL.equals(report.status())).count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new InspectionSummary(scopedTenantId, reports.size(), passed, warned, failed, reports);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tableKey 业务键，用于在同一租户下定位资源。
     * @param inspectedBy inspected by 参数，用于 inspectLiveContract 流程中的校验、计算或对象转换。
     * @return 返回 inspectLiveContract 流程生成的业务结果。
     */
    public InspectionReport inspectLiveContract(Long tenantId, String tableKey, String inspectedBy) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseTableContractDO contract = findContract(scopedTenantId, tableKey);
        return inspectLive(scopedTenantId, contract, inspectedBy);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param inspectedBy inspected by 参数，用于 inspectLiveAll 流程中的校验、计算或对象转换。
     * @return 返回 inspectLiveAll 流程生成的业务结果。
     */
    public InspectionSummary inspectLiveAll(Long tenantId, String inspectedBy) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<InspectionReport> reports = listContracts(scopedTenantId, null, STATUS_ACTIVE).stream()
                .map(contract -> inspectLiveContract(scopedTenantId, contract.tableKey(), inspectedBy))
                .toList();
        long passed = reports.stream().filter(report -> STATUS_PASS.equals(report.status())).count();
        long warned = reports.stream().filter(report -> STATUS_WARN.equals(report.status())).count();
        long failed = reports.stream().filter(report -> STATUS_FAIL.equals(report.status())).count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new InspectionSummary(scopedTenantId, reports.size(), passed, warned, failed, reports);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tableKey 业务键，用于在同一租户下定位资源。
     * @param live live 参数，用于 planRemediation 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 planRemediation 流程中的校验、计算或对象转换。
     * @return 返回 planRemediation 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param live live 参数，用于 planAllRemediation 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 planAllRemediation 流程中的校验、计算或对象转换。
     * @return 返回 planAllRemediation 流程生成的业务结果。
     */
    public RemediationSummary planAllRemediation(Long tenantId, boolean live, String inspectedBy) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RemediationSummary(
                scopedTenantId,
                live,
                tables.size(),
                tablesWithRemediation,
                executableSteps,
                manualSteps,
                tables);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tableKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehouseTableContractDO findContract(Long tenantId, String tableKey) {
        String scopedTableKey = required(tableKey, "tableKey");
        LambdaQueryWrapper<CdpWarehouseTableContractDO> query =
                new LambdaQueryWrapper<CdpWarehouseTableContractDO>()
                        .in(CdpWarehouseTableContractDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseTableContractDO::getTableKey, scopedTableKey)
                        .orderByAsc(CdpWarehouseTableContractDO::getTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehouseTableContractDO selected = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseTableContractDO row : safeList(contractMapper.selectList(query))) {
            selected = row;
        }
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (selected == null) {
            throw new IllegalArgumentException("table contract not found: " + scopedTableKey);
        }
        return selected;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param scopedTenantId 业务对象 ID，用于定位具体记录。
     * @param contract contract 参数，用于 inspect 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 inspect 流程中的校验、计算或对象转换。
     * @return 返回 inspect 流程生成的业务结果。
     */
    private InspectionReport inspect(Long scopedTenantId,
                                     CdpWarehouseTableContractDO contract,
                                     String inspectedBy) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime inspectedAt = LocalDateTime.now();
        CheckAccumulator check = new CheckAccumulator();
        String ddl = null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

        // 汇总前面计算出的状态和明细，返回给调用方。
        return persistInspection(scopedTenantId, contract, inspectedBy, inspectedAt, check, contract.getDdlAssetPath());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param scopedTenantId 业务对象 ID，用于定位具体记录。
     * @param contract contract 参数，用于 inspectLive 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 inspectLive 流程中的校验、计算或对象转换。
     * @return 返回 inspectLive 流程生成的业务结果。
     */
    private InspectionReport inspectLive(Long scopedTenantId,
                                         CdpWarehouseTableContractDO contract,
                                         String inspectedBy) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime inspectedAt = LocalDateTime.now();
        CheckAccumulator check = new CheckAccumulator();
        String ddl = null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

        // 汇总前面计算出的状态和明细，返回给调用方。
        return persistInspection(scopedTenantId, contract, inspectedBy, inspectedAt, check, LIVE_DDL_SOURCE);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param scopedTenantId 业务对象 ID，用于定位具体记录。
     * @param live live 参数，用于 remediationPlan 流程中的校验、计算或对象转换。
     * @param contract contract 参数，用于 remediationPlan 流程中的校验、计算或对象转换。
     * @param report report 参数，用于 remediationPlan 流程中的校验、计算或对象转换。
     * @return 返回 remediationPlan 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contract contract 参数，用于 remediationStep 流程中的校验、计算或对象转换。
     * @param violation violation 参数，用于 remediationStep 流程中的校验、计算或对象转换。
     * @return 返回 remediationStep 流程生成的业务结果。
     */
    private RemediationStep remediationStep(CdpWarehouseTableContractDO contract, String violation) {
        // 准备本次处理所需的上下文和中间变量。
        String value = violation == null ? "" : violation;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return manualStep(
                "MANUAL_REVIEW_REQUIRED",
                RISK_MEDIUM,
                value,
                "Review the table contract, inspection evidence, and Doris DDL before choosing a remediation path.");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contract contract 参数，用于 propertyStep 流程中的校验、计算或对象转换。
     * @param code 业务编码，用于匹配对应类型或状态。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param propertyName 名称文本，用于展示或唯一性校验。
     * @param propertyValue 待处理值，用于规则计算或转换。
     * @param action action 参数，用于 propertyStep 流程中的校验、计算或对象转换。
     * @return 返回 propertyStep 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param code 业务编码，用于匹配对应类型或状态。
     * @param riskLevel risk level 参数，用于 manualStep 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param action action 参数，用于 manualStep 流程中的校验、计算或对象转换。
     * @return 返回 manualStep 流程生成的业务结果。
     */
    private RemediationStep manualStep(String code, String riskLevel, String reason, String action) {
        return new RemediationStep(code, riskLevel, false, reason, action, null);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param scopedTenantId 业务对象 ID，用于定位具体记录。
     * @param contract contract 参数，用于 persistInspection 流程中的校验、计算或对象转换。
     * @param inspectedBy inspected by 参数，用于 persistInspection 流程中的校验、计算或对象转换。
     * @param inspectedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param check check 参数，用于 persistInspection 流程中的校验、计算或对象转换。
     * @param ddlSource ddl source 参数，用于 persistInspection 流程中的校验、计算或对象转换。
     * @return 返回 persistInspection 流程生成的业务结果。
     */
    private InspectionReport persistInspection(Long scopedTenantId,
                                               CdpWarehouseTableContractDO contract,
                                               String inspectedBy,
                                               LocalDateTime inspectedAt,
                                               CheckAccumulator check,
                                               String ddlSource) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        inspectionMapper.insert(row);
        contractMapper.updateInspection(contract.getTenantId(), contract.getTableKey(), inspectedAt, status, message);

        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param contract contract 参数，用于 evaluateDdl 流程中的校验、计算或对象转换。
     * @param ddl ddl 参数，用于 evaluateDdl 流程中的校验、计算或对象转换。
     * @param check check 参数，用于 evaluateDdl 流程中的校验、计算或对象转换。
     */
    private void evaluateDdl(CdpWarehouseTableContractDO contract, String ddl, CheckAccumulator check) {
        // 准备本次处理所需的上下文和中间变量。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param check check 参数，用于 status 流程中的校验、计算或对象转换。
     * @return 返回 status 生成的文本或业务键。
     */
    private String status(CheckAccumulator check) {
        if (check.violations.isEmpty()) {
            return STATUS_PASS;
        }
        return check.fatal ? STATUS_FAIL : STATUS_WARN;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param violations violations 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(String status, List<String> violations) {
        String value = violations.isEmpty()
                ? "Physical table contract passed"
                : status + " with " + violations.size() + " violation(s): " + String.join("; ", violations);
        return limitMessage(value);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limitMessage(String value) {
        if (!hasText(value)) {
            return "unknown";
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param compactDdl compact ddl 参数，用于 partitionMatches 流程中的校验、计算或对象转换。
     * @param partitionColumn partition column 参数，用于 partitionMatches 流程中的校验、计算或对象转换。
     * @return 返回 partition matches 的布尔判断结果。
     */
    private boolean partitionMatches(String compactDdl, String partitionColumn) {
        if (!hasText(partitionColumn)) {
            return false;
        }
        String column = normalizeIdentifier(partitionColumn);
        return compactDdl.contains("partitionbyrange(" + column + ")");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param compactDdl compact ddl 参数，用于 distributionMatches 流程中的校验、计算或对象转换。
     * @param distributionColumns distribution columns 参数，用于 distributionMatches 流程中的校验、计算或对象转换。
     * @return 返回 distribution matches 的布尔判断结果。
     */
    private boolean distributionMatches(String compactDdl, String distributionColumns) {
        String clause = hashClause(compactDdl);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(clause)) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String column : splitColumns(distributionColumns)) {
            if (!clause.contains(normalizeIdentifier(column))) {
                return false;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return true;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param compactDdl compact ddl 参数，用于 bucketMatches 流程中的校验、计算或对象转换。
     * @param bucketCount bucket count 参数，用于 bucketMatches 流程中的校验、计算或对象转换。
     * @return 返回 bucket matches 的布尔判断结果。
     */
    private boolean bucketMatches(String compactDdl, Integer bucketCount) {
        if (bucketCount == null || bucketCount <= 0) {
            return false;
        }
        int distributedIndex = compactDdl.indexOf("distributedbyhash(");
        return distributedIndex >= 0 && compactDdl.indexOf("buckets" + bucketCount, distributedIndex) >= 0;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param compactDdl compact ddl 参数，用于 hashClause 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param compactDdl compact ddl 参数，用于 hasProperty 流程中的校验、计算或对象转换。
     * @param propertyName 名称文本，用于展示或唯一性校验。
     * @param propertyValue 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasProperty(String compactDdl, String propertyName, String propertyValue) {
        if (!hasText(propertyName) || propertyValue == null) {
            return false;
        }
        String key = propertyName.trim().toLowerCase(Locale.ROOT);
        String value = propertyValue.trim().toLowerCase(Locale.ROOT);
        return compactDdl.contains("\"" + key + "\"=\"" + value + "\"")
                || compactDdl.contains("'" + key + "'='" + value + "'");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param ddl ddl 参数，用于 normalizeDdl 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeDdl(String ddl) {
        return ddl.toLowerCase(Locale.ROOT)
                .replace('`', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param columns columns 参数，用于 splitColumns 流程中的校验、计算或对象转换。
     * @return 返回 split columns 汇总后的集合、分页或映射视图。
     */
    private List<String> splitColumns(String columns) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(columns)) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return java.util.Arrays.stream(columns.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace("`", "");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private TableContractView toView(CdpWarehouseTableContractDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param physicalName 名称文本，用于展示或唯一性校验。
     * @return 返回 quoted table name 生成的文本或业务键。
     */
    private String quotedTableName(String physicalName) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(physicalName)) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String escapeSqlPropertyValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require positive 计算得到的数量、金额或指标值。
     */
    private Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param values values 参数，用于 toJsonArray 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJsonArray(List<String> values) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .map(this::jsonQuote)
                .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json quote 生成的文本或业务键。
     */
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

    /**
     * DdlAssetReader 承载对应领域的业务规则、流程编排和结果转换。
     */
    interface DdlAssetReader {
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param assetPath asset path 参数，用于 read 流程中的校验、计算或对象转换。
         * @return 返回 read 生成的文本或业务键。
         */
        String read(String assetPath);
    }

    /**
     * LiveDdlReader 承载对应领域的业务规则、流程编排和结果转换。
     */
    interface LiveDdlReader {
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param physicalName 名称文本，用于展示或唯一性校验。
         * @return 返回 read 生成的文本或业务键。
         */
        String read(String physicalName);
    }

    /**
     * DorisLiveDdlReader 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class DorisLiveDdlReader implements LiveDdlReader {

        private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param dorisJdbcTemplate doris jdbc template 参数，用于 DorisLiveDdlReader 流程中的校验、计算或对象转换。
         * @return 返回 DorisLiveDdlReader 流程生成的业务结果。
         */
        private DorisLiveDdlReader(ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
            this.dorisJdbcTemplate = dorisJdbcTemplate;
        }

        @Override
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param physicalName 名称文本，用于展示或唯一性校验。
         * @return 返回 read 生成的文本或业务键。
         */
        public String read(String physicalName) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            JdbcTemplate jdbcTemplate = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (jdbcTemplate == null) {
                throw new IllegalStateException("Doris is disabled");
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW CREATE TABLE " + quotedName(physicalName));
            if (rows == null || rows.isEmpty()) {
                throw new IllegalStateException("Doris returned no DDL");
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Object value : rows.get(0).values()) {
                if (value != null && value.toString().toLowerCase(Locale.ROOT).contains("create table")) {
                    return value.toString();
                }
            }
            throw new IllegalStateException("Doris SHOW CREATE TABLE returned no CREATE TABLE text");
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param physicalName 名称文本，用于展示或唯一性校验。
         * @return 返回 quoted name 生成的文本或业务键。
         */
        private String quotedName(String physicalName) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (physicalName == null || physicalName.isBlank()) {
                throw new IllegalArgumentException("physicalName is required");
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return java.util.Arrays.stream(physicalName.split("\\."))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .map(this::quotePart)
                    .collect(Collectors.joining("."));
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回 quote part 生成的文本或业务键。
         */
        private String quotePart(String value) {
            if (!value.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("invalid Doris table identifier: " + value);
            }
            return "`" + value + "`";
        }
    }

    /**
     * ClasspathDdlAssetReader 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class ClasspathDdlAssetReader implements DdlAssetReader {
        @Override
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param assetPath asset path 参数，用于 read 流程中的校验、计算或对象转换。
         * @return 返回 read 生成的文本或业务键。
         */
        public String read(String assetPath) {
            // 准备本次处理所需的上下文和中间变量。
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classLoader == null ? null : classLoader.getResourceAsStream(assetPath);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (stream == null) {
                stream = CdpWarehouseTableGovernanceService.class.getClassLoader()
                        .getResourceAsStream(assetPath);
            }
            if (stream == null) {
                throw new IllegalArgumentException("DDL asset not found: " + assetPath);
            }
            try (InputStream input = stream) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * CheckAccumulator 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class CheckAccumulator {
        private final List<String> violations = new ArrayList<>();
        private int checkedItems;
        private boolean fatal;

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param passed passed 参数，用于 check 流程中的校验、计算或对象转换。
         * @param violation violation 参数，用于 check 流程中的校验、计算或对象转换。
         * @param fatalViolation fatal violation 参数，用于 check 流程中的校验、计算或对象转换。
         */
        private void check(boolean passed, String violation, boolean fatalViolation) {
            checkedItems++;
            if (!passed) {
                fail(violation, fatalViolation);
            }
        }

        /**
         * 推进状态流转并记录本次处理结果。
         *
         * @param violation violation 参数，用于 fail 流程中的校验、计算或对象转换。
         * @param fatalViolation fatal violation 参数，用于 fail 流程中的校验、计算或对象转换。
         */
        private void fail(String violation, boolean fatalViolation) {
            violations.add(violation);
            fatal = fatal || fatalViolation;
        }
    }

    /**
     * TableContractCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * TableContractView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * InspectionReport 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * InspectionSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record InspectionSummary(
            Long tenantId,
            int total,
            long passed,
            long warned,
            long failed,
            List<InspectionReport> reports) {
    }

    /**
     * RemediationSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RemediationSummary(
            Long tenantId,
            boolean live,
            int total,
            long tablesWithRemediation,
            long executableSteps,
            long manualSteps,
            List<TableRemediationPlan> tables) {
    }

    /**
     * TableRemediationPlan 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * RemediationStep 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RemediationStep(
            String code,
            String riskLevel,
            boolean executable,
            String reason,
            String operatorAction,
            String recommendedSql) {
    }
}
