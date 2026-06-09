package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationContactProfileDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_contact_profile")
public class ConversationContactProfileDO {

    /** 会话联系人画像主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的用户 ID */
    private String userId;

    /** 会话联系人画像展示名称 */
    private String displayName;

    /** 关联的外部联系人 ID */
    private String externalContactId;

    /** 会话联系人画像私域领域来源 */
    private String privateDomainSource;

    /** 会话联系人画像负责人 */
    private String owner;

    /** 会话联系人画像生命周期阶段 */
    private String lifecycleStage;

    /** 会话联系人画像标签列表 JSON */
    private String tagsJson;

    /** 会话联系人画像事件属性 JSON */
    private String attributesJson;

    /** 会话联系人画像创建时间 */
    private LocalDateTime createdAt;

    /** 会话联系人画像最后更新时间 */
    private LocalDateTime updatedAt;
}
