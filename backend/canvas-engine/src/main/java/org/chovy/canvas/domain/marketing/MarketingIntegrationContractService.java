package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractAuditEventDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractAuditEventMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingIntegrationContractService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class MarketingIntegrationContractService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractAuditEventMapper auditMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MarketingIntegrationContractService 实例并注入 domain.marketing 场景依赖。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingIntegrationContractService(MarketingIntegrationContractMapper contractMapper,
                                               MarketingIntegrationContractAuditEventMapper auditMapper,
                                               ObjectMapper objectMapper) {
        this.contractMapper = contractMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 创建或更新表治理契约，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractView upsertContract(
            Long tenantId,
            MarketingIntegrationContractCommand command,
            String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("integration contract command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String contractKey = normalizeKey(command.contractKey(), "contractKey");
        MarketingIntegrationContractDO row =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                contractMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getContractKey, contractKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        String previousStatus = insert ? null : row.getStatus();
        if (insert) {
            row = new MarketingIntegrationContractDO();
            row.setTenantId(scopedTenantId);
            row.setContractKey(contractKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setDisplayName(defaultString(command.displayName(), contractKey));
        row.setProviderFamily(normalizeUpper(required(command.providerFamily(), "providerFamily"), "providerFamily"));
        row.setSourceCapabilityKey(normalizeKey(command.sourceCapabilityKey(), "sourceCapabilityKey"));
        row.setTargetCapabilityKey(normalizeKey(command.targetCapabilityKey(), "targetCapabilityKey"));
        row.setAssetKey(normalizeKey(command.assetKey(), "assetKey"));
        row.setDirection(normalizeDirection(command.direction()));
        row.setEnvironment(normalizeEnvironment(command.environment()));
        row.setAuthMode(normalizeAuthMode(command.authMode()));
        row.setCredentialDependency(trimToLimit(command.credentialDependency(), 255));
        row.setApiRoot(required(command.apiRoot(), "apiRoot"));
        row.setOwnerTeam(trimToLimit(command.ownerTeam(), 128));
        row.setStatus(normalizeStatus(command.status()));
        row.setSlaTier(normalizeUpper(command.slaTier(), "STANDARD"));
        row.setTimeoutMs(normalizeTimeout(command.timeoutMs()));
        row.setRetryPolicyJson(toJson(command.retryPolicy()));
        row.setSchemaContractJson(toJson(command.schemaContract()));
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            contractMapper.insert(row);
        } else {
            contractMapper.updateById(row);
        }
        writeAudit(row, insert ? "CREATED" : "UPDATED", previousStatus, row.getStatus(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param providerFamily provider family 参数，用于 listContracts 流程中的校验、计算或对象转换。
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractView> listContracts(
            Long tenantId,
            String status,
            String providerFamily,
            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedProvider = normalizeOptionalUpper(providerFamily);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return contractMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, MarketingIntegrationContractDO::getStatus, normalizedStatus)
                        .eq(normalizedProvider != null,
                                MarketingIntegrationContractDO::getProviderFamily,
                                normalizedProvider)
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedProvider == null || normalizedProvider.equals(row.getProviderFamily()))
                .map(this::toView)
                .toList();
    }

    /**
     * 执行业务操作 archiveContract，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractView archiveContract(Long tenantId, Long contractId, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = safeTenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingIntegrationContractDO row = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(scopedTenantId, row == null ? null : row.getTenantId(), "integration contract");
        String previousStatus = row.getStatus();
        row.setStatus("ARCHIVED");
        row.setUpdatedBy(defaultString(actor, "system"));
        contractMapper.updateById(row);
        writeAudit(row, "ARCHIVED", previousStatus, row.getStatus(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractAuditEventView> listAuditEvents(
            Long tenantId,
            Long contractId,
            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO row = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(scopedTenantId, row == null ? null : row.getTenantId(), "integration contract");
        return auditMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractAuditEventDO>()
                        .eq(MarketingIntegrationContractAuditEventDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractAuditEventDO::getContractId, row.getId())
                        .orderByDesc(MarketingIntegrationContractAuditEventDO::getRevision)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .map(this::toAuditView)
                .toList();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param previousStatus 业务状态，用于筛选或推进状态流转。
     * @param newStatus 业务状态，用于筛选或推进状态流转。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void writeAudit(MarketingIntegrationContractDO row,
                            String eventType,
                            String previousStatus,
                            String newStatus,
                            String actor) {
        MarketingIntegrationContractAuditEventDO audit = new MarketingIntegrationContractAuditEventDO();
        audit.setTenantId(row.getTenantId());
        audit.setContractId(row.getId());
        audit.setContractKey(row.getContractKey());
        audit.setRevision(nextRevision(row.getTenantId(), row.getId()));
        audit.setEventType(eventType);
        audit.setPreviousStatus(previousStatus);
        audit.setNewStatus(newStatus);
        audit.setSnapshotJson(toJson(snapshot(row)));
        audit.setChangedFieldsJson(toJson(changedFields(previousStatus, newStatus, eventType)));
        audit.setChangedBy(defaultString(actor, "system"));
        auditMapper.insert(audit);
    }

    /**
     * 执行 nextRevision 流程，围绕 next revision 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractId 业务对象 ID，用于定位具体记录。
     * @return 返回 next revision 计算得到的数量、金额或指标值。
     */
    private Integer nextRevision(Long tenantId, Long contractId) {
        MarketingIntegrationContractAuditEventDO latest =
                auditMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractAuditEventDO>()
                        .eq(MarketingIntegrationContractAuditEventDO::getTenantId, tenantId)
                        .eq(MarketingIntegrationContractAuditEventDO::getContractId, contractId)
                        .orderByDesc(MarketingIntegrationContractAuditEventDO::getRevision)
                        .last("LIMIT 1"));
        return latest == null || latest.getRevision() == null ? 1 : latest.getRevision() + 1;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    private Map<String, Object> snapshot(MarketingIntegrationContractDO row) {
        return Map.ofEntries(
                Map.entry("contractKey", row.getContractKey()),
                Map.entry("displayName", row.getDisplayName()),
                Map.entry("providerFamily", row.getProviderFamily()),
                Map.entry("sourceCapabilityKey", row.getSourceCapabilityKey()),
                Map.entry("targetCapabilityKey", row.getTargetCapabilityKey()),
                Map.entry("assetKey", row.getAssetKey()),
                Map.entry("direction", row.getDirection()),
                Map.entry("environment", row.getEnvironment()),
                Map.entry("authMode", row.getAuthMode()),
                Map.entry("apiRoot", row.getApiRoot()),
                Map.entry("status", row.getStatus()),
                Map.entry("slaTier", row.getSlaTier()),
                Map.entry("timeoutMs", row.getTimeoutMs()));
    }

    /**
     * 执行 changedFields 流程，围绕 changed fields 完成校验、计算或结果组装。
     *
     * @param previousStatus 业务状态，用于筛选或推进状态流转。
     * @param newStatus 业务状态，用于筛选或推进状态流转。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @return 返回 changedFields 流程生成的业务结果。
     */
    private Map<String, Object> changedFields(String previousStatus, String newStatus, String eventType) {
        if (previousStatus != null && newStatus != null && !previousStatus.equals(newStatus)) {
            return Map.of("changedFields", List.of("status"));
        }
        return Map.of("changedFields", List.of(eventType.toLowerCase(Locale.ROOT)));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingIntegrationContractView toView(MarketingIntegrationContractDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingIntegrationContractView(
                row.getId(),
                row.getTenantId(),
                row.getContractKey(),
                row.getDisplayName(),
                row.getProviderFamily(),
                row.getSourceCapabilityKey(),
                row.getTargetCapabilityKey(),
                row.getAssetKey(),
                row.getDirection(),
                row.getEnvironment(),
                row.getAuthMode(),
                row.getCredentialDependency(),
                row.getApiRoot(),
                row.getOwnerTeam(),
                row.getStatus(),
                row.getSlaTier(),
                row.getTimeoutMs(),
                fromJson(row.getRetryPolicyJson()),
                fromJson(row.getSchemaContractJson()),
                fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingIntegrationContractAuditEventView toAuditView(MarketingIntegrationContractAuditEventDO row) {
        return new MarketingIntegrationContractAuditEventView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getRevision(),
                row.getEventType(),
                row.getPreviousStatus(),
                row.getNewStatus(),
                fromJson(row.getSnapshotJson()),
                fromJson(row.getChangedFieldsJson()),
                row.getChangedBy(),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("integration contract metadata must be JSON serializable", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String value, String field) {
        String normalized = required(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "DEGRADED", "BLOCKED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported integration contract status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeDirection(String value) {
        String direction = normalizeUpper(value, "OUTBOUND");
        return switch (direction) {
            case "INBOUND", "OUTBOUND", "BIDIRECTIONAL", "INTERNAL" -> direction;
            default -> throw new IllegalArgumentException("unsupported integration direction: " + direction);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeEnvironment(String value) {
        String environment = normalizeUpper(value, "PRODUCTION");
        return switch (environment) {
            case "PRODUCTION", "STAGING", "SANDBOX" -> environment;
            default -> throw new IllegalArgumentException("unsupported integration environment: " + environment);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeAuthMode(String value) {
        String authMode = normalizeUpper(value, "OAUTH");
        return switch (authMode) {
            case "OAUTH", "API_KEY", "HMAC", "INTERNAL", "NONE" -> authMode;
            default -> throw new IllegalArgumentException("unsupported integration auth mode: " + authMode);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 30000;
        }
        if (timeoutMs < 1000 || timeoutMs > 300000) {
            throw new IllegalArgumentException("timeoutMs must be between 1000 and 300000");
        }
        return timeoutMs;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    /**
     * 规范化输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     * @param entity entity 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
