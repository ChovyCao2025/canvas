package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiBigScreenResourceCommand 命令。
 */
public record BiBigScreenResourceCommand(
        /**
         * screenKey 对应的业务键。
         */
        String screenKey,
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
