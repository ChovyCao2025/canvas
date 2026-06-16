package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiPortalResourceCommand 命令。
 */
public record BiPortalResourceCommand(
        /**
         * portalKey 对应的业务键。
         */
        String portalKey,
        /**
         * 展示标题。
         */
        String title,
        /**
         * 说明文本。
         */
        String description,
        /**
         * dashboardKeys 对应的数据集合。
         */
        List<String> dashboardKeys,
        /**
         * 布局配置。
         */
        Map<String, Object> layout,
        /**
         * settings 对应的数据集合。
         */
        Map<String, Object> settings,
        String status) {
}
