package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO;

import java.time.LocalDateTime;

/**
 * CdpWarehousePrivacySubjectTombstoneMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectHash subject hash 参数，用于 recordBlocked 流程中的校验、计算或对象转换。
     * @param blockedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    int recordBlocked(@Param("tenantId") Long tenantId,
                      @Param("subjectType") String subjectType,
                      @Param("subjectHash") String subjectHash,
                      @Param("blockedAt") LocalDateTime blockedAt);
}
