package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChannelDedupeRecordDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("channel_dedupe_record")
public class ChannelDedupeRecordDO {

    /** 渠道去重记录主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 渠道去重记录去重分组 */
    private String dedupeGroup;

    /** 渠道去重记录内容哈希 */
    private String contentHash;

    /** 渠道去重记录触达渠道 */
    private String channel;

    /** 关联的用户 ID */
    private String userId;

    /** 渠道去重记录过期时间 */
    private LocalDateTime expiresAt;

    /** 渠道去重记录创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
