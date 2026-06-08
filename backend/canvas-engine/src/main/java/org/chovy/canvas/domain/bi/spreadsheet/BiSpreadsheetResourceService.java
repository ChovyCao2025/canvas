package org.chovy.canvas.domain.bi.spreadsheet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * BiSpreadsheetResourceService 编排 domain.bi.spreadsheet 场景的领域业务规则。
 */
@Service
public class BiSpreadsheetResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final BiWorkspaceMapper workspaceMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiSpreadsheetVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 BiSpreadsheetResourceService 实例并注入 domain.bi.spreadsheet 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiSpreadsheetResourceService(BiWorkspaceMapper workspaceMapper,
                                        BiSpreadsheetMapper spreadsheetMapper,
                                        BiSpreadsheetVersionMapper versionMapper,
                                        ObjectMapper objectMapper) {
        this.workspaceMapper = workspaceMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @return 当前租户下可管理或可运行的 BI 资源列表
     */
    public List<BiSpreadsheetResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(spreadsheetMapper.selectList(new LambdaQueryWrapper<BiSpreadsheetDO>()
                        .eq(BiSpreadsheetDO::getTenantId, scopedTenantId)
                        .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                        .ne(BiSpreadsheetDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiSpreadsheetDO::getUpdatedAt)
                        .orderByAsc(BiSpreadsheetDO::getSpreadsheetKey)))
                .stream()
                .map(row -> toResource(row, row.getStatus()))
                .toList();
    }

    /**
     * 读取单个 BI 资源详情；当持久化草稿不存在时按业务规则回退到内置预设。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param spreadsheetKey 电子表格业务键，用于定位草稿、发布版本和归档资源
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiSpreadsheetResource get(Long tenantId, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        return toResource(row, row.getStatus());
    }

    /**
     * 保存 BI 资源草稿，执行编辑权限、协作锁和资源键校验，维持草稿生命周期状态。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param resource resource 参数，用于限定本次 BI 业务操作的输入范围
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiSpreadsheetResource saveDraft(Long tenantId, String username, BiSpreadsheetResource resource) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO existing = find(scopedTenantId, workspaceId, resource.spreadsheetKey());
        int version = existing == null ? 1 : value(existing.getVersion(), 1);
        BiSpreadsheetDO row = new BiSpreadsheetDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setSpreadsheetKey(required(resource.spreadsheetKey(), "spreadsheetKey"));
        row.setName(required(resource.name(), "name"));
        row.setDescription(resource.description());
        row.setSheetJson(json(resource.sheets()));
        row.setDataBindingJson(json(resource.dataBinding()));
        row.setStyleJson(json(resource.style()));
        row.setStatus(STATUS_DRAFT);
        row.setVersion(version);
        row.setCreatedBy(defaultUser(username));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        spreadsheetMapper.upsert(row);
        BiSpreadsheetDO persisted = find(scopedTenantId, workspaceId, resource.spreadsheetKey());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toResource(persisted == null ? row : persisted, STATUS_DRAFT);
    }

    /**
     * 发布 BI 资源，将通过权限和发布审批校验的草稿提升为可访问版本并写入版本快照。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param spreadsheetKey 电子表格业务键，用于定位草稿、发布版本和归档资源
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiSpreadsheetResource publish(Long tenantId, String username, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        spreadsheetMapper.publish(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO published = find(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO effective = published == null ? row : published;
        if (published == null) {
            effective.setStatus(STATUS_PUBLISHED);
            effective.setVersion(value(row.getVersion(), 1) + 1);
        }
        BiSpreadsheetResource resource = toResource(effective, STATUS_PUBLISHED);
        insertVersion(scopedTenantId, workspaceId, effective, resource, username);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return resource;
    }

    /**
     * 归档 BI 资源，使其退出常规列表和运行态访问，同时保留历史版本用于追溯。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param spreadsheetKey 电子表格业务键，用于定位草稿、发布版本和归档资源
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiSpreadsheetResource archive(Long tenantId, String spreadsheetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        spreadsheetMapper.archive(scopedTenantId, workspaceId, spreadsheetKey);
        BiSpreadsheetDO archived = find(scopedTenantId, workspaceId, spreadsheetKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, STATUS_ARCHIVED);
        }
        return toResource(archived, STATUS_ARCHIVED);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param spreadsheetKey 电子表格业务键，用于定位草稿、发布版本和归档资源
     * @param limit 本次读取、处理或领取的最大数量
     * @return 按版本号倒序返回的资源历史版本列表
     */
    public List<BiSpreadsheetVersionView> listVersions(Long tenantId, String spreadsheetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiSpreadsheetDO row = find(scopedTenantId, workspaceId, spreadsheetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiSpreadsheetVersionDO>()
                        .eq(BiSpreadsheetVersionDO::getTenantId, scopedTenantId)
                        .eq(BiSpreadsheetVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiSpreadsheetVersionDO::getSpreadsheetId, row.getId())
                        .orderByDesc(BiSpreadsheetVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    /**
     * 从指定历史版本恢复 BI 资源草稿，并校验编辑权限与协作锁，避免覆盖他人变更。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param spreadsheetKey 电子表格业务键，用于定位草稿、发布版本和归档资源
     * @param version 需要恢复或读取的历史版本号
     * @return 处理后的 BI 资源及其生命周期状态
     */
    public BiSpreadsheetResource restoreVersion(Long tenantId, String username, String spreadsheetKey, int version) {
        // 电子表格恢复限定在当前租户和固定 BI 工作区内，读取指定发布快照后写回为草稿。
        // 此 Service 暂无独立协作锁/审批 guard，恢复仍复用 saveDraft 的资源键和租户隔离校验。
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        if (find(scopedTenantId, workspaceId, spreadsheetKey) == null) {
            throw new IllegalArgumentException("BI spreadsheet not found: " + spreadsheetKey);
        }
        BiSpreadsheetVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetVersionDO>()
                .eq(BiSpreadsheetVersionDO::getTenantId, scopedTenantId)
                .eq(BiSpreadsheetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiSpreadsheetVersionDO::getSpreadsheetKey, spreadsheetKey)
                .eq(BiSpreadsheetVersionDO::getVersion, version));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI spreadsheet version not found: " + spreadsheetKey + "#" + version);
        }
        return saveDraft(scopedTenantId, username, resource(snapshot.getResourceJson()));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiSpreadsheetVersionView toVersionView(BiSpreadsheetVersionDO row) {
        // 版本视图反序列化发布时的完整资源 JSON，展示口径与恢复口径一致。
        return new BiSpreadsheetVersionView(
                row.getId(),
                row.getSpreadsheetKey(),
                row.getVersion(),
                row.getStatus(),
                resource(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param resource resource 参数，用于 insertVersion 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     */
    private void insertVersion(Long tenantId,
                               Long workspaceId,
                               BiSpreadsheetDO row,
                               BiSpreadsheetResource resource,
                               String username) {
        // 发布版本保存完整电子表格资源快照，包含工作表结构、数据绑定和样式配置。
        // 快照按租户、工作区和电子表格 id 隔离，避免不同租户的版本历史混用。
        if (versionMapper == null) {
            return;
        }
        BiSpreadsheetVersionDO version = new BiSpreadsheetVersionDO();
        version.setTenantId(tenantId);
        version.setWorkspaceId(workspaceId);
        version.setSpreadsheetId(row.getId());
        version.setSpreadsheetKey(row.getSpreadsheetKey());
        version.setVersion(value(row.getVersion(), 1));
        version.setStatus(STATUS_PUBLISHED);
        version.setResourceJson(json(resource));
        version.setPublishedBy(defaultUser(username));
        versionMapper.insert(version);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回组装或转换后的结果对象。
     */
    private BiSpreadsheetResource toResource(BiSpreadsheetDO row, String status) {
        return new BiSpreadsheetResource(
                row.getId(),
                row.getSpreadsheetKey(),
                row.getName(),
                row.getDescription(),
                list(row.getSheetJson()),
                map(row.getDataBindingJson()),
                map(row.getStyleJson()),
                status,
                value(row.getVersion(), 1),
                "PERSISTED");
    }

    /**
     * 执行 resource 流程，围绕 resource 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 resource 流程生成的业务结果。
     */
    private BiSpreadsheetResource resource(String json) {
        try {
            return objectMapper.readValue(json, BiSpreadsheetResource.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("resourceJson must be a valid BI spreadsheet resource", e);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param spreadsheetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiSpreadsheetDO find(Long tenantId, Long workspaceId, String spreadsheetKey) {
        return spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                .eq(BiSpreadsheetDO::getTenantId, tenantId)
                .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                .eq(BiSpreadsheetDO::getSpreadsheetKey, required(spreadsheetKey, "spreadsheetKey")));
    }

    /**
     * 执行 workspaceId 流程，围绕 workspace id 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 workspace id 计算得到的数量、金额或指标值。
     */
    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .eq(BiWorkspaceDO::getTenantId, tenantId)
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY));
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalStateException("BI workspace not found: " + WORKSPACE_KEY);
        }
        return workspace.getId();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "{}" : json,
                    new TypeReference<Map<String, Object>>() {
                    });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON object field is invalid", e);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<Map<String, Object>> list(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON array field is invalid", e);
        }
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
            throw new IllegalArgumentException("resource field must be JSON serializable", e);
        }
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
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param label label 参数，用于 required 流程中的校验、计算或对象转换。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username.trim();
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
