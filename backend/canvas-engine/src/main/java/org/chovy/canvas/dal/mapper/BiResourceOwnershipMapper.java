package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiResourceOwnershipDO;

@Mapper
public interface BiResourceOwnershipMapper extends BaseMapper<BiResourceOwnershipDO> {

    @Insert("""
            INSERT INTO bi_resource_ownership
                (tenant_id, workspace_id, resource_type, resource_key, owner_user, transferred_by, transferred_at)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.resourceType}, #{row.resourceKey},
                 #{row.ownerUser}, #{row.transferredBy}, #{row.transferredAt})
            ON DUPLICATE KEY UPDATE
                owner_user = VALUES(owner_user),
                transferred_by = VALUES(transferred_by),
                transferred_at = VALUES(transferred_at)
            """)
    int upsert(@Param("row") BiResourceOwnershipDO row);
}
