package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dataset_field")
public class BiDatasetFieldDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long datasetId;

    private String fieldKey;

    private String displayName;

    private String columnExpression;

    private String roleKey;

    private String dataType;

    private String semanticType;

    private String defaultAggregation;

    private String formatPattern;

    private String unit;

    private Boolean visible;

    private String sensitiveLevel;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
