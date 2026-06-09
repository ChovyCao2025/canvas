package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseIncidentMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseIncidentService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseIncidentService {

    private static final int MAX_LIMIT = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_PASS = "PASS";
    private static final String SOURCE_REALTIME_PIPELINE = "WAREHOUSE_REALTIME_PIPELINE";
    private static final String SOURCE_REALTIME_JOB = "WAREHOUSE_REALTIME_JOB";
    private static final String SOURCE_READINESS = "WAREHOUSE_READINESS";
    private static final String SOURCE_TABLE_DRIFT = "WAREHOUSE_TABLE_DRIFT";
    private static final String SOURCE_AVAILABILITY = "WAREHOUSE_AVAILABILITY";
    private static final String SOURCE_CONSUMER_AVAILABILITY = "WAREHOUSE_CONSUMER_AVAILABILITY";

    private final CdpWarehouseIncidentMapper incidentMapper;

    /**
     * 初始化 CdpWarehouseIncidentService 实例。
     *
     * @param incidentMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseIncidentService(CdpWarehouseIncidentMapper incidentMapper) {
        this.incidentMapper = incidentMapper;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param check check 参数，用于 recordQualityIncident 流程中的校验、计算或对象转换。
     */
    public void recordQualityIncident(CdpWarehouseQualityService.QualityCheckResult check) {
        if (check == null || !shouldOpenIncident(check.status())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(check.tenantId()));
        row.setIncidentKey("QUALITY:" + required(check.checkType(), "UNKNOWN"));
        row.setSourceType("WAREHOUSE_QUALITY_CHECK");
        row.setSourceId(check.id());
        row.setSeverity(severity(check.status()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(title(check));
        row.setDescription(description(check));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordRealtimePipelineIncident(RealtimePipelineIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.status())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey("REALTIME_PIPELINE:" + required(input.pipelineKey(), "UNKNOWN"));
        row.setSourceType(SOURCE_REALTIME_PIPELINE);
        row.setSourceId(input.pipelineId());
        row.setSeverity(severity(input.status()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(pipelineTitle(input));
        row.setDescription(pipelineDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordRealtimeJobIncident(RealtimeJobIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.healthStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey("REALTIME_JOB:" + required(input.pipelineKey(), "UNKNOWN")
                + ":" + required(input.jobKey(), "UNKNOWN"));
        row.setSourceType(SOURCE_REALTIME_JOB);
        row.setSourceId(input.jobInstanceId());
        row.setSeverity(severity(input.healthStatus()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(jobTitle(input));
        row.setDescription(jobDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordReadinessIncident(ReadinessIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.sectionStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey("READINESS:" + required(input.sectionKey(), "UNKNOWN"));
        row.setSourceType(SOURCE_READINESS);
        row.setSourceId(null);
        row.setSeverity(severity(input.sectionStatus()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(readinessTitle(input));
        row.setDescription(readinessDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordTableDriftIncident(TableDriftIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.status())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey("TABLE_DRIFT:" + required(input.tableKey(), "UNKNOWN"));
        row.setSourceType(SOURCE_TABLE_DRIFT);
        row.setSourceId(input.inspectionId());
        row.setSeverity(severity(input.status()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(tableDriftTitle(input));
        row.setDescription(tableDriftDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordAvailabilityIncident(AvailabilityIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.gateStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey(availabilityIncidentKey(input.mode(), input.gateKey()));
        row.setSourceType(SOURCE_AVAILABILITY);
        row.setSourceId(null);
        row.setSeverity(severity(input.gateStatus()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(availabilityTitle(input));
        row.setDescription(availabilityDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     */
    public void recordConsumerAvailabilityIncident(ConsumerAvailabilityIncidentInput input) {
        if (input == null || !shouldOpenIncident(input.contractStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setTenantId(normalizeTenant(input.tenantId()));
        row.setIncidentKey(consumerAvailabilityIncidentKey(input.contractKey()));
        row.setSourceType(SOURCE_CONSUMER_AVAILABILITY);
        row.setSourceId(null);
        row.setSeverity(severity(input.contractStatus()));
        row.setStatus(STATUS_OPEN);
        row.setTitle(consumerAvailabilityTitle(input));
        row.setDescription(consumerAvailabilityDescription(input));
        row.setFirstSeenAt(now);
        row.setLastSeenAt(now);
        incidentMapper.upsertOpen(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<IncidentView> listIncidents(Long tenantId, String status, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseIncidentDO> query = new LambdaQueryWrapper<CdpWarehouseIncidentDO>()
                .eq(CdpWarehouseIncidentDO::getTenantId, scopedTenantId)
                .orderByDesc(CdpWarehouseIncidentDO::getLastSeenAt)
                .orderByDesc(CdpWarehouseIncidentDO::getId)
                .last("LIMIT " + boundLimit(limit));
        if (hasText(status)) {
            query.eq(CdpWarehouseIncidentDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        List<CdpWarehouseIncidentDO> rows = incidentMapper.selectList(query);
        return rows == null ? List.of() : rows.stream().map(this::toView).toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param incidentId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 acknowledge 的布尔判断结果。
     */
    public boolean acknowledge(Long tenantId, Long incidentId, String operator) {
        requireId(incidentId);
        return incidentMapper.acknowledge(normalizeTenant(tenantId), incidentId,
                normalizeOperator(operator), LocalDateTime.now()) == 1;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param incidentId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 resolve 的布尔判断结果。
     */
    public boolean resolve(Long tenantId, Long incidentId, String operator) {
        requireId(incidentId);
        return incidentMapper.resolve(normalizeTenant(tenantId), incidentId,
                normalizeOperator(operator), LocalDateTime.now()) == 1;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tableKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 resolve table drift incident 的布尔判断结果。
     */
    public boolean resolveTableDriftIncident(Long tenantId, String tableKey, String operator) {
        String incidentKey = "TABLE_DRIFT:" + required(tableKey, "UNKNOWN");
        return incidentMapper.resolveTableDriftByKey(
                normalizeTenant(tenantId),
                incidentKey,
                normalizeOperator(operator),
                LocalDateTime.now()) == 1;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mode mode 参数，用于 resolveAvailabilityIncident 流程中的校验、计算或对象转换。
     * @param gateKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 resolve availability incident 的布尔判断结果。
     */
    public boolean resolveAvailabilityIncident(Long tenantId, String mode, String gateKey, String operator) {
        return incidentMapper.resolveAvailabilityByKey(
                normalizeTenant(tenantId),
                availabilityIncidentKey(mode, gateKey),
                normalizeOperator(operator),
                LocalDateTime.now()) == 1;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 resolve consumer availability incident 的布尔判断结果。
     */
    public boolean resolveConsumerAvailabilityIncident(Long tenantId, String contractKey, String operator) {
        return incidentMapper.resolveConsumerAvailabilityByKey(
                normalizeTenant(tenantId),
                consumerAvailabilityIncidentKey(contractKey),
                normalizeOperator(operator),
                LocalDateTime.now()) == 1;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private IncidentView toView(CdpWarehouseIncidentDO row) {
        return new IncidentView(
                row.getId(),
                row.getTenantId(),
                row.getIncidentKey(),
                row.getSourceType(),
                row.getSourceId(),
                row.getSeverity(),
                row.getStatus(),
                row.getTitle(),
                row.getDescription(),
                nullToZero(row.getOccurrenceCount()),
                row.getFirstSeenAt(),
                row.getLastSeenAt(),
                row.getAcknowledgedBy(),
                row.getAcknowledgedAt(),
                row.getResolvedBy(),
                row.getResolvedAt());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean shouldOpenIncident(String status) {
        if (!hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return !STATUS_PASS.equals(normalized) && !STATUS_SKIPPED.equals(normalized);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 severity 生成的文本或业务键。
     */
    private String severity(String status) {
        if (!hasText(status)) {
            return STATUS_WARN;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return STATUS_FAIL.equals(normalized) ? "CRITICAL" : STATUS_WARN;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param check check 参数，用于 title 流程中的校验、计算或对象转换。
     * @return 返回 title 生成的文本或业务键。
     */
    private String title(CdpWarehouseQualityService.QualityCheckResult check) {
        return "Warehouse quality " + check.status() + ": " + check.checkType();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 pipeline title 生成的文本或业务键。
     */
    private String pipelineTitle(RealtimePipelineIncidentInput input) {
        return "Warehouse realtime pipeline " + upperDefault(input.status(), STATUS_WARN)
                + ": " + required(input.pipelineKey(), "UNKNOWN");
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 readiness title 生成的文本或业务键。
     */
    private String readinessTitle(ReadinessIncidentInput input) {
        return "Warehouse readiness " + upperDefault(input.sectionStatus(), STATUS_WARN)
                + ": " + required(input.sectionKey(), "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 job title 生成的文本或业务键。
     */
    private String jobTitle(RealtimeJobIncidentInput input) {
        return "Warehouse realtime job " + upperDefault(input.healthStatus(), STATUS_WARN)
                + ": " + required(input.pipelineKey(), "UNKNOWN")
                + "/" + required(input.jobKey(), "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 table drift title 生成的文本或业务键。
     */
    private String tableDriftTitle(TableDriftIncidentInput input) {
        return "Warehouse table drift " + upperDefault(input.status(), STATUS_WARN)
                + ": " + required(input.tableKey(), "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 availability title 生成的文本或业务键。
     */
    private String availabilityTitle(AvailabilityIncidentInput input) {
        return "Warehouse availability " + upperDefault(input.gateStatus(), STATUS_WARN)
                + ": " + required(input.mode(), "HYBRID")
                + "/" + required(input.gateKey(), "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 consumer availability title 生成的文本或业务键。
     */
    private String consumerAvailabilityTitle(ConsumerAvailabilityIncidentInput input) {
        return "Warehouse consumer availability " + upperDefault(input.contractStatus(), STATUS_WARN)
                + ": " + required(input.contractKey(), "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param check check 参数，用于 description 流程中的校验、计算或对象转换。
     * @return 返回 description 生成的文本或业务键。
     */
    private String description(CdpWarehouseQualityService.QualityCheckResult check) {
        String value = "checkType=" + check.checkType()
                + ", status=" + check.status()
                + ", sourceCount=" + check.sourceCount()
                + ", warehouseCount=" + check.warehouseCount()
                + ", diff=" + check.diffCount()
                + ", threshold=" + check.thresholdValue()
                + ", details=" + check.details();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 pipeline description 生成的文本或业务键。
     */
    private String pipelineDescription(RealtimePipelineIncidentInput input) {
        String value = "pipelineKey=" + input.pipelineKey()
                + ", status=" + input.status()
                + ", sinkRef=" + input.sinkRef()
                + ", checkpointId=" + input.checkpointId()
                + ", checkpointAt=" + input.checkpointAt()
                + ", lagMs=" + input.lagMs()
                + ", message=" + input.message()
                + ", reasons=" + input.reasons();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 readiness description 生成的文本或业务键。
     */
    private String readinessDescription(ReadinessIncidentInput input) {
        String value = "readinessStatus=" + input.readinessStatus()
                + ", sectionKey=" + input.sectionKey()
                + ", sectionStatus=" + input.sectionStatus()
                + ", reason=" + input.reason()
                + ", generatedAt=" + input.generatedAt();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 job description 生成的文本或业务键。
     */
    private String jobDescription(RealtimeJobIncidentInput input) {
        String value = "pipelineKey=" + input.pipelineKey()
                + ", jobKey=" + input.jobKey()
                + ", engineType=" + input.engineType()
                + ", engineJobId=" + input.engineJobId()
                + ", deploymentRef=" + input.deploymentRef()
                + ", runtimeStatus=" + input.runtimeStatus()
                + ", desiredStatus=" + input.desiredStatus()
                + ", healthStatus=" + input.healthStatus()
                + ", lastHeartbeatAt=" + input.lastHeartbeatAt()
                + ", ownerName=" + input.ownerName()
                + ", lastErrorMessage=" + input.lastErrorMessage()
                + ", reasons=" + input.reasons();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 table drift description 生成的文本或业务键。
     */
    private String tableDriftDescription(TableDriftIncidentInput input) {
        String value = "tableKey=" + input.tableKey()
                + ", physicalName=" + input.physicalName()
                + ", status=" + input.status()
                + ", source=" + input.inspectionSource()
                + ", inspectionId=" + input.inspectionId()
                + ", checkedItems=" + input.checkedItems()
                + ", violationCount=" + input.violationCount()
                + ", inspectedAt=" + input.inspectedAt()
                + ", message=" + input.message()
                + ", violations=" + input.violations();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 availability description 生成的文本或业务键。
     */
    private String availabilityDescription(AvailabilityIncidentInput input) {
        String value = "mode=" + input.mode()
                + ", gateKey=" + input.gateKey()
                + ", gateStatus=" + input.gateStatus()
                + ", decisionStatus=" + input.decisionStatus()
                + ", reason=" + input.reason()
                + ", requestedFrom=" + input.requestedFrom()
                + ", requestedTo=" + input.requestedTo()
                + ", availableUntil=" + input.availableUntil()
                + ", lagMinutes=" + input.lagMinutes()
                + ", evidenceCount=" + input.evidenceCount()
                + ", generatedAt=" + input.generatedAt();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 consumer availability description 生成的文本或业务键。
     */
    private String consumerAvailabilityDescription(ConsumerAvailabilityIncidentInput input) {
        String value = "contractKey=" + input.contractKey()
                + ", consumerType=" + input.consumerType()
                + ", consumerRef=" + input.consumerRef()
                + ", mode=" + input.mode()
                + ", contractStatus=" + input.contractStatus()
                + ", allowed=" + input.allowed()
                + ", gatePolicy=" + input.gatePolicy()
                + ", message=" + input.message()
                + ", requestedFrom=" + input.requestedFrom()
                + ", requestedTo=" + input.requestedTo()
                + ", evaluatedAt=" + input.evaluatedAt()
                + ", assetGates=" + input.assetGateSummaries();
        if (value.length() <= MAX_DESCRIPTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param mode mode 参数，用于 availabilityIncidentKey 流程中的校验、计算或对象转换。
     * @param gateKey 业务键，用于在同一租户下定位资源。
     * @return 返回 availability incident key 生成的文本或业务键。
     */
    private String availabilityIncidentKey(String mode, String gateKey) {
        return "AVAILABILITY:" + required(mode, "HYBRID") + ":" + required(gateKey, "UNKNOWN");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @return 返回 consumer availability incident key 生成的文本或业务键。
     */
    private String consumerAvailabilityIncidentKey(String contractKey) {
        return "CONSUMER_AVAILABILITY:" + required(contractKey, "UNKNOWN");
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
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 required 流程中的校验、计算或对象转换。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 upperDefault 流程中的校验、计算或对象转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     */
    private void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
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
     * IncidentView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record IncidentView(
            Long id,
            Long tenantId,
            String incidentKey,
            String sourceType,
            Long sourceId,
            String severity,
            String status,
            String title,
            String description,
            long occurrenceCount,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt,
            String acknowledgedBy,
            LocalDateTime acknowledgedAt,
            String resolvedBy,
            LocalDateTime resolvedAt) {
    }

    /**
     * RealtimePipelineIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RealtimePipelineIncidentInput(
            Long tenantId,
            Long pipelineId,
            String pipelineKey,
            String sinkRef,
            String status,
            String message,
            String checkpointId,
            LocalDateTime checkpointAt,
            Long lagMs,
            List<String> reasons) {
    }

    /**
     * RealtimeJobIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RealtimeJobIncidentInput(
            Long tenantId,
            Long jobInstanceId,
            String pipelineKey,
            String jobKey,
            String engineType,
            String engineJobId,
            String deploymentRef,
            String runtimeStatus,
            String desiredStatus,
            String healthStatus,
            LocalDateTime lastHeartbeatAt,
            String ownerName,
            String lastErrorMessage,
            List<String> reasons) {
    }

    /**
     * ReadinessIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReadinessIncidentInput(
            Long tenantId,
            String sectionKey,
            String readinessStatus,
            String sectionStatus,
            String reason,
            LocalDateTime generatedAt) {
    }

    /**
     * TableDriftIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TableDriftIncidentInput(
            Long tenantId,
            Long inspectionId,
            String tableKey,
            String physicalName,
            String status,
            int checkedItems,
            int violationCount,
            List<String> violations,
            String message,
            String inspectionSource,
            LocalDateTime inspectedAt) {
    }

    /**
     * AvailabilityIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AvailabilityIncidentInput(
            Long tenantId,
            String mode,
            String decisionStatus,
            String gateKey,
            String gateStatus,
            String reason,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime availableUntil,
            Long lagMinutes,
            int evidenceCount,
            LocalDateTime generatedAt) {
    }

    /**
     * ConsumerAvailabilityIncidentInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ConsumerAvailabilityIncidentInput(
            Long tenantId,
            String contractKey,
            String consumerType,
            String consumerRef,
            String mode,
            String contractStatus,
            boolean allowed,
            String gatePolicy,
            String message,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime evaluatedAt,
            List<String> assetGateSummaries) {
        public ConsumerAvailabilityIncidentInput {
            assetGateSummaries = assetGateSummaries == null ? List.of() : List.copyOf(assetGateSummaries);
        }
    }
}
