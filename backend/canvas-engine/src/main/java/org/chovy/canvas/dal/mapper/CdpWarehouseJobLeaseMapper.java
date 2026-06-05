package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO;

import java.time.LocalDateTime;

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
    int tryAcquire(@Param("row") CdpWarehouseJobLeaseDO row, @Param("now") LocalDateTime now);

    @Select("""
            SELECT id, tenant_id, lease_key, owner_id, lease_until, last_acquired_at, created_at, updated_at
            FROM cdp_warehouse_job_lease
            WHERE tenant_id = #{tenantId}
              AND lease_key = #{leaseKey}
            LIMIT 1
            """)
    CdpWarehouseJobLeaseDO findByKey(@Param("tenantId") Long tenantId, @Param("leaseKey") String leaseKey);

    @Update("""
            UPDATE cdp_warehouse_job_lease
            SET lease_until = #{releasedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND lease_key = #{leaseKey}
              AND owner_id = #{ownerId}
            """)
    int release(@Param("tenantId") Long tenantId,
                @Param("leaseKey") String leaseKey,
                @Param("ownerId") String ownerId,
                @Param("releasedAt") LocalDateTime releasedAt);
}
