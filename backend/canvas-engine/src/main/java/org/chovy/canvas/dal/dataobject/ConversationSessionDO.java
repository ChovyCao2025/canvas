package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationSessionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_session")
public class ConversationSessionDO {

    /** 会话会话主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的画布 ID */
    private Long canvasId;

    /** 关联的版本 ID */
    private Long versionId;

    /** 关联的执行 ID */
    private String executionId;

    /** 关联的用户 ID */
    private String userId;

    /** 会话会话触达渠道 */
    private String channel;

    /** 会话会话服务商 */
    private String provider;

    /** 会话会话当前状态 */
    private String status;

    /** 会话会话轮次数量 */
    private Integer turnCount;

    /** 会话会话上下文明细 JSON */
    private String contextJson;

    /** 会话会话最近消息时间 */
    private LocalDateTime lastMessageAt;

    /** 会话会话过期时间 */
    private LocalDateTime expiresAt;

    /** 会话会话创建时间 */
    private LocalDateTime createdAt;

    /** 会话会话最后更新时间 */
    private LocalDateTime updatedAt;
}
