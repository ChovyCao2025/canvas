package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_private_sync_run")
public class ConversationPrivateSyncRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String syncType;

    private String status;

    private String requestedBy;

    private String sourceCursor;

    private String nextCursor;

    private Integer contactCount;

    private Integer contactUpserted;

    private Integer groupCount;

    private Integer groupUpserted;

    private Integer memberCount;

    private Integer memberUpserted;

    private Integer failedCount;

    private String errorMessage;

    private String metadataJson;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
