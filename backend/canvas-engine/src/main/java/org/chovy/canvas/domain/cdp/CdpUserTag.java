package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_tag")
public class CdpUserTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String tagCode;
    private String tagValue;
    private String valueType;
    private String sourceType;
    private String sourceRefId;
    private String status;
    private LocalDateTime effectiveAt;
    private LocalDateTime expiresAt;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
