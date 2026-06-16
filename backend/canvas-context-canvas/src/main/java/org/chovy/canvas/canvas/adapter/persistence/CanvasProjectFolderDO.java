package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasProjectFolderDO相关的业务逻辑。
 */
@TableName("canvas_project_folder")
public class CanvasProjectFolderDO {

    /**
     * 保存标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 保存租户标识。
     */
    private Long tenantId;

    /**
     * 保存画布标识。
     */
    private Long canvasId;

    /**
     * 保存project标识。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long projectId;

    /**
     * 保存projectKey。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectKey;

    /**
     * 保存projectName。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String projectName;

    /**
     * 保存folderKey。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderKey;

    /**
     * 保存folderName。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String folderName;

    /**
     * 保存更新人。
     */
    private String updatedBy;

    /**
     * 保存创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 保存更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 获取标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取租户标识。
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识。
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 获取画布标识。
     */
    public Long getCanvasId() {
        return canvasId;
    }

    /**
     * 设置画布标识。
     */
    public void setCanvasId(Long canvasId) {
        this.canvasId = canvasId;
    }

    /**
     * 获取project标识。
     */
    public Long getProjectId() {
        return projectId;
    }

    /**
     * 设置project标识。
     */
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    /**
     * 获取ProjectKey。
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * 设置ProjectKey。
     */
    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    /**
     * 获取ProjectName。
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置ProjectName。
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * 获取FolderKey。
     */
    public String getFolderKey() {
        return folderKey;
    }

    /**
     * 设置FolderKey。
     */
    public void setFolderKey(String folderKey) {
        this.folderKey = folderKey;
    }

    /**
     * 获取FolderName。
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * 设置FolderName。
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
