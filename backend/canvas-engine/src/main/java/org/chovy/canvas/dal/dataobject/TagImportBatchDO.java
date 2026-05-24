package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_import_batch")
public class TagImportBatchDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;
    private String status;
    private String fileName;
    private String externalUrl;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
