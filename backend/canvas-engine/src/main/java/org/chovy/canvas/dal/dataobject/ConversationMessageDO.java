package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_message")
public class ConversationMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sessionId;

    private String direction;

    private String messageType;

    private String externalMessageId;

    private String idempotencyKey;

    private String contentJson;

    private String textContent;

    private String intent;

    private Boolean processed;

    private LocalDateTime createdAt;
}
