package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CanvasProjectFolderDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_project_folder")
public class CanvasProjectFolderDO {
    /** 画布项目文件夹主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 标签显示名称 */
    private Long tenantId;
    /** 关联的画布 ID */
    private Long canvasId;
    /** 关联的项目 ID */
    private Long projectId;
    /** 画布项目文件夹项目业务键 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectKey;
    /** 画布项目文件夹项目名称 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectName;
    /** 画布项目文件夹文件夹业务键 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderKey;
    /** 画布项目文件夹文件夹名称 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderName;
    /** 画布项目文件夹最后更新人 */
    private String updatedBy;
    /** 画布项目文件夹创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 画布项目文件夹最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
