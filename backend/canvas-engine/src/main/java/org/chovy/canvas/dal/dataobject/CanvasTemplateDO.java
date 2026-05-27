package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布模板（canvas_template）。
 */
@Data
@TableName("canvas_template")
public class CanvasTemplateDO {

    @TableId(type = IdType.AUTO)
    /** 模板主键 ID。 */
    private Long id;

    /** 模板名称。 */
    private String name;

    /** 模板描述。 */
    private String description;

    /** 模板分类。 */
    private String category;

    /** 官方模板稳定唯一键。 */
    private String templateKey;

    /** 公司类型。 */
    private String companyType;

    /** 营销场景。 */
    private String marketingScenario;

    /** 示例难度。 */
    private String difficulty;

    /** 覆盖的节点类型，逗号分隔。 */
    private String coveredNodeTypes;

    /** 官方模板排序。 */
    private Integer sortOrder;

    /** 模板是否启用：1=启用。 */
    private Integer enabled;

    /** 模板图结构 JSON。 */
    private String graphJson;

    /** 模板缩略图（URL 或 Base64，按实现约定）。 */
    private String thumbnail;

    /** 官方模板标记：1=官方，0=自定义。 */
    private Integer isOfficial;

    /** 模板使用次数。 */
    private Integer useCount;

    /** 创建人。 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 模板创建时间，由 MyBatis-Plus 自动填充。 */
    private LocalDateTime createdAt;
}
