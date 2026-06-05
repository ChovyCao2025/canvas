package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiPortalMenuDO;

@Mapper
public interface BiPortalMenuMapper extends BaseMapper<BiPortalMenuDO> {

    @Delete("""
            DELETE FROM bi_portal_menu
            WHERE tenant_id = #{tenantId}
              AND portal_id = #{portalId}
            """)
    int deleteByPortal(@Param("tenantId") Long tenantId, @Param("portalId") Long portalId);
}
