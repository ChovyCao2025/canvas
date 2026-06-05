package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_row_permission")
public class BiRowPermissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long datasetId;

    private String ruleKey;

    private String subjectType;

    private String subjectId;

    private String filterJson;

    private Boolean enabled;

    private LocalDateTime createdAt;
}
