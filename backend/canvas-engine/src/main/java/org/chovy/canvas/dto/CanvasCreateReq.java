package org.chovy.canvas.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建画布请求。
 *
 * <p>由画布列表页“新建旅程”提交，创建后会同时初始化草稿版本。
 */
@Data
public class CanvasCreateReq {

    /** 画布名称。 */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 画布描述（可选）。 */
    @Size(max = 1024)
    private String description;

    /** 初始 graph JSON（可为空，后端会回填默认 START/END 结构）。 */
    @Size(max = 1_000_000)
    private String graphJson;

    /** 创建人（用户名或操作人标识）。 */
    @Size(max = 128)
    private String createdBy;

    /** 所属租户 ID，由认证上下文写入；超级管理员可显式指定。 */
    @Positive
    private Long tenantId;

    /** 平铺项目分组 key。 */
    @Size(max = 128)
    private String projectKey;

    /** 平铺项目展示名。 */
    @Size(max = 255)
    private String projectName;

    /** 平铺文件夹分组 key。 */
    @Size(max = 128)
    private String folderKey;

    /** 平铺文件夹展示名。 */
    @Size(max = 255)
    private String folderName;

    /** 触发类型：REALTIME | SCHEDULED。 */
    @Pattern(regexp = "REALTIME|SCHEDULED")
    private String triggerType;

    /** 定时触发表达式（triggerType=SCHEDULED 时使用）。 */
    @Size(max = 128)
    private String cronExpression;
}
