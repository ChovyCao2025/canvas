package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_spreadsheet")
public class BiSpreadsheetDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String spreadsheetKey;

    private String name;

    private String description;

    private String sheetJson;

    private String dataBindingJson;

    private String styleJson;

    private String status;

    private Integer version;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
