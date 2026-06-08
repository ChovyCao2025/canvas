package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;

/**
 * BiBigScreenMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiBigScreenMapper extends BaseMapper<BiBigScreenDO> {

    @Insert("""
            INSERT INTO bi_big_screen
                (tenant_id, workspace_id, screen_key, name, description, size_json,
                 background_json, layout_json, refresh_json, mobile_layout_json,
                 status, version, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.screenKey}, #{row.name},
                 #{row.description}, #{row.sizeJson}, #{row.backgroundJson}, #{row.layoutJson},
                 #{row.refreshJson}, #{row.mobileLayoutJson}, #{row.status}, #{row.version},
                 #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                size_json = VALUES(size_json),
                background_json = VALUES(background_json),
                layout_json = VALUES(layout_json),
                refresh_json = VALUES(refresh_json),
                mobile_layout_json = VALUES(mobile_layout_json),
                status = VALUES(status),
                version = VALUES(version),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") BiBigScreenDO row);

    @Update("""
            UPDATE bi_big_screen
            SET status = 'PUBLISHED',
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND screen_key = #{screenKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param screenKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("screenKey") String screenKey);

    @Update("""
            UPDATE bi_big_screen
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND screen_key = #{screenKey}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param screenKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 计算得到的数量、金额或指标值。
     */
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("screenKey") String screenKey);
}
