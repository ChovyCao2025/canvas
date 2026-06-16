package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasTemplateDO相关的业务逻辑。
 */
@TableName("canvas_template")
public class CanvasTemplateDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存名称。
     */
    private String name;

    /**
     * 保存描述。
     */
    private String description;

    /**
     * 保存category。
     */
    private String category;

    /**
     * 保存templateKey。
     */
    private String templateKey;

    /**
     * 保存companyType。
     */
    private String companyType;

    /**
     * 保存marketingScenario。
     */
    private String marketingScenario;

    /**
     * 保存difficulty。
     */
    private String difficulty;

    /**
     * 保存coveredNodeTypes。
     */
    private String coveredNodeTypes;

    /**
     * 保存sortOrder。
     */
    private Integer sortOrder;

    /**
     * 保存启用状态。
     */
    private Integer enabled;

    /**
     * 保存graphJSON 内容。
     */
    private String graphJson;

    /**
     * 保存thumbnail。
     */
    private String thumbnail;

    /**
     * 保存isOfficial。
     */
    private Integer isOfficial;

    /**
     * 保存useCount。
     */
    private Integer useCount;

    /**
     * 保存创建人。
     */
    private String createdBy;

    /**
     * 保存创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
