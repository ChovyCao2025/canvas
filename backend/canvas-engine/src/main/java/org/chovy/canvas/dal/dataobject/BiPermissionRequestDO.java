package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiPermissionRequestDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_permission_request")
public class BiPermissionRequestDO {

    /** BI权限请求主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI权限请求资源类型 */
    private String resourceType;

    /** BI权限请求资源业务键 */
    private String resourceKey;

    /** BI权限请求请求动作 */
    private String requestedAction;

    /** BI权限请求请求人 */
    private String requestedBy;

    /** BI权限请求请求时间 */
    private LocalDateTime requestedAt;

    /** BI权限请求原因说明 */
    private String reason;

    /** BI权限请求当前状态 */
    private String status;

    /** BI权限请求审核人 */
    private String reviewedBy;

    /** BI权限请求审核时间 */
    private LocalDateTime reviewedAt;

    /** BI权限请求评审备注 */
    private String reviewComment;

    /** 关联的已发放权限 ID */
    private Long grantedPermissionId;
}
