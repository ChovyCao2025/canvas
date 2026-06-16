package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_execution")
public class CanvasExecutionDO {

    @TableId
    public String id;

    @TableField("tenant_id")
    public Long tenantId;

    public Long canvasId;
    public Long versionId;
    public String userId;
    public String perfRunId;
    public String triggerType;
    public Integer status;
    public String result;
    public String contextSnapshotJson;
    public String lastDedupKey;

    @TableField(fill = FieldFill.INSERT)
    public LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    public LocalDateTime updatedAt;
}
