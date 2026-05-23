package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_import_source")
public class TagImportSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String url;
    private String method;
    private String headersJson;
    private String bodyTemplate;
    private String pageParam;
    private String pageSizeParam;
    private Integer pageSize;
    private String recordsPath;
    private String fieldMapping;
    private Integer enabled;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
