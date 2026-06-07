package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_session")
public class ConversationSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long canvasId;

    private Long versionId;

    private String executionId;

    private String userId;

    private String channel;

    private String provider;

    private String status;

    private Integer turnCount;

    private String contextJson;

    private LocalDateTime lastMessageAt;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
