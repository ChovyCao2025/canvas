package org.chovy.canvas.platform.domain;

import java.util.Objects;

/**
 * 描述平台推进过程中的一个工作流。
 */
public final class PlatformWorkstream {

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
     * 是否必须具备可执行子规格。
     */
    private final boolean requiresChildSpec;

    /**
     * 子规格文档路径。
     */
    private final String childSpecPath;

    /**
     * 工作流状态摘要。
     */
    private final String summary;

    /**
     * 创建平台工作流定义。
     *
     * @param workstreamKey 工作流稳定键
     * @param displayName 工作流展示名称
     * @param priority 工作流优先级
     * @param requiresChildSpec 是否必须具备可执行子规格
     * @param childSpecPath 子规格文档路径
     * @param summary 工作流状态摘要
     */
    public PlatformWorkstream(
            String workstreamKey,
            String displayName,
            String priority,
            boolean requiresChildSpec,
            String childSpecPath,
            String summary) {
        this.workstreamKey = workstreamKey;
        this.displayName = displayName;
        this.priority = priority;
        this.requiresChildSpec = requiresChildSpec;
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
     * 返回是否必须具备可执行子规格。
     *
     * @return 必须具备可执行子规格时返回 true
     */
    public boolean requiresChildSpec() {
        return requiresChildSpec;
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
     * 判断两个工作流定义是否相同。
     *
     * @param object 待比较对象
     * @return 所有字段相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlatformWorkstream that)) {
            return false;
        }
        return requiresChildSpec == that.requiresChildSpec
                && Objects.equals(workstreamKey, that.workstreamKey)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(priority, that.priority)
                && Objects.equals(childSpecPath, that.childSpecPath)
                && Objects.equals(summary, that.summary);
    }

    /**
     * 计算工作流定义哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(workstreamKey, displayName, priority, requiresChildSpec, childSpecPath, summary);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "PlatformWorkstream[workstreamKey=" + workstreamKey
                + ", displayName=" + displayName
                + ", priority=" + priority
                + ", requiresChildSpec=" + requiresChildSpec
                + ", childSpecPath=" + childSpecPath
                + ", summary=" + summary + "]";
    }
}
