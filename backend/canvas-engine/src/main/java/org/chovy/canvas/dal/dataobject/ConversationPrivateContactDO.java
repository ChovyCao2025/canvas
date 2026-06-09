package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationPrivateContactDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_private_contact")
public class ConversationPrivateContactDO {

    /** 会话私域联系人主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话私域联系人服务商 */
    private String provider;

    /** 关联的外部联系人 ID */
    private String externalContactId;

    /** 关联的用户 ID */
    private String userId;

    /** 会话私域联系人展示名称 */
    private String displayName;

    /** 会话私域联系人头像URL */
    private String avatarUrl;

    /** 会话私域联系人企业名称 */
    private String corpName;

    /** 会话私域联系人性别 */
    private String gender;

    /** 会话私域联系人合并ID哈希 */
    private String unionIdHash;

    /** 会话私域联系人标签列表 JSON */
    private String tagsJson;

    /** 会话私域联系人事件属性 JSON */
    private String attributesJson;

    /** 会话私域联系人原始载荷 JSON */
    private String rawPayloadJson;

    /** 会话私域联系人同步时间 */
    private LocalDateTime syncedAt;

    /** 会话私域联系人创建时间 */
    private LocalDateTime createdAt;

    /** 会话私域联系人最后更新时间 */
    private LocalDateTime updatedAt;
}
