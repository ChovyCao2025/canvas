package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_session` 表的会话持久化对象。
 */
@TableName("conversation_session")
public class ConversationSessionDO {
    /**
     * 会话记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离会话数据的租户标识。
     */
    Long tenantId;

    /**
     * 会话绑定的画布标识。
     */
    Long canvasId;

    /**
     * 会话绑定的画布版本标识。
     */
    Long versionId;

    /**
     * 运行时执行链路的外部标识。
     */
    String executionId;

    /**
     * 会话参与用户的业务标识。
     */
    String userId;

    /**
     * 会话来源渠道。
     */
    String channel;

    /**
     * 会话来源服务商。
     */
    String provider;

    /**
     * 会话生命周期状态。
     */
    String status;

    /**
     * 当前会话累计轮次数。
     */
    Integer turnCount;

    /**
     * 会话上下文快照的 JSON 表示。
     */
    String contextJson;

    /**
     * 最近一条消息发生时间。
     */
    LocalDateTime lastMessageAt;

    /**
     * 会话过期时间。
     */
    LocalDateTime expiresAt;

    /**
     * 会话创建时间。
     */
    LocalDateTime createdAt;

    /**
     * 会话最近更新时间。
     */
    LocalDateTime updatedAt;

    /**
     * 返回租户标识，供测试和 MyBatis 映射校验读取。
     *
     * @return 租户标识
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 返回会话上下文 JSON，供持久化映射校验读取。
     *
     * @return 会话上下文 JSON
     */
    public String getContextJson() {
        return contextJson;
    }
}
