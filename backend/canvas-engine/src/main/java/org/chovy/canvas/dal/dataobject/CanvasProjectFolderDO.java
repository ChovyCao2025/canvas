package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_project_folder")
public class CanvasProjectFolderDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long canvasId;
    private Long projectId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectKey;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderKey;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderName;
    private String updatedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
