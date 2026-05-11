package com.photon.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("context_field")
public class ContextField {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fieldKey;
    private String fieldName;

    /** STRING / NUMBER / BOOLEAN / LIST */
    private String dataType;

    private String sourceNodeType;
    private String description;
}
