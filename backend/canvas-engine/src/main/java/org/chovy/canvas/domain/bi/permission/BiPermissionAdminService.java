package org.chovy.canvas.domain.bi.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * BiPermissionAdminService 编排 domain.bi.permission 场景的领域业务规则。
 */
@Service
public class BiPermissionAdminService {

    private static final String AUDIT_ACTION = "BI_PERMISSION_CHANGE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_PERMISSION";
    private static final String RESOURCE_DATASET = "DATASET";
    private static final String RESOURCE_DASHBOARD = "DASHBOARD";
    private static final String RESOURCE_CHART = "CHART";
    private static final String RESOURCE_PORTAL = "PORTAL";
    private static final String RESOURCE_SPREADSHEET = "SPREADSHEET";
    private static final String RESOURCE_BIG_SCREEN = "BIG_SCREEN";
    private static final String RESOURCE_DATASOURCE = "DATASOURCE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Set<String> RESOURCE_TYPES = Set.of(
            RESOURCE_DATASET,
            RESOURCE_DASHBOARD,
            RESOURCE_CHART,
            RESOURCE_PORTAL,
            "SELF_SERVICE",
            RESOURCE_SPREADSHEET,
            RESOURCE_BIG_SCREEN,
            RESOURCE_DATASOURCE);
    private static final Set<String> SUBJECT_TYPES = Set.of("USER", "ROLE", "ALL", "USER_GROUP", "GROUP");
    private static final Set<String> ACTION_KEYS = Set.of(
            "VIEW",
            "USE",
            "QUERY",
            "EDIT",
            "PUBLISH",
            "EXPORT",
            "EMBED",
            "SUBSCRIBE",
            "ALL",
            "*");
    private static final Set<String> EFFECTS = Set.of("ALLOW", "DENY");
    private static final Set<String> COLUMN_POLICIES = Set.of("ALLOW", "DENY", "MASK");

    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final BiResourcePermissionMapper resourcePermissionMapper;
    private final BiRowPermissionMapper rowPermissionMapper;
    private final BiColumnPermissionMapper columnPermissionMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 BiPermissionAdminService 实例并注入 domain.bi.permission 场景依赖。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param resourcePermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rowPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param columnPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPermissionAdminService(BiDatasetMapper datasetMapper,
                                    BiDashboardMapper dashboardMapper,
                                    BiChartMapper chartMapper,
                                    BiPortalMapper portalMapper,
                                    BiBigScreenMapper bigScreenMapper,
                                    BiSpreadsheetMapper spreadsheetMapper,
                                    BiResourcePermissionMapper resourcePermissionMapper,
                                    BiRowPermissionMapper rowPermissionMapper,
                                    BiColumnPermissionMapper columnPermissionMapper,
                                    ObjectMapper objectMapper) {
        this(datasetMapper, dashboardMapper, chartMapper, portalMapper, bigScreenMapper, spreadsheetMapper, null,
                resourcePermissionMapper, rowPermissionMapper, columnPermissionMapper, null, objectMapper);
    }

    /**
     * 创建 BiPermissionAdminService 实例并注入 domain.bi.permission 场景依赖。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param resourcePermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rowPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param columnPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiPermissionAdminService(BiDatasetMapper datasetMapper,
                                    BiDashboardMapper dashboardMapper,
                                    BiChartMapper chartMapper,
                                    BiPortalMapper portalMapper,
                                    BiBigScreenMapper bigScreenMapper,
                                    BiSpreadsheetMapper spreadsheetMapper,
                                    DataSourceConfigMapper dataSourceConfigMapper,
                                    BiResourcePermissionMapper resourcePermissionMapper,
                                    BiRowPermissionMapper rowPermissionMapper,
                                    BiColumnPermissionMapper columnPermissionMapper,
                                    BiAuditLogMapper auditLogMapper,
                                    ObjectMapper objectMapper) {
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.resourcePermissionMapper = resourcePermissionMapper;
        this.rowPermissionMapper = rowPermissionMapper;
        this.columnPermissionMapper = columnPermissionMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @param resourceId BI 资源数据库主键，用于精确匹配权限规则
     * @return 指定资源的资源级权限配置列表
     */
    public List<BiResourcePermissionView> listResourcePermissions(Long tenantId,
                                                                  String resourceType,
                                                                  String resourceKey,
                                                                  Long resourceId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ResourceRef ref = hasText(resourceType) && (hasText(resourceKey) || resourceId != null)
                /**
                 * 解析业务依赖或上下文值。
                 *
                 * @param scopedTenantId 业务对象 ID，用于定位具体记录。
                 * @param resourceType 类型标识，用于选择对应处理分支。
                 * @param resourceKey 业务键，用于在同一租户下定位资源。
                 * @param resourceId 业务对象 ID，用于定位具体记录。
                 * @return 返回 resolveResource 流程生成的业务结果。
                 */
                ? resolveResource(scopedTenantId, resourceType, resourceKey, resourceId)
                : null;
        LambdaQueryWrapper<BiResourcePermissionDO> query =
                new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiResourcePermissionDO::getResourceType)
                        .orderByAsc(BiResourcePermissionDO::getResourceId)
                        .orderByAsc(BiResourcePermissionDO::getSubjectType)
                        .orderByAsc(BiResourcePermissionDO::getSubjectId)
                        .orderByAsc(BiResourcePermissionDO::getActionKey);
        if (ref != null) {
            query.eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                    .eq(BiResourcePermissionDO::getResourceId, ref.resourceId());
        // 根据前序判断结果进入后续条件分支。
        } else if (hasText(resourceType)) {
            query.eq(BiResourcePermissionDO::getResourceType, upperRequired(resourceType, "resourceType"));
        }
        return safeList(resourcePermissionMapper.selectList(query)).stream()
                .map(this::toResourceView)
                .toList();
    }

    /**
     * 创建或更新资源级权限配置，并记录管理员操作审计。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiResourcePermissionView upsertResourcePermission(Long tenantId,
                                                             String username,
                                                             BiResourcePermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("resource permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ResourceRef ref = resolveResource(scopedTenantId,
                command.resourceType(),
                command.resourceKey(),
                command.resourceId());
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String actionKey = enumValue(command.actionKey(), ACTION_KEYS, "actionKey");
        String effect = enumValue(command.effect(), EFFECTS, "effect");
        BiResourcePermissionDO row = resourcePermissionMapper.selectOne(
                new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                        .eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                        .eq(BiResourcePermissionDO::getResourceId, ref.resourceId())
                        .eq(BiResourcePermissionDO::getSubjectType, subjectType)
                        .eq(BiResourcePermissionDO::getSubjectId, subjectId)
                        .eq(BiResourcePermissionDO::getActionKey, actionKey)
                        .last("LIMIT 1"));
        Map<String, Object> before = resourceSnapshot(row);
        String operation = row == null ? "CREATE" : "UPDATE";
        if (row == null) {
            row = new BiResourcePermissionDO();
            row.setTenantId(scopedTenantId);
            row.setWorkspaceId(ref.workspaceId());
            row.setResourceType(ref.resourceType());
            row.setResourceId(ref.resourceId());
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setActionKey(actionKey);
            row.setEffect(effect);
            row.setCreatedBy(defaultUser(username));
            resourcePermissionMapper.insert(row);
            row = resourcePermissionMapper.selectOne(new LambdaQueryWrapper<BiResourcePermissionDO>()
                    .eq(BiResourcePermissionDO::getTenantId, scopedTenantId)
                    .eq(BiResourcePermissionDO::getResourceType, ref.resourceType())
                    .eq(BiResourcePermissionDO::getResourceId, ref.resourceId())
                    .eq(BiResourcePermissionDO::getSubjectType, subjectType)
                    .eq(BiResourcePermissionDO::getSubjectId, subjectId)
                    .eq(BiResourcePermissionDO::getActionKey, actionKey)
                    .last("LIMIT 1"));
        } else {
            row.setEffect(effect);
            row.setCreatedBy(defaultUser(username));
            resourcePermissionMapper.updateById(row);
        }
        BiResourcePermissionView view = toResourceView(row);
        auditChange(scopedTenantId, row.getWorkspaceId(), row.getId(), username,
                /**
                 * 执行 resourceSnapshot 流程，围绕 resource snapshot 完成校验、计算或结果组装。
                 *
                 * @return 返回 resourceSnapshot 流程生成的业务结果。
                 */
                "RESOURCE", operation, before, resourceSnapshot(row));
        return view;
    }

    /**
     * 删除资源级权限配置，使对应主体不再获得该资源授权。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteResourcePermission(Long tenantId, Long id) {
        deleteResourcePermission(tenantId, "system", id);
    }

    /**
     * 删除资源级权限配置，使对应主体不再获得该资源授权。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteResourcePermission(Long tenantId, String username, Long id) {
        BiResourcePermissionDO row = resourcePermissionMapper.selectById(id);
        Map<String, Object> before = resourceSnapshot(row);
        Long workspaceId = row == null ? null : row.getWorkspaceId();
        deleteOwned(row, normalizeTenant(tenantId), id, () -> resourcePermissionMapper.deleteById(id));
        auditChange(normalizeTenant(tenantId), workspaceId, id, username,
                "RESOURCE", "DELETE", before, null);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @return 指定数据集的行级权限规则列表
     */
    public List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long datasetId = hasText(datasetKey) ? resolveDataset(scopedTenantId, datasetKey).getId() : null;
        LambdaQueryWrapper<BiRowPermissionDO> query =
                new LambdaQueryWrapper<BiRowPermissionDO>()
                        .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiRowPermissionDO::getDatasetId)
                        .orderByAsc(BiRowPermissionDO::getRuleKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasetId != null) {
            query.eq(BiRowPermissionDO::getDatasetId, datasetId);
        }
        Map<Long, String> datasetKeys = datasetKeys(scopedTenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(rowPermissionMapper.selectList(query)).stream()
                .map(row -> toRowView(row, datasetKeys.get(row.getDatasetId())))
                .toList();
    }

    /**
     * 创建或更新数据集行级权限规则，并写入权限治理审计。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiRowPermissionView upsertRowPermission(Long tenantId,
                                                   BiRowPermissionCommand command) {
        return upsertRowPermission(tenantId, "system", command);
    }

    /**
     * 创建或更新数据集行级权限规则，并写入权限治理审计。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiRowPermissionView upsertRowPermission(Long tenantId,
                                                   String username,
                                                   BiRowPermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("row permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.datasetKey());
        String ruleKey = required(command.ruleKey(), "ruleKey");
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String filterJson = rowFilterJson(command);
        Boolean enabled = command.enabled() == null ? true : command.enabled();
        BiRowPermissionDO row = rowPermissionMapper.selectOne(new LambdaQueryWrapper<BiRowPermissionDO>()
                .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                .eq(BiRowPermissionDO::getDatasetId, dataset.getId())
                .eq(BiRowPermissionDO::getRuleKey, ruleKey)
                .last("LIMIT 1"));
        Map<String, Object> before = rowSnapshot(row);
        String operation = row == null ? "CREATE" : "UPDATE";
        if (row == null) {
            row = new BiRowPermissionDO();
            row.setTenantId(scopedTenantId);
            row.setDatasetId(dataset.getId());
            row.setRuleKey(ruleKey);
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setFilterJson(filterJson);
            row.setEnabled(enabled);
            rowPermissionMapper.insert(row);
            row = rowPermissionMapper.selectOne(new LambdaQueryWrapper<BiRowPermissionDO>()
                    .eq(BiRowPermissionDO::getTenantId, scopedTenantId)
                    .eq(BiRowPermissionDO::getDatasetId, dataset.getId())
                    .eq(BiRowPermissionDO::getRuleKey, ruleKey)
                    .last("LIMIT 1"));
        } else {
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setFilterJson(filterJson);
            row.setEnabled(enabled);
            rowPermissionMapper.updateById(row);
        }
        BiRowPermissionView view = toRowView(row, dataset.getDatasetKey());
        auditChange(scopedTenantId, dataset.getWorkspaceId(), row.getId(), username,
                /**
                 * 组装输出结构或完成对象转换。
                 *
                 * @return 返回 rowSnapshot 流程生成的业务结果。
                 */
                "ROW", operation, before, rowSnapshot(row));
        return view;
    }

    /**
     * 删除数据集行级权限规则，停止在查询编译时追加对应过滤条件。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteRowPermission(Long tenantId, Long id) {
        deleteRowPermission(tenantId, "system", id);
    }

    /**
     * 删除数据集行级权限规则，停止在查询编译时追加对应过滤条件。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteRowPermission(Long tenantId, String username, Long id) {
        BiRowPermissionDO row = rowPermissionMapper.selectById(id);
        Map<String, Object> before = rowSnapshot(row);
        Long workspaceId = workspaceIdForDataset(row == null ? null : row.getDatasetId());
        deleteOwned(row, normalizeTenant(tenantId), id, () -> rowPermissionMapper.deleteById(id));
        auditChange(normalizeTenant(tenantId), workspaceId, id, username,
                "ROW", "DELETE", before, null);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param datasetKey 数据集业务键，用于定位语义模型、权限规则和加速策略
     * @return 指定数据集的列级权限和脱敏规则列表
     */
    public List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long datasetId = hasText(datasetKey) ? resolveDataset(scopedTenantId, datasetKey).getId() : null;
        LambdaQueryWrapper<BiColumnPermissionDO> query =
                new LambdaQueryWrapper<BiColumnPermissionDO>()
                        .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                        .orderByAsc(BiColumnPermissionDO::getDatasetId)
                        .orderByAsc(BiColumnPermissionDO::getFieldKey)
                        .orderByAsc(BiColumnPermissionDO::getSubjectType)
                        .orderByAsc(BiColumnPermissionDO::getSubjectId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasetId != null) {
            query.eq(BiColumnPermissionDO::getDatasetId, datasetId);
        }
        Map<Long, String> datasetKeys = datasetKeys(scopedTenantId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(columnPermissionMapper.selectList(query)).stream()
                .map(row -> toColumnView(row, datasetKeys.get(row.getDatasetId())))
                .toList();
    }

    /**
     * 创建或更新列级权限规则，控制字段裁剪和脱敏展示。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiColumnPermissionView upsertColumnPermission(Long tenantId,
                                                         BiColumnPermissionCommand command) {
        return upsertColumnPermission(tenantId, "system", command);
    }

    /**
     * 创建或更新列级权限规则，控制字段裁剪和脱敏展示。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiColumnPermissionView upsertColumnPermission(Long tenantId,
                                                         String username,
                                                         BiColumnPermissionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("column permission command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.datasetKey());
        String fieldKey = required(command.fieldKey(), "fieldKey");
        String subjectType = enumValue(command.subjectType(), SUBJECT_TYPES, "subjectType");
        String subjectId = required(command.subjectId(), "subjectId");
        String policy = enumValue(command.policy(), COLUMN_POLICIES, "policy");
        String maskJson = command.mask().isEmpty() ? null : json(command.mask());
        Boolean enabled = command.enabled() == null ? true : command.enabled();
        BiColumnPermissionDO row = columnPermissionMapper.selectOne(new LambdaQueryWrapper<BiColumnPermissionDO>()
                .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                .eq(BiColumnPermissionDO::getDatasetId, dataset.getId())
                .eq(BiColumnPermissionDO::getFieldKey, fieldKey)
                .eq(BiColumnPermissionDO::getSubjectType, subjectType)
                .eq(BiColumnPermissionDO::getSubjectId, subjectId)
                .last("LIMIT 1"));
        Map<String, Object> before = columnSnapshot(row);
        String operation = row == null ? "CREATE" : "UPDATE";
        if (row == null) {
            row = new BiColumnPermissionDO();
            row.setTenantId(scopedTenantId);
            row.setDatasetId(dataset.getId());
            row.setFieldKey(fieldKey);
            row.setSubjectType(subjectType);
            row.setSubjectId(subjectId);
            row.setPolicy(policy);
            row.setMaskJson(maskJson);
            row.setEnabled(enabled);
            columnPermissionMapper.insert(row);
            row = columnPermissionMapper.selectOne(new LambdaQueryWrapper<BiColumnPermissionDO>()
                    .eq(BiColumnPermissionDO::getTenantId, scopedTenantId)
                    .eq(BiColumnPermissionDO::getDatasetId, dataset.getId())
                    .eq(BiColumnPermissionDO::getFieldKey, fieldKey)
                    .eq(BiColumnPermissionDO::getSubjectType, subjectType)
                    .eq(BiColumnPermissionDO::getSubjectId, subjectId)
                    .last("LIMIT 1"));
        } else {
            row.setPolicy(policy);
            row.setMaskJson(maskJson);
            row.setEnabled(enabled);
            columnPermissionMapper.updateById(row);
        }
        BiColumnPermissionView view = toColumnView(row, dataset.getDatasetKey());
        auditChange(scopedTenantId, dataset.getWorkspaceId(), row.getId(), username,
                /**
                 * 执行 columnSnapshot 流程，围绕 column snapshot 完成校验、计算或结果组装。
                 *
                 * @return 返回 columnSnapshot 流程生成的业务结果。
                 */
                "COLUMN", operation, before, columnSnapshot(row));
        return view;
    }

    /**
     * 删除列级权限规则，使对应字段恢复默认权限策略。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteColumnPermission(Long tenantId, Long id) {
        deleteColumnPermission(tenantId, "system", id);
    }

    /**
     * 删除列级权限规则，使对应字段恢复默认权限策略。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param id 记录主键，用于删除或定位权限配置
     */
    public void deleteColumnPermission(Long tenantId, String username, Long id) {
        BiColumnPermissionDO row = columnPermissionMapper.selectById(id);
        Map<String, Object> before = columnSnapshot(row);
        Long workspaceId = workspaceIdForDataset(row == null ? null : row.getDatasetId());
        deleteOwned(row, normalizeTenant(tenantId), id, () -> columnPermissionMapper.deleteById(id));
        auditChange(normalizeTenant(tenantId), workspaceId, id, username,
                "COLUMN", "DELETE", before, null);
    }

    /**
     * 查询最近治理审计记录，用于追踪权限、策略和配置变更。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param limit 本次读取、处理或领取的最大数量
     * @return 符合条件的业务列表
     */
    public List<BiPermissionAuditEntry> recentAudit(Long tenantId, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (auditLogMapper == null) {
            return List.of();
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiAuditLogDO> rows = auditLogMapper.selectList(
                new LambdaQueryWrapper<BiAuditLogDO>()
                        .eq(BiAuditLogDO::getTenantId, scopedTenantId)
                        .eq(BiAuditLogDO::getActionKey, AUDIT_ACTION)
                        .eq(BiAuditLogDO::getResourceType, AUDIT_RESOURCE_TYPE)
                        .orderByDesc(BiAuditLogDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(rows).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> AUDIT_ACTION.equals(row.getActionKey()))
                .filter(row -> AUDIT_RESOURCE_TYPE.equals(row.getResourceType()))
                .sorted(Comparator.comparing(BiAuditLogDO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(boundedLimit)
                .map(row -> new BiPermissionAuditEntry(
                        row.getId(),
                        row.getActorId(),
                        row.getActionKey(),
                        row.getResourceType(),
                        row.getDetailJson(),
                        row.getCreatedAt()))
                .toList();
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 resolveResource 流程生成的业务结果。
     */
    private ResourceRef resolveResource(Long tenantId,
                                        String resourceType,
                                        String resourceKey,
                                        Long resourceId) {
        // 管理端写权限前先把业务 key 解析为数据库 ID 和 workspaceId，后续审计与运行态授权都以该快照为准。
        String scopedType = enumValue(resourceType, RESOURCE_TYPES, "resourceType");
        if (resourceId != null && resourceId > 0) {
            return new ResourceRef(scopedType, resourceId, workspaceId(tenantId, scopedType, resourceId), resourceKey);
        }
        if (!hasText(resourceKey)) {
            throw new IllegalArgumentException("resourceKey or resourceId is required");
        }
        if (RESOURCE_DATASET.equals(scopedType)) {
            BiDatasetDO dataset = resolveDataset(tenantId, resourceKey);
            return new ResourceRef(scopedType, dataset.getId(), dataset.getWorkspaceId(), dataset.getDatasetKey());
        }
        if (RESOURCE_DASHBOARD.equals(scopedType)) {
            BiDashboardDO dashboard = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .in(BiDashboardDO::getTenantId, tenantScope(tenantId))
                    .eq(BiDashboardDO::getDashboardKey, required(resourceKey, "resourceKey"))
                    .ne(BiDashboardDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiDashboardDO::getTenantId)
                    .last("LIMIT 1"));
            if (dashboard == null || dashboard.getId() == null) {
                throw new IllegalArgumentException("BI dashboard not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, dashboard.getId(), dashboard.getWorkspaceId(), dashboard.getDashboardKey());
        }
        if (RESOURCE_CHART.equals(scopedType)) {
            BiChartDO chart = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .in(BiChartDO::getTenantId, tenantScope(tenantId))
                    .eq(BiChartDO::getChartKey, required(resourceKey, "resourceKey"))
                    .ne(BiChartDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiChartDO::getTenantId)
                    .last("LIMIT 1"));
            if (chart == null || chart.getId() == null) {
                throw new IllegalArgumentException("BI chart not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, chart.getId(), chart.getWorkspaceId(), chart.getChartKey());
        }
        if (RESOURCE_PORTAL.equals(scopedType)) {
            BiPortalDO portal = portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                    .in(BiPortalDO::getTenantId, tenantScope(tenantId))
                    .eq(BiPortalDO::getPortalKey, required(resourceKey, "resourceKey"))
                    .ne(BiPortalDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiPortalDO::getTenantId)
                    .last("LIMIT 1"));
            if (portal == null || portal.getId() == null) {
                throw new IllegalArgumentException("BI portal not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, portal.getId(), portal.getWorkspaceId(), portal.getPortalKey());
        }
        if (RESOURCE_BIG_SCREEN.equals(scopedType)) {
            BiBigScreenDO screen = bigScreenMapper.selectOne(new LambdaQueryWrapper<BiBigScreenDO>()
                    .in(BiBigScreenDO::getTenantId, tenantScope(tenantId))
                    .eq(BiBigScreenDO::getScreenKey, required(resourceKey, "resourceKey"))
                    .ne(BiBigScreenDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiBigScreenDO::getTenantId)
                    .last("LIMIT 1"));
            if (screen == null || screen.getId() == null) {
                throw new IllegalArgumentException("BI big screen not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, screen.getId(), screen.getWorkspaceId(), screen.getScreenKey());
        }
        if (RESOURCE_SPREADSHEET.equals(scopedType)) {
            BiSpreadsheetDO spreadsheet = spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                    .in(BiSpreadsheetDO::getTenantId, tenantScope(tenantId))
                    .eq(BiSpreadsheetDO::getSpreadsheetKey, required(resourceKey, "resourceKey"))
                    .ne(BiSpreadsheetDO::getStatus, STATUS_ARCHIVED)
                    .orderByDesc(BiSpreadsheetDO::getTenantId)
                    .last("LIMIT 1"));
            if (spreadsheet == null || spreadsheet.getId() == null) {
                throw new IllegalArgumentException("BI spreadsheet not found: " + resourceKey);
            }
            return new ResourceRef(scopedType, spreadsheet.getId(), spreadsheet.getWorkspaceId(),
                    spreadsheet.getSpreadsheetKey());
        }
        if (RESOURCE_DATASOURCE.equals(scopedType)) {
            DataSourceConfigDO datasource = resolveDatasource(tenantId, resourceKey);
            return new ResourceRef(scopedType, datasource.getId(), 0L, datasourceKey(datasource));
        }
        throw new IllegalArgumentException("resourceId is required for resourceType: " + scopedType);
    }

    /**
     * 执行 workspaceId 流程，围绕 workspace id 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 workspace id 计算得到的数量、金额或指标值。
     */
    private Long workspaceId(Long tenantId, String resourceType, Long resourceId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (RESOURCE_DATASET.equals(resourceType)) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_DASHBOARD.equals(resourceType)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_CHART.equals(resourceType)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_PORTAL.equals(resourceType)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_BIG_SCREEN.equals(resourceType)) {
            BiBigScreenDO row = bigScreenMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_SPREADSHEET.equals(resourceType)) {
            BiSpreadsheetDO row = spreadsheetMapper.selectById(resourceId);
            return row == null || row.getWorkspaceId() == null ? 0L : row.getWorkspaceId();
        }
        if (RESOURCE_DATASOURCE.equals(resourceType)) {
            return 0L;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return 0L;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 resolveDataset 流程生成的业务结果。
     */
    private BiDatasetDO resolveDataset(Long tenantId, String datasetKey) {
        BiDatasetDO dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
        if (dataset == null || dataset.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        return dataset;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiResourcePermissionView toResourceView(BiResourcePermissionDO row) {
        if (row == null) {
            throw new IllegalStateException("BI resource permission was not persisted");
        }
        return new BiResourcePermissionView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                resourceKey(row.getResourceType(), row.getResourceId()),
                row.getResourceId(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getActionKey(),
                row.getEffect(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回组装或转换后的结果对象。
     */
    private BiRowPermissionView toRowView(BiRowPermissionDO row, String datasetKey) {
        if (row == null) {
            throw new IllegalStateException("BI row permission was not persisted");
        }
        return new BiRowPermissionView(
                row.getId(),
                row.getTenantId(),
                datasetKey,
                row.getDatasetId(),
                row.getRuleKey(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getFilterJson(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回组装或转换后的结果对象。
     */
    private BiColumnPermissionView toColumnView(BiColumnPermissionDO row, String datasetKey) {
        if (row == null) {
            throw new IllegalStateException("BI column permission was not persisted");
        }
        return new BiColumnPermissionView(
                row.getId(),
                row.getTenantId(),
                datasetKey,
                row.getDatasetId(),
                row.getFieldKey(),
                row.getSubjectType(),
                row.getSubjectId(),
                row.getPolicy(),
                row.getMaskJson(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getCreatedAt());
    }

    /**
     * 执行 datasetKeys 流程，围绕 dataset keys 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 dataset keys 生成的文本或业务键。
     */
    private Map<Long, String> datasetKeys(Long tenantId) {
        Map<Long, String> values = new LinkedHashMap<>();
        for (BiDatasetDO row : safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByAsc(BiDatasetDO::getDatasetKey)))) {
            values.put(row.getId(), row.getDatasetKey());
        }
        return values;
    }

    /**
     * 执行 resourceKey 流程，围绕 resource key 完成校验、计算或结果组装。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        // 列表视图需要把权限表中的 resourceId 还原为业务 key；查不到时返回 null，不放宽实际权限。
        if (RESOURCE_DATASET.equals(resourceType)) {
            BiDatasetDO row = datasetMapper.selectById(resourceId);
            return row == null ? null : row.getDatasetKey();
        }
        if (RESOURCE_DASHBOARD.equals(resourceType)) {
            BiDashboardDO row = dashboardMapper.selectById(resourceId);
            return row == null ? null : row.getDashboardKey();
        }
        if (RESOURCE_CHART.equals(resourceType)) {
            BiChartDO row = chartMapper.selectById(resourceId);
            return row == null ? null : row.getChartKey();
        }
        if (RESOURCE_PORTAL.equals(resourceType)) {
            BiPortalDO row = portalMapper.selectById(resourceId);
            return row == null ? null : row.getPortalKey();
        }
        if (RESOURCE_BIG_SCREEN.equals(resourceType)) {
            BiBigScreenDO row = bigScreenMapper.selectById(resourceId);
            return row == null ? null : row.getScreenKey();
        }
        if (RESOURCE_SPREADSHEET.equals(resourceType)) {
            BiSpreadsheetDO row = spreadsheetMapper.selectById(resourceId);
            return row == null ? null : row.getSpreadsheetKey();
        }
        if (RESOURCE_DATASOURCE.equals(resourceType)) {
            DataSourceConfigDO row = dataSourceConfigMapper == null ? null : dataSourceConfigMapper.selectById(resourceId);
            return row == null ? "jdbc-" + resourceId : datasourceKey(row);
        }
        return null;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回 resolveDatasource 流程生成的业务结果。
     */
    private DataSourceConfigDO resolveDatasource(Long tenantId, String resourceKey) {
        if (dataSourceConfigMapper == null) {
            throw new IllegalStateException("BI datasource permission resolver is not configured");
        }
        // 数据源权限只允许当前租户配置，且支持 jdbc-{id} 与名称两种管理入口。
        String key = required(resourceKey, "resourceKey");
        Long id = datasourceIdFromKey(key);
        LambdaQueryWrapper<DataSourceConfigDO> query = new LambdaQueryWrapper<DataSourceConfigDO>()
                .eq(DataSourceConfigDO::getTenantId, normalizeTenant(tenantId))
                .last("LIMIT 1");
        if (id != null) {
            query.eq(DataSourceConfigDO::getId, id);
        } else {
            query.eq(DataSourceConfigDO::getName, key);
        }
        DataSourceConfigDO datasource = dataSourceConfigMapper.selectOne(query);
        if (datasource == null || datasource.getId() == null) {
            throw new IllegalArgumentException("BI datasource not found: " + resourceKey);
        }
        return datasource;
    }

    /**
     * 执行 datasourceIdFromKey 流程，围绕 datasource id from key 完成校验、计算或结果组装。
     *
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回 datasource id from key 计算得到的数量、金额或指标值。
     */
    private Long datasourceIdFromKey(String resourceKey) {
        String normalized = resourceKey.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("jdbc-")) {
            return null;
        }
        try {
            return Long.parseLong(normalized.substring("jdbc-".length()));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 执行 datasourceKey 流程，围绕 datasource key 完成校验、计算或结果组装。
     *
     * @param datasource datasource 参数，用于 datasourceKey 流程中的校验、计算或对象转换。
     * @return 返回 datasource key 生成的文本或业务键。
     */
    private String datasourceKey(DataSourceConfigDO datasource) {
        return "jdbc-" + datasource.getId();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 row filter json 生成的文本或业务键。
     */
    private String rowFilterJson(BiRowPermissionCommand command) {
        // 行权限支持新版 filters 数组和旧版单 filter；统一序列化后由运行态解析为强制查询条件。
        if (!command.filters().isEmpty()) {
            return json(Map.of("filters", command.filters()));
        }
        if (!command.filter().isEmpty()) {
            return json(command.filter());
        }
        throw new IllegalArgumentException("row permission filter is required");
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param permissionId 业务对象 ID，用于定位具体记录。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param permissionKind permission kind 参数，用于 auditChange 流程中的校验、计算或对象转换。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param before before 参数，用于 auditChange 流程中的校验、计算或对象转换。
     * @param after after 参数，用于 auditChange 流程中的校验、计算或对象转换。
     */
    private void auditChange(Long tenantId,
                             Long workspaceId,
                             Long permissionId,
                             String actor,
                             String permissionKind,
                             String operation,
                             Map<String, Object> before,
                             Map<String, Object> after) {
        if (auditLogMapper == null) {
            return;
        }
        // 审计记录 before/after 快照用于追责和回放；写失败不回滚权限变更，避免审计故障阻塞治理配置。
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("permissionKind", permissionKind);
        detail.put("operation", operation);
        detail.put("before", before);
        detail.put("after", after);
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setWorkspaceId(workspaceId);
        row.setActorId(defaultUser(actor));
        row.setActionKey(AUDIT_ACTION);
        row.setResourceType(AUDIT_RESOURCE_TYPE);
        row.setResourceId(permissionId);
        row.setDetailJson(json(detail));
        row.setCreatedAt(LocalDateTime.now());
        try {
            auditLogMapper.insert(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Permission writes should still apply when audit storage is temporarily unavailable.
        }
    }

    /**
     * 执行 resourceSnapshot 流程，围绕 resource snapshot 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 resourceSnapshot 流程生成的业务结果。
     */
    private Map<String, Object> resourceSnapshot(BiResourcePermissionDO row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", row.getId());
        value.put("tenantId", row.getTenantId());
        value.put("workspaceId", row.getWorkspaceId());
        value.put("resourceType", row.getResourceType());
        value.put("resourceId", row.getResourceId());
        value.put("subjectType", row.getSubjectType());
        value.put("subjectId", row.getSubjectId());
        value.put("actionKey", row.getActionKey());
        value.put("effect", row.getEffect());
        return value;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 rowSnapshot 流程生成的业务结果。
     */
    private Map<String, Object> rowSnapshot(BiRowPermissionDO row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", row.getId());
        value.put("tenantId", row.getTenantId());
        value.put("datasetId", row.getDatasetId());
        value.put("ruleKey", row.getRuleKey());
        value.put("subjectType", row.getSubjectType());
        value.put("subjectId", row.getSubjectId());
        value.put("filterJson", row.getFilterJson());
        value.put("enabled", Boolean.TRUE.equals(row.getEnabled()));
        return value;
    }

    /**
     * 执行 columnSnapshot 流程，围绕 column snapshot 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 columnSnapshot 流程生成的业务结果。
     */
    private Map<String, Object> columnSnapshot(BiColumnPermissionDO row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", row.getId());
        value.put("tenantId", row.getTenantId());
        value.put("datasetId", row.getDatasetId());
        value.put("fieldKey", row.getFieldKey());
        value.put("subjectType", row.getSubjectType());
        value.put("subjectId", row.getSubjectId());
        value.put("policy", row.getPolicy());
        value.put("maskJson", row.getMaskJson());
        value.put("enabled", Boolean.TRUE.equals(row.getEnabled()));
        return value;
    }

    /**
     * 执行 workspaceIdForDataset 流程，围绕 workspace id for dataset 完成校验、计算或结果组装。
     *
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 workspace id for dataset 计算得到的数量、金额或指标值。
     */
    private Long workspaceIdForDataset(Long datasetId) {
        if (datasetId == null) {
            return null;
        }
        BiDatasetDO dataset = datasetMapper.selectById(datasetId);
        return dataset == null ? null : dataset.getWorkspaceId();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI permission payload", e);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param deleteAction delete action 参数，用于 deleteOwned 流程中的校验、计算或对象转换。
     */
    private void deleteOwned(Object row, Long tenantId, Long id, Runnable deleteAction) {
        if (row == null) {
            throw new IllegalArgumentException("BI permission not found: " + id);
        }
        Long rowTenantId;
        // 删除前只读取权限行自身租户，不信任调用方传入类型，避免跨租户删除或误删不同权限表记录。
        if (row instanceof BiResourcePermissionDO value) {
            rowTenantId = value.getTenantId();
        // 根据前序判断结果进入后续条件分支。
        } else if (row instanceof BiRowPermissionDO value) {
            rowTenantId = value.getTenantId();
        // 根据前序判断结果进入后续条件分支。
        } else if (row instanceof BiColumnPermissionDO value) {
            rowTenantId = value.getTenantId();
        } else {
            throw new IllegalArgumentException("unsupported BI permission row: " + id);
        }
        if (!normalizeTenant(rowTenantId).equals(tenantId)) {
            throw new IllegalArgumentException("BI permission does not belong to current tenant: " + id);
        }
        deleteAction.run();
    }

    /**
     * 执行 enumValue 流程，围绕 enum value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param allowed allowed 参数，用于 enumValue 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 enum value 生成的文本或业务键。
     */
    private String enumValue(String value, Set<String> allowed, String fieldName) {
        String normalized = upperRequired(value, fieldName);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("unsupported " + fieldName + ": " + value);
        }
        return normalized;
    }

    /**
     * 执行 tenantScope 流程，围绕 tenant scope 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return hasText(username) ? username.trim() : "system";
    }

    /**
     * 校验并获取必需参数、资源或权限。
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
     * 执行 upperRequired 流程，围绕 upper required 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * ResourceRef 数据记录。
     */
    private record ResourceRef(
            String resourceType,
            Long resourceId,
            Long workspaceId,
            String resourceKey) {
    }
}
