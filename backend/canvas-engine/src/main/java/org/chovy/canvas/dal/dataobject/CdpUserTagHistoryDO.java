package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDP 用户标签历史 数据对象，对应数据库表 {@code cdp_user_tag_history}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("cdp_user_tag_history")
public class CdpUserTagHistoryDO {

    @TableId(type = IdType.AUTO)
    /** 标签变更历史主键 ID */
    private Long id;

    /** CDP 内部统一用户 ID */
    private String userId;

    /** 标签编码，对应 tag_definition.tag_code */
    private String tagCode;

    /** 变更前标签值，新增时为空 */
    private String oldValue;

    /** 变更后标签值，删除时为空 */
    private String newValue;

    /** 变更操作类型，如 UPSERT、DELETE、EXPIRE */
    private String operation;

    /** 变更来源类型，如 CANVAS、IMPORT、MANUAL */
    private String sourceType;

    /** 来源引用 ID，如执行 ID、导入批次 ID 或外部请求 ID */
    private String sourceRefId;

    /** 幂等键，用于防止重复写入同一标签变更历史 */
    private String idempotencyKey;

    /** 变更原因或操作备注 */
    private String reason;

    /** 操作人 */
    private String operator;

    /** 实际操作时间 */
    private LocalDateTime operatedAt;
}
