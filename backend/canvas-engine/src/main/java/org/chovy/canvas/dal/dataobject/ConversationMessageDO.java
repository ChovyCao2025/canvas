package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationMessageDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_message")
public class ConversationMessageDO {

    /** 会话消息主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的会话 ID */
    private Long sessionId;

    /** 会话消息指标方向 */
    private String direction;

    /** 会话消息消息类型 */
    private String messageType;

    /** 关联的外部消息 ID */
    private String externalMessageId;

    /** 会话消息幂等键 */
    private String idempotencyKey;

    /** 会话消息内容明细 JSON */
    private String contentJson;

    /** 会话消息文本内容 */
    private String textContent;

    /** 会话消息意图 */
    private String intent;

    /** 会话消息处理 */
    private Boolean processed;

    /** 会话消息创建时间 */
    private LocalDateTime createdAt;
}
