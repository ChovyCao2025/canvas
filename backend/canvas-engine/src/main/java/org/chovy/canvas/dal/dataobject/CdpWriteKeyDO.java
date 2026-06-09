package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWriteKeyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_write_key")
public class CdpWriteKeyDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    /** CDP写入键主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP写入键名称 */
    private String name;
    /** CDP写入键键前缀 */
    private String keyPrefix;
    /** CDP写入键键哈希 */
    private String keyHash;
    /** CDP写入键平台 */
    private String platform;
    /** CDP写入键当前状态 */
    private String status;
    /** CDP写入键速率限制QPS */
    private Integer rateLimitQps;
    /** CDP写入键每日配额 */
    private Long dailyQuota;
    /** CDP写入键说明 */
    private String description;
    /** CDP写入键创建人 */
    private String createdBy;
    /** CDP写入键创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** CDP写入键最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
