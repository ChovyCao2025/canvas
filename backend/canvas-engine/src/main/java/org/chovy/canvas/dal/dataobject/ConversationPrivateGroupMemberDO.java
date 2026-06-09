package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationPrivateGroupMemberDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_private_group_member")
public class ConversationPrivateGroupMemberDO {

    /** 会话私域分组成员主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话私域分组成员服务商 */
    private String provider;

    /** 关联的外部分组 ID */
    private String externalGroupId;

    /** 关联的成员用户 ID */
    private String memberUserId;

    /** 会话私域分组成员成员类型 */
    private String memberType;

    /** 会话私域分组成员展示名称 */
    private String displayName;

    /** 会话私域分组成员合并ID哈希 */
    private String unionIdHash;

    /** 会话私域分组成员加入时间 */
    private LocalDateTime joinTime;

    /** 会话私域分组成员原始载荷 JSON */
    private String rawPayloadJson;

    /** 会话私域分组成员同步时间 */
    private LocalDateTime syncedAt;

    /** 会话私域分组成员创建时间 */
    private LocalDateTime createdAt;

    /** 会话私域分组成员最后更新时间 */
    private LocalDateTime updatedAt;
}
