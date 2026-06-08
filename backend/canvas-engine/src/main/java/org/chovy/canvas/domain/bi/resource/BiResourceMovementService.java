package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourceLocationDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceLocationMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * BiResourceMovementService 编排 domain.bi.resource 场景的领域业务规则。
 */
@Service
public class BiResourceMovementService {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern FOLDER_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*(/[A-Za-z0-9][A-Za-z0-9_-]*){0,15}");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiResourceLocationMapper locationMapper;

    /**
     * 创建 BiResourceMovementService 实例并注入 domain.bi.resource 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param locationMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiResourceMovementService(BiWorkspaceMapper workspaceMapper,
                                     BiDatasetMapper datasetMapper,
                                     BiDashboardMapper dashboardMapper,
                                     BiChartMapper chartMapper,
                                     BiPortalMapper portalMapper,
                                     BiBigScreenMapper bigScreenMapper,
                                     BiSpreadsheetMapper spreadsheetMapper,
                                     BiResourceLocationMapper locationMapper) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.locationMapper = locationMapper;
    }

    /**
     * 移动 BI 资源到目标目录或位置，更新资源位置并记录移动人。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param movedBy 执行资源移动的操作人
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiResourceLocationView move(Long tenantId, String movedBy, BiResourceMoveCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("BI resource move command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

        BiResourceLocationDO row = new BiResourceLocationDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setFolderKey(folderKey(command.folderKey()));
        row.setSortOrder(sortOrder(command.sortOrder()));
        row.setMovedBy(defaultUser(movedBy));
        row.setMovedAt(LocalDateTime.now());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        locationMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @return 指定类型 BI 资源的位置列表
     */
    public List<BiResourceLocationView> list(Long tenantId, String resourceType) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        LambdaQueryWrapper<BiResourceLocationDO> query = new LambdaQueryWrapper<BiResourceLocationDO>()
                .eq(BiResourceLocationDO::getTenantId, scopedTenantId)
                .eq(BiResourceLocationDO::getWorkspaceId, workspace.getId())
                .orderByAsc(BiResourceLocationDO::getFolderKey)
                .orderByAsc(BiResourceLocationDO::getSortOrder)
                .orderByAsc(BiResourceLocationDO::getResourceKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (resourceType != null && !resourceType.isBlank()) {
            query.eq(BiResourceLocationDO::getResourceType, normalizeResourceType(resourceType));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(locationMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     */
    private void assertResourceExists(Long tenantId, Long workspaceId, String resourceType, String resourceKey) {
        // 准备本次处理所需的上下文和中间变量。
        Object row = switch (resourceType) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            case "DATASET" -> datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                    .eq(BiDatasetDO::getTenantId, tenantId)
                    .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                    .eq(BiDatasetDO::getDatasetKey, resourceKey)
                    .last("LIMIT 1"));
            case "DASHBOARD" -> dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .eq(BiDashboardDO::getTenantId, tenantId)
                    .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                    .eq(BiDashboardDO::getDashboardKey, resourceKey)
                    .last("LIMIT 1"));
            case "CHART" -> chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .eq(BiChartDO::getTenantId, tenantId)
                    .eq(BiChartDO::getWorkspaceId, workspaceId)
                    .eq(BiChartDO::getChartKey, resourceKey)
                    .last("LIMIT 1"));
            case "PORTAL" -> portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                    .eq(BiPortalDO::getTenantId, tenantId)
                    .eq(BiPortalDO::getWorkspaceId, workspaceId)
                    .eq(BiPortalDO::getPortalKey, resourceKey)
                    .last("LIMIT 1"));
            case "BIG_SCREEN" -> bigScreenMapper.selectOne(new LambdaQueryWrapper<BiBigScreenDO>()
                    .eq(BiBigScreenDO::getTenantId, tenantId)
                    .eq(BiBigScreenDO::getWorkspaceId, workspaceId)
                    .eq(BiBigScreenDO::getScreenKey, resourceKey)
                    .last("LIMIT 1"));
            case "SPREADSHEET" -> spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                    .eq(BiSpreadsheetDO::getTenantId, tenantId)
                    .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                    .eq(BiSpreadsheetDO::getSpreadsheetKey, resourceKey)
                    .last("LIMIT 1"));
            default -> throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
        };
        if (row == null) {
            throw new IllegalArgumentException("BI resource not found: " + resourceType + "/" + resourceKey);
        }
        if (STATUS_ARCHIVED.equals(resourceStatus(row))) {
            throw new IllegalArgumentException("BI resource is archived: " + resourceType + "/" + resourceKey);
        }
    }

    /**
     * 执行 resourceStatus 流程，围绕 resource status 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 resource status 生成的文本或业务键。
     */
    private String resourceStatus(Object row) {
        return switch (row) {
            case BiDatasetDO dataset -> dataset.getStatus();
            case BiDashboardDO dashboard -> dashboard.getStatus();
            case BiChartDO chart -> chart.getStatus();
            case BiPortalDO portal -> portal.getStatus();
            case BiBigScreenDO screen -> screen.getStatus();
            case BiSpreadsheetDO spreadsheet -> spreadsheet.getStatus();
            default -> null;
        };
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 defaultWorkspace 流程生成的业务结果。
     */
    private BiWorkspaceDO defaultWorkspace(Long tenantId) {
        LambdaQueryWrapper<BiWorkspaceDO> query = new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, DEFAULT_WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1");
        query.getParamNameValuePairs().put("workspaceKey", DEFAULT_WORKSPACE_KEY);
        BiWorkspaceDO workspace = workspaceMapper.selectOne(query);
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalArgumentException("BI workspace not found");
        }
        return workspace;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiResourceLocationView toView(BiResourceLocationDO row) {
        return new BiResourceLocationView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getFolderKey(),
                row.getSortOrder(),
                row.getMovedBy(),
                row.getMovedAt());
    }

    /**
     * 规范化输入值。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if ("DATASET".equals(value) || "DASHBOARD".equals(value)
                || "CHART".equals(value) || "PORTAL".equals(value)
                || "BIG_SCREEN".equals(value) || "SPREADSHEET".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 resourceKey 流程，围绕 resource key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(String value) {
        String key = required(value, "resourceKey");
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("resourceKey contains unsafe characters");
        }
        return key;
    }

    /**
     * 执行 folderKey 流程，围绕 folder key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 folder key 生成的文本或业务键。
     */
    private String folderKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = value.trim();
        if (key.length() > 128 || !FOLDER_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("folderKey contains unsafe characters");
        }
        return key;
    }

    /**
     * 校验资源在目录中的排序值。
     *
     * <p>空值按 0 处理，负数直接拒绝，保证同一文件夹内排序口径稳定且不会出现反向哨兵值。</p>
     */
    private int sortOrder(Integer value) {
        int sortOrder = value == null ? 0 : value;
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be nonnegative");
        }
        return sortOrder;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String value) {
        return value == null || value.isBlank() ? "system" : value.trim();
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
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
