package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ExecutionRerunAuditDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("execution_rerun_audit")
public class ExecutionRerunAuditDO {
    /** 执行重跑审计主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的画布 ID */
    private Long canvasId;
    /** 关联的用户 ID */
    private String userId;
    /** 关联的测试用户 ID */
    private Long testUserId;
    /** 关联的原始执行 ID */
    private String originalExecutionId;
    /** 执行重跑审计运行模式 */
    private String mode;
    /** 执行重跑审计原因说明 */
    private String reason;
    /** 执行重跑审计操作人 */
    private String operator;
    /** 执行重跑审计当前状态 */
    private String status;
    /** 执行重跑审计输入参数 */
    private String inputParams;
    /** 执行重跑审计创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 执行重跑审计最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
