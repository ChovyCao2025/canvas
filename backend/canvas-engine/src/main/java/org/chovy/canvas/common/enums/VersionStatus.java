package org.chovy.canvas.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 版本状态枚举（`canvas_version.status`）。
 *
 * <p>同一画布可存在多个版本记录，但同一时刻只允许一个发布版本。
 * 版本推进链路通常是 DRAFT -> PUBLISHED。
 */
@Getter
@AllArgsConstructor
public enum VersionStatus {

    /** 草稿版本。 */
    DRAFT(0),

    /** 已发布版本。 */
    PUBLISHED(1);

    /** 写入数据库的状态码。 */
    private final int code;

    // 版本状态与画布状态不同：前者描述“版本记录”，后者描述“画布整体运营状态”。
}
