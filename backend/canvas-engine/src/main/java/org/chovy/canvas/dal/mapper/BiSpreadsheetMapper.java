package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;

@Mapper
public interface BiSpreadsheetMapper extends BaseMapper<BiSpreadsheetDO> {

    @Insert("""
            INSERT INTO bi_spreadsheet
                (tenant_id, workspace_id, spreadsheet_key, name, description, sheet_json,
                 data_binding_json, style_json, status, version, created_by)
            VALUES
                (#{row.tenantId}, #{row.workspaceId}, #{row.spreadsheetKey}, #{row.name},
                 #{row.description}, #{row.sheetJson}, #{row.dataBindingJson}, #{row.styleJson},
                 #{row.status}, #{row.version}, #{row.createdBy})
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                sheet_json = VALUES(sheet_json),
                data_binding_json = VALUES(data_binding_json),
                style_json = VALUES(style_json),
                status = VALUES(status),
                version = VALUES(version),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") BiSpreadsheetDO row);

    @Update("""
            UPDATE bi_spreadsheet
            SET status = 'PUBLISHED',
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND spreadsheet_key = #{spreadsheetKey}
            """)
    int publish(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("spreadsheetKey") String spreadsheetKey);

    @Update("""
            UPDATE bi_spreadsheet
            SET status = 'ARCHIVED',
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND workspace_id = #{workspaceId}
              AND spreadsheet_key = #{spreadsheetKey}
            """)
    int archive(@Param("tenantId") Long tenantId,
                @Param("workspaceId") Long workspaceId,
                @Param("spreadsheetKey") String spreadsheetKey);
}
