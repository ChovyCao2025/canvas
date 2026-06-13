package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_message")
public class ConversationMessageDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    Long sessionId;
    String direction;
    String messageType;
    String externalMessageId;
    String idempotencyKey;
    String contentJson;
    String textContent;
    String intent;
    Boolean processed;
    LocalDateTime createdAt;
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    public String getContentJson() {
        return contentJson;
    }
}
