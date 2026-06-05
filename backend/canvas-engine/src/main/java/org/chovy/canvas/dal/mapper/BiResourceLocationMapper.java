package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiResourceLocationDO;

@Mapper
public interface BiResourceLocationMapper extends BaseMapper<BiResourceLocationDO> {

    @Insert("""
            INSERT INTO bi_resource_location
                (tenant_id, workspace_id, resource_type, resource_key, folder_key, sort_order, moved_by, moved_at)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.resourceType}, #{row.resourceKey},
                 #{row.folderKey}, #{row.sortOrder}, #{row.movedBy}, #{row.movedAt})
            ON DUPLICATE KEY UPDATE
                folder_key = VALUES(folder_key),
                sort_order = VALUES(sort_order),
                moved_by = VALUES(moved_by),
                moved_at = VALUES(moved_at)
            """)
    int upsert(@Param("row") BiResourceLocationDO row);
}
