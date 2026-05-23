package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_profile")
public class CdpUserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String displayName;
    private String phone;
    private String email;
    private String status;
    private String propertiesJson;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
