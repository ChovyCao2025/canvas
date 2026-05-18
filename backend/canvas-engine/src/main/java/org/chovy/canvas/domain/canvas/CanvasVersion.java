package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布版本快照（canvas_version）。
 *
 * <p>每次发布或灰度都会生成一个不可变的版本快照，保存完整的 graphJson。
 * 引擎执行时从对应版本读取图结构，保证执行行为与发布时一致，不受后续编辑影响。
 */
@Data
@TableName("canvas_version")
public class CanvasVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属画布 ID */
    private Long canvasId;

    /** 版本号，从 1 递增（不跨画布） */
    private Integer version;

    /**
     * 画布完整图结构 JSON。
     * 包含 nodes（节点列表）和 edges（连线列表），由前端序列化后存储。
     */
    private String graphJson;

    /**
     * 版本状态，见 {@link org.chovy.canvas.domain.constant.VersionStatus}。
     * DRAFT=0, PUBLISHED=1（已发布版本和灰度版本均为 1）
     */
    private Integer status;

    /** 发布操作人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
