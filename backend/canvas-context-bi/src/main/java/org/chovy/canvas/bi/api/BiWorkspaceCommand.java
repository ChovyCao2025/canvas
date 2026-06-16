package org.chovy.canvas.bi.api;
/**
 * BiWorkspaceCommand 命令。
 */
public record BiWorkspaceCommand(
        /**
         * 工作空间键。
         */
        String workspaceKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description,
        /**
         * 状态值。
         */
        String status
) {
}
