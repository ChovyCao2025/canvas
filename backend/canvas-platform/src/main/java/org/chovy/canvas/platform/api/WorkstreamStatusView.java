package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 展示平台工作流当前准入状态的视图。
 */
public final class WorkstreamStatusView {

    /**
     * 工作流稳定键。
     */
    private final String workstreamKey;

    /**
     * 工作流展示名称。
     */
    private final String displayName;

    /**
     * 工作流优先级。
     */
    private final String priority;

    /**
     * 当前准入状态。
     */
    private final String status;

    /**
     * 子规格文档路径。
     */
    private final String childSpecPath;

    /**
     * 工作流状态摘要。
     */
    private final String summary;

    /**
     * 创建平台工作流状态视图。
     *
     * @param workstreamKey 工作流稳定键
     * @param displayName 工作流展示名称
     * @param priority 工作流优先级
     * @param status 当前准入状态
     * @param childSpecPath 子规格文档路径
     * @param summary 工作流状态摘要
     */
    public WorkstreamStatusView(
            String workstreamKey,
            String displayName,
            String priority,
            String status,
            String childSpecPath,
            String summary) {
        this.workstreamKey = workstreamKey;
        this.displayName = displayName;
        this.priority = priority;
        this.status = status;
        this.childSpecPath = childSpecPath;
        this.summary = summary;
    }

    /**
     * 返回工作流稳定键。
     *
     * @return 工作流稳定键
     */
    public String workstreamKey() {
        return workstreamKey;
    }

    /**
     * 返回工作流展示名称。
     *
     * @return 工作流展示名称
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 返回工作流优先级。
     *
     * @return 工作流优先级
     */
    public String priority() {
        return priority;
    }

    /**
     * 返回当前准入状态。
     *
     * @return 当前准入状态
     */
    public String status() {
        return status;
    }

    /**
     * 返回子规格文档路径。
     *
     * @return 子规格文档路径
     */
    public String childSpecPath() {
        return childSpecPath;
    }

    /**
     * 返回工作流状态摘要。
     *
     * @return 工作流状态摘要
     */
    public String summary() {
        return summary;
    }

    /**
     * 判断两个视图值对象是否相同。
     *
     * @param object 待比较对象
     * @return 所有字段相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof WorkstreamStatusView that)) {
            return false;
        }
        return Objects.equals(workstreamKey, that.workstreamKey)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(priority, that.priority)
                && Objects.equals(status, that.status)
                && Objects.equals(childSpecPath, that.childSpecPath)
                && Objects.equals(summary, that.summary);
    }

    /**
     * 计算视图值对象哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(workstreamKey, displayName, priority, status, childSpecPath, summary);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "WorkstreamStatusView[workstreamKey=" + workstreamKey
                + ", displayName=" + displayName
                + ", priority=" + priority
                + ", status=" + status
                + ", childSpecPath=" + childSpecPath
                + ", summary=" + summary + "]";
    }
}
