package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_sop_task")
public class ConversationSopTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workItemId;

    private String taskKey;

    private String title;

    private String status;

    private String assignee;

    private LocalDateTime dueAt;

    private String completedBy;

    private LocalDateTime completedAt;

    private String metadataJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
