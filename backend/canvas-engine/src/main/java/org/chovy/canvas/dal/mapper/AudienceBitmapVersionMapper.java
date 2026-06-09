package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.AudienceBitmapVersionDO;

/**
 * AudienceBitmapVersionMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface AudienceBitmapVersionMapper extends BaseMapper<AudienceBitmapVersionDO> {

    @Update("""
            UPDATE audience_bitmap_version
            SET status = 'READY',
                ready_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
              AND version = #{version}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 markReady 流程中的校验、计算或对象转换。
     * @return 返回 mark ready 计算得到的数量、金额或指标值。
     */
    int markReady(@Param("tenantId") Long tenantId,
                  @Param("audienceId") Long audienceId,
                  @Param("version") Long version);

    @Update("""
            UPDATE audience_bitmap_version
            SET status = 'ROLLED_BACK'
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
              AND status = 'READY'
              AND version > #{targetVersion}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param targetVersion target version 参数，用于 markReadyVersionsNewerThanRolledBack 流程中的校验、计算或对象转换。
     * @return 返回 mark ready versions newer than rolled back 计算得到的数量、金额或指标值。
     */
    int markReadyVersionsNewerThanRolledBack(@Param("tenantId") Long tenantId,
                                             @Param("audienceId") Long audienceId,
                                             @Param("targetVersion") Long targetVersion);

    @Select("""
            SELECT id, tenant_id, audience_id, version, bitmap_key, estimated_size,
                   bitmap_size_kb, source, status, created_by, created_at, ready_at
            FROM audience_bitmap_version
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
              AND version = #{version}
              AND status = 'READY'
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 selectReadyVersion 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    AudienceBitmapVersionDO selectReadyVersion(@Param("tenantId") Long tenantId,
                                               @Param("audienceId") Long audienceId,
                                               @Param("version") Long version);

    @Select("""
            SELECT id, tenant_id, audience_id, version, bitmap_key, estimated_size,
                   bitmap_size_kb, source, status, created_by, created_at, ready_at
            FROM audience_bitmap_version
            WHERE tenant_id = #{tenantId}
              AND audience_id = #{audienceId}
              AND status = 'READY'
            ORDER BY version DESC
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    AudienceBitmapVersionDO selectLatestReady(@Param("tenantId") Long tenantId,
                                              @Param("audienceId") Long audienceId);
}
