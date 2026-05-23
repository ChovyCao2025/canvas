package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_tag_current")
public class UserTagCurrent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String idType;
    private String idValue;
    private String tagCode;
    private String tagValue;
    private LocalDateTime tagTime;
    private String sourceType;
    private Long sourceBatchId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
