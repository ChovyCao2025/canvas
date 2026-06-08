package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiPortalMenuDO;

/**
 * BiPortalMenuMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiPortalMenuMapper extends BaseMapper<BiPortalMenuDO> {

    @Delete("""
            DELETE FROM bi_portal_menu
            WHERE tenant_id = #{tenantId}
              AND portal_id = #{portalId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param portalId 业务对象 ID，用于定位具体记录。
     * @return 返回 delete by portal 计算得到的数量、金额或指标值。
     */
    int deleteByPortal(@Param("tenantId") Long tenantId, @Param("portalId") Long portalId);
}
