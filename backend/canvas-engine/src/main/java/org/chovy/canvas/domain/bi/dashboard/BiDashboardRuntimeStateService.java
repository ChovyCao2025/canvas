package org.chovy.canvas.domain.bi.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDashboardRuntimeStateMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * BiDashboardRuntimeStateService 编排 domain.bi.dashboard 场景的领域业务规则。
 */
@Service
public class BiDashboardRuntimeStateService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final TypeReference<Map<String, Object>> PARAMETER_MAP = new TypeReference<>() {
    };

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDashboardRuntimeStateMapper stateMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 BiDashboardRuntimeStateService 实例并注入 domain.bi.dashboard 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param stateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardRuntimeStateService(BiWorkspaceMapper workspaceMapper,
                                          BiDashboardRuntimeStateMapper stateMapper,
                                          ObjectMapper objectMapper) {
        this.workspaceMapper = workspaceMapper;
        this.stateMapper = stateMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取单个 BI 资源详情；当持久化草稿不存在时按业务规则回退到内置预设。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param dashboardKey 仪表板业务键，用于定位草稿、发布版本和运行态资源
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiDashboardRuntimeStateView get(Long tenantId, String username, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String user = defaultUser(username);
        String key = resourceKey(dashboardKey, "dashboardKey");
        BiDashboardRuntimeStateDO row = stateMapper.selectOne(new LambdaQueryWrapper<BiDashboardRuntimeStateDO>()
                .eq(BiDashboardRuntimeStateDO::getTenantId, scopedTenantId)
                .eq(BiDashboardRuntimeStateDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardRuntimeStateDO::getDashboardKey, key)
                .eq(BiDashboardRuntimeStateDO::getUsername, user)
                .last("LIMIT 1"));
        if (row == null) {
            return new BiDashboardRuntimeStateView(key, user, Map.of(), null);
        }
        return toView(row);
    }

    /**
     * 仪表板资源生命周期方法，围绕草稿、发布、版本和导入导出执行。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param dashboardKey 仪表板业务键，用于定位草稿、发布版本和运行态资源
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiDashboardRuntimeStateView save(Long tenantId,
                                            String username,
                                            String dashboardKey,
                                            BiDashboardRuntimeStateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("dashboard runtime state command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String user = defaultUser(username);
        String key = resourceKey(dashboardKey, "dashboardKey");
        LocalDateTime now = LocalDateTime.now();
        BiDashboardRuntimeStateDO row = new BiDashboardRuntimeStateDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setDashboardKey(key);
        row.setUsername(user);
        row.setParameterJson(json(command.parameters()));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        stateMapper.upsert(row);
        return new BiDashboardRuntimeStateView(key, user, command.parameters(), now);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDashboardRuntimeStateView toView(BiDashboardRuntimeStateDO row) {
        return new BiDashboardRuntimeStateView(
                row.getDashboardKey(),
                row.getUsername(),
                parameters(row.getParameterJson()),
                row.getUpdatedAt());
    }

    /**
     * 执行 parameters 流程，围绕 parameters 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 parameters 流程生成的业务结果。
     */
    private Map<String, Object> parameters(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, PARAMETER_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to parse BI dashboard runtime parameters", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param String string 参数，用于 json 流程中的校验、计算或对象转换。
     * @param parameters parameters 参数，用于 json 流程中的校验、计算或对象转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize BI dashboard runtime parameters", e);
        }
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
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY)
                .last("LIMIT 1"));
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalStateException("BI workspace not found: " + WORKSPACE_KEY);
        }
        return workspace.getId();
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
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 执行 resourceKey 流程，围绕 resource key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(String value, String fieldName) {
        if (value == null || value.isBlank() || !RESOURCE_KEY.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must match " + RESOURCE_KEY.pattern());
        }
        return value;
    }
}
