package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_message` 表的会话消息持久化对象。
 */
@TableName("conversation_message")
public class ConversationMessageDO {
    /**
     * 消息记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离消息数据的租户标识。
     */
    Long tenantId;

    /**
     * 消息所属会话的主键。
     */
    Long sessionId;

    /**
     * 消息方向，例如用户入站或系统出站。
     */
    String direction;

    /**
     * 消息类型，例如文本、事件或其他渠道载荷。
     */
    String messageType;

    /**
     * 渠道侧提供的外部消息标识。
     */
    String externalMessageId;

    /**
     * 用于避免重复入库的幂等键。
     */
    String idempotencyKey;

    /**
     * 消息原始结构化内容的 JSON 表示。
     */
    String contentJson;

    /**
     * 便于检索和意图识别的纯文本内容。
     */
    String textContent;

    /**
     * 解析得到的会话意图。
     */
    String intent;

    /**
     * 标记消息是否已被后续流程处理。
     */
    Boolean processed;

    /**
     * 消息写入数据库的时间。
     */
    LocalDateTime createdAt;

    /**
     * 返回用于重复消息去重的幂等键。
     *
     * @return 渠道消息幂等键
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 返回消息结构化内容的 JSON 文本。
     *
     * @return 消息内容 JSON
     */
    public String getContentJson() {
        return contentJson;
    }
}
