package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserWorkspacePreferenceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("user_workspace_preference")
public class UserWorkspacePreferenceDO {

    /** 用户工作空间偏好主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 关联的用户 ID */
    @TableField("user_id")
    private String userId;

    /** 用户工作空间偏好偏好业务键 */
    @TableField("preference_key")
    private String preferenceKey;

    /** 用户工作空间偏好偏好明细 JSON */
    @TableField("preference_json")
    private String preferenceJson;

    /** 用户工作空间偏好创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 用户工作空间偏好最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
