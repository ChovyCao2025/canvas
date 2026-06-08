package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiAuditLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_audit_log")
public class BiAuditLogDO {

    /** BI审计日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的操作人 ID */
    private String actorId;

    /** BI审计日志动作业务键 */
    private String actionKey;

    /** BI审计日志资源类型 */
    private String resourceType;

    /** BI审计日志资源 ID */
    private Long resourceId;

    /** BI审计日志明细 JSON */
    private String detailJson;

    /** BI审计日志创建时间 */
    private LocalDateTime createdAt;
}
