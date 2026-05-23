package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("identity_type")
public class IdentityType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String description;
    private Integer enabled;
    private Integer allowImport;
    private Integer multiValue;
    private Integer priority;
    private Integer participateMapping;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
