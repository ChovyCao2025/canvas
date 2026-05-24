package org.chovy.canvas.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 画布状态枚举。
 */
@Getter
@AllArgsConstructor
public enum CanvasStatusEnum {

    /** 草稿态。 */
    DRAFT(0),

    /** 已发布。 */
    PUBLISHED(1),

    /** 已下线。 */
    OFFLINE(2),

    /** 已归档。 */
    ARCHIVED(3),

    /** 已紧急停止。 */
    KILLED(4);

    /** 状态码。 */
    private final Integer code;
}
