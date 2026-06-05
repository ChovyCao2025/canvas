package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analytics_funnel_definition")
public class AnalyticsFunnelDefinitionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String funnelKey;

    private Integer version;

    private String name;

    private String stepsJson;

    private Boolean enabled;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
