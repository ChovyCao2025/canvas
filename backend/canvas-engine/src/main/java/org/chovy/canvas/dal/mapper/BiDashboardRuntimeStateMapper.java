package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO;

/**
 * BiDashboardRuntimeStateMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiDashboardRuntimeStateMapper extends BaseMapper<BiDashboardRuntimeStateDO> {

            /**
             * 执行 bi_dashboard_runtime_state 流程，围绕 bi dashboard runtime state 完成校验、计算或结果组装。
             *
             * @param tenant_id 业务对象 ID，用于定位具体记录。
             * @param workspace_id 业务对象 ID，用于定位具体记录。
             * @param dashboard_key 业务键，用于在同一租户下定位资源。
             * @param username 操作人标识，用于审计和权限判断。
             * @param parameter_json JSON 字符串，承载结构化配置或明细。
             * @param created_at 时间参数，用于计算窗口、过期或审计时间。
             * @param row 持久化行数据，承载数据库记录内容。
             * @return 返回 bi_dashboard_runtime_state 流程生成的业务结果。
             */
    @Insert("""
            INSERT INTO bi_dashboard_runtime_state (
                tenant_id, workspace_id, dashboard_key, username, parameter_json, created_at, updated_at
            ) VALUES (
                #{tenantId}, #{workspaceId}, #{dashboardKey}, #{username}, #{parameterJson}, NOW(), #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                parameter_json = VALUES(parameter_json),
                updated_at = VALUES(updated_at)
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(BiDashboardRuntimeStateDO row);
}
