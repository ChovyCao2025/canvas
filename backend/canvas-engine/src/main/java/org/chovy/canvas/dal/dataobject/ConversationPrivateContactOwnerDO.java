package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationPrivateContactOwnerDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_private_contact_owner")
public class ConversationPrivateContactOwnerDO {

    /** 会话私域联系人负责人主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话私域联系人负责人服务商 */
    private String provider;

    /** 关联的外部联系人 ID */
    private String externalContactId;

    /** 关联的负责人用户 ID */
    private String ownerUserId;

    /** 会话私域联系人负责人备注 */
    private String remark;

    /** 会话私域联系人负责人状态 */
    private String state;

    /** 会话私域联系人负责人添加方式 */
    private String addWay;

    /** 会话私域联系人负责人标签列表 JSON */
    private String tagsJson;

    /** 会话私域联系人负责人事件属性 JSON */
    private String attributesJson;

    /** 会话私域联系人负责人原始载荷 JSON */
    private String rawPayloadJson;

    /** 会话私域联系人负责人同步时间 */
    private LocalDateTime syncedAt;

    /** 会话私域联系人负责人创建时间 */
    private LocalDateTime createdAt;

    /** 会话私域联系人负责人最后更新时间 */
    private LocalDateTime updatedAt;
}
