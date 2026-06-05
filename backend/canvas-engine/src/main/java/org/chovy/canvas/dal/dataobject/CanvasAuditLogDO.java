package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_audit_log")
public class CanvasAuditLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("canvas_id")
    private Long canvasId;

    private String operator;

    @TableField("operator_role")
    private String operatorRole;

    private String action;

    @TableField("from_version")
    private Long fromVersion;

    @TableField("to_version")
    private Long toVersion;

    private String detail;

    private String ip;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
