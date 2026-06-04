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
@TableName("canvas_project")
public class CanvasProjectDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String projectKey;
    private String projectName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultSettingsJson;
    private Integer requireReviewBeforePublish;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String quietHoursJson;
    private String createdBy;
    private String updatedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
