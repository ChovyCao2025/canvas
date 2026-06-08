package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiSpreadsheetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_spreadsheet")
public class BiSpreadsheetDO {

    /** BI电子表格主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI电子表格电子表格业务键 */
    private String spreadsheetKey;

    /** BI电子表格名称 */
    private String name;

    /** BI电子表格说明 */
    private String description;

    /** BI电子表格工作表明细 JSON */
    private String sheetJson;

    /** BI电子表格数据绑定 JSON */
    private String dataBindingJson;

    /** BI电子表格样式明细 JSON */
    private String styleJson;

    /** BI电子表格当前状态 */
    private String status;

    /** BI电子表格版本号 */
    private Integer version;

    /** BI电子表格创建人 */
    private String createdBy;

    /** BI电子表格创建时间 */
    private LocalDateTime createdAt;

    /** BI电子表格最后更新时间 */
    private LocalDateTime updatedAt;
}
