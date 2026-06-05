package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.AudienceBitmapVersionDO;

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
    AudienceBitmapVersionDO selectLatestReady(@Param("tenantId") Long tenantId,
                                              @Param("audienceId") Long audienceId);
}
