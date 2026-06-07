package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO;

@Mapper
public interface BiDashboardRuntimeStateMapper extends BaseMapper<BiDashboardRuntimeStateDO> {

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
    int upsert(BiDashboardRuntimeStateDO row);
}
