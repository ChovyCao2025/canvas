package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CanvasAuditLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_audit_log")
public class CanvasAuditLogDO {

    /** 画布审计日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的画布 ID */
    @TableField("canvas_id")
    private Long canvasId;

    /** 操作人 username */
    private String operator;

    /** 画布审计日志操作员角色 */
    @TableField("operator_role")
    private String operatorRole;

    /** 画布审计日志动作 */
    private String action;

    /** 变更前版本 */
    @TableField("from_version")
    private Long fromVersion;

    /** 变更后版本 */
    @TableField("to_version")
    private Long toVersion;

    /** 变更详情 JSON */
    private String detail;

    /** 操作来源 IP */
    private String ip;

    /** 画布审计日志创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
