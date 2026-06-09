package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationPrivateGroupDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_private_group")
public class ConversationPrivateGroupDO {

    /** 会话私域分组主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话私域分组服务商 */
    private String provider;

    /** 关联的外部分组 ID */
    private String externalGroupId;

    /** 会话私域分组名称 */
    private String name;

    /** 关联的负责人用户 ID */
    private String ownerUserId;

    /** 会话私域分组当前状态 */
    private String status;

    /** 会话私域分组成员数量 */
    private Integer memberCount;

    /** 会话私域分组创建时间远程 */
    private LocalDateTime createdAtRemote;

    /** 会话私域分组原始载荷 JSON */
    private String rawPayloadJson;

    /** 会话私域分组同步时间 */
    private LocalDateTime syncedAt;

    /** 会话私域分组创建时间 */
    private LocalDateTime createdAt;

    /** 会话私域分组最后更新时间 */
    private LocalDateTime updatedAt;
}
