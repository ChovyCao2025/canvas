package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO;

import java.time.LocalDateTime;

/**
 * CdpWarehouseJobLeaseMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseJobLeaseMapper extends BaseMapper<CdpWarehouseJobLeaseDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_job_lease
            (tenant_id, lease_key, owner_id, lease_until, last_acquired_at)
            VALUES
            (#{row.tenantId}, #{row.leaseKey}, #{row.ownerId}, #{row.leaseUntil}, #{row.lastAcquiredAt})
            ON DUPLICATE KEY UPDATE
                owner_id = IF(lease_until <= #{now} OR owner_id = #{row.ownerId}, VALUES(owner_id), owner_id),
                lease_until = IF(lease_until <= #{now} OR owner_id = #{row.ownerId}, VALUES(lease_until), lease_until),
                last_acquired_at = IF(lease_until <= #{now} OR owner_id = #{row.ownerId}, VALUES(last_acquired_at), last_acquired_at),
                updated_at = IF(lease_until <= #{now} OR owner_id = #{row.ownerId}, CURRENT_TIMESTAMP, updated_at)
            """)
    /**
     * 执行 tryAcquire 流程，围绕 try acquire 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 try acquire 计算得到的数量、金额或指标值。
     */
    int tryAcquire(@Param("row") CdpWarehouseJobLeaseDO row, @Param("now") LocalDateTime now);

    @Select("""
            SELECT id, tenant_id, lease_key, owner_id, lease_until, last_acquired_at, created_at, updated_at
            FROM cdp_warehouse_job_lease
            WHERE tenant_id = #{tenantId}
              AND lease_key = #{leaseKey}
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param leaseKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    CdpWarehouseJobLeaseDO findByKey(@Param("tenantId") Long tenantId, @Param("leaseKey") String leaseKey);

    @Update("""
            UPDATE cdp_warehouse_job_lease
            SET lease_until = #{releasedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND lease_key = #{leaseKey}
              AND owner_id = #{ownerId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param leaseKey 业务键，用于在同一租户下定位资源。
     * @param ownerId 业务对象 ID，用于定位具体记录。
     * @param releasedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 release 计算得到的数量、金额或指标值。
     */
    int release(@Param("tenantId") Long tenantId,
                @Param("leaseKey") String leaseKey,
                @Param("ownerId") String ownerId,
                @Param("releasedAt") LocalDateTime releasedAt);
}
