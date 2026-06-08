package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiWorkspaceMemberDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_workspace_member")
public class BiWorkspaceMemberDO {

    /** BI工作空间成员主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的用户 ID */
    private String userId;

    /** BI工作空间成员角色业务键 */
    private String roleKey;

    /** BI工作空间成员创建时间 */
    private LocalDateTime createdAt;
}
