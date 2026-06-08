package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiSpreadsheetVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_spreadsheet_version")
public class BiSpreadsheetVersionDO {

    /** BI电子表格版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的电子表格 ID */
    private Long spreadsheetId;

    /** BI电子表格版本电子表格业务键 */
    private String spreadsheetKey;

    /** BI电子表格版本版本号 */
    private Integer version;

    /** BI电子表格版本当前状态 */
    private String status;

    /** BI电子表格版本资源内容 JSON */
    private String resourceJson;

    /** BI电子表格版本发布人 */
    private String publishedBy;

    /** BI电子表格版本创建时间 */
    private LocalDateTime createdAt;
}
