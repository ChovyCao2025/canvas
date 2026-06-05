package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_bitmap_rollback")
public class AudienceBitmapRollbackDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long audienceId;

    private Long targetVersion;

    private String targetBitmapKey;

    private Long rolledBackVersions;

    private String status;

    private String reason;

    @TableField("operator_name")
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
