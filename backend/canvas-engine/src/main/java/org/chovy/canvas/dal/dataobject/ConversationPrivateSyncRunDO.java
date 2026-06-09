package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationPrivateSyncRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_private_sync_run")
public class ConversationPrivateSyncRunDO {

    /** 会话私域同步运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话私域同步运行服务商 */
    private String provider;

    /** 会话私域同步运行同步类型 */
    private String syncType;

    /** 会话私域同步运行当前状态 */
    private String status;

    /** 会话私域同步运行请求人 */
    private String requestedBy;

    /** 会话私域同步运行来源游标 */
    private String sourceCursor;

    /** 会话私域同步运行下次游标 */
    private String nextCursor;

    /** 会话私域同步运行联系人数量 */
    private Integer contactCount;

    /** 会话私域同步运行联系人写入或更新 */
    private Integer contactUpserted;

    /** 会话私域同步运行分组数量 */
    private Integer groupCount;

    /** 会话私域同步运行分组写入或更新 */
    private Integer groupUpserted;

    /** 会话私域同步运行成员数量 */
    private Integer memberCount;

    /** 会话私域同步运行成员写入或更新 */
    private Integer memberUpserted;

    /** 会话私域同步运行处理失败数量 */
    private Integer failedCount;

    /** 会话私域同步运行错误信息 */
    private String errorMessage;

    /** 会话私域同步运行扩展元数据 JSON */
    private String metadataJson;

    /** 会话私域同步运行开始时间 */
    private LocalDateTime startedAt;

    /** 会话私域同步运行完成时间 */
    private LocalDateTime completedAt;

    /** 会话私域同步运行创建时间 */
    private LocalDateTime createdAt;

    /** 会话私域同步运行最后更新时间 */
    private LocalDateTime updatedAt;
}
