package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A/B 实验定义（ab_experiment）。
 *
 * <p>注册可在 AB_SPLIT 节点中引用的实验，由实验平台管理分流规则（命中率、分桶等）。
 * AB_SPLIT 节点执行时通过 experimentKey 查询实验平台，
 * 根据返回的分组（group）路由到对应分支。
 */
@Data
@TableName("ab_experiment")
public class AbExperiment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实验显示名称 */
    private String name;

    /** 实验唯一标识，AB_SPLIT 节点通过此 key 查询实验分组结果 */
    private String experimentKey;

    /** 实验描述 */
    private String description;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
