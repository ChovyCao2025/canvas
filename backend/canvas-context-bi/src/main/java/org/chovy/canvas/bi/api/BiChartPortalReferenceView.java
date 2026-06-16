package org.chovy.canvas.bi.api;
/**
 * BiChartPortalReferenceView 视图。
 */
public record BiChartPortalReferenceView(
        /**
         * portalKey 对应的业务键。
         */
        String portalKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * menuKey 对应的业务键。
         */
        String menuKey,
        /**
         * menuTitle 字段值。
         */
        String menuTitle,
        String status) {
}
