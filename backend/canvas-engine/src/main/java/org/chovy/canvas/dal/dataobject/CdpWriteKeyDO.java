package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_write_key")
public class CdpWriteKeyDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String name;
    private String keyPrefix;
    private String keyHash;
    private String platform;
    private String status;
    private Integer rateLimitQps;
    private Long dailyQuota;
    private String description;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
