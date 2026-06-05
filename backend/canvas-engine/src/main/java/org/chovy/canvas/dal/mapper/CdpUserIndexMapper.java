package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpUserIndexDO;

@Mapper
public interface CdpUserIndexMapper extends BaseMapper<CdpUserIndexDO> {

    @Select("""
            SELECT id, tenant_id, user_id, user_index, created_at, updated_at
            FROM cdp_user_index
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    CdpUserIndexDO selectByTenantAndUser(@Param("tenantId") Long tenantId,
                                         @Param("userId") String userId);

    @Select("""
            SELECT COALESCE(MAX(user_index), 0) + 1
            FROM cdp_user_index
            WHERE tenant_id = #{tenantId}
            """)
    Long nextIndexForTenant(@Param("tenantId") Long tenantId);
}
