package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_tag_operation")
public class CdpTagOperationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operationType;
    private String tagCode;
    private String tagValue;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private String errorMsg;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
