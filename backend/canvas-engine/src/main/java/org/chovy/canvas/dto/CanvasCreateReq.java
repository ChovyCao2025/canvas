package org.chovy.canvas.dto;

import lombok.Data;

/**
 * 创建画布请求。
 *
 * <p>由画布列表页“新建旅程”提交，创建后会同时初始化草稿版本。
 */
@Data
public class CanvasCreateReq {

    /** 画布名称。 */
    private String name;

    /** 画布描述（可选）。 */
    private String description;

    /** 初始 graph JSON（可为空，后端会回填默认 START/END 结构）。 */
    private String graphJson;

    /** 创建人（用户名或操作人标识）。 */
    private String createdBy;

    /** 触发类型：REALTIME | SCHEDULED。 */
    private String triggerType;

    /** 定时触发表达式（triggerType=SCHEDULED 时使用）。 */
    private String cronExpression;
}
