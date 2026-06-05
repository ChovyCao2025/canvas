package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO;

import java.time.LocalDateTime;

@Mapper
public interface CdpWarehousePrivacySubjectTombstoneMapper
        extends BaseMapper<CdpWarehousePrivacySubjectTombstoneDO> {

    @Update("""
            UPDATE cdp_warehouse_privacy_subject_tombstone
            SET blocked_event_count = blocked_event_count + 1,
                last_blocked_at = #{blockedAt},
                updated_at = #{blockedAt}
            WHERE tenant_id = #{tenantId}
              AND subject_type = #{subjectType}
              AND subject_hash = #{subjectHash}
              AND status = 'ACTIVE'
            """)
    int recordBlocked(@Param("tenantId") Long tenantId,
                      @Param("subjectType") String subjectType,
                      @Param("subjectHash") String subjectHash,
                      @Param("blockedAt") LocalDateTime blockedAt);
}
