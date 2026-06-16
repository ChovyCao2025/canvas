package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_contact_profile` 表的联系人画像持久化对象。
 */
@TableName("conversation_contact_profile")
public class ConversationContactProfileDO {
    /**
     * 联系人画像记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离联系人画像数据的租户标识。
     */
    Long tenantId;

    /**
     * 会话域内识别联系人的用户标识。
     */
    String userId;

    /**
     * 面向运营人员展示的联系人名称。
     */
    String displayName;

    /**
     * 外部私域或客服系统中的联系人标识。
     */
    String externalContactId;

    /**
     * 联系人来源的私域平台或渠道。
     */
    String privateDomainSource;

    /**
     * 负责维护该联系人的归属人员。
     */
    String owner;

    /**
     * 联系人所处的生命周期阶段。
     */
    String lifecycleStage;

    /**
     * 联系人标签列表的 JSON 表示。
     */
    String tagsJson;

    /**
     * 联系人扩展属性的 JSON 表示。
     */
    String attributesJson;

    /**
     * 联系人画像首次写入数据库的时间。
     */
    LocalDateTime createdAt;

    /**
     * 联系人画像最近一次更新的时间。
     */
    LocalDateTime updatedAt;
}
