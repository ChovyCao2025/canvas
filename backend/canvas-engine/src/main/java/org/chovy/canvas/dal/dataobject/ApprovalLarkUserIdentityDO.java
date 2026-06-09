package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ApprovalLarkUserIdentityDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("approval_lark_user_identity")
public class ApprovalLarkUserIdentityDO {

    /** 审批飞书用户身份主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 审批飞书用户身份用户名 */
    private String username;
    /** 关联的飞书Open ID */
    private String larkOpenId;
    /** 关联的飞书用户 ID */
    private String larkUserId;
    /** 关联的飞书部门 ID */
    private String larkDepartmentId;

    /** 审批飞书用户身份创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 审批飞书用户身份最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
