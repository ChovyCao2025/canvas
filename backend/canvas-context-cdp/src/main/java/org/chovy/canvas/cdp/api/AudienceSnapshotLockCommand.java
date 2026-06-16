package org.chovy.canvas.cdp.api;

/**
 * 表示 AudienceSnapshotLockCommand 的业务数据或处理组件。
 */
public final class AudienceSnapshotLockCommand {

    /**
     * 人群标识。
     */
    private final Long audienceId;

    /**
     * 画布标识。
     */
    private final Long canvasId;

    /**
     * 画布版本标识。
     */
    private final Long canvasVersionId;

    /**
     * 节点标识。
     */
    private final String nodeId;

    /**
     * operator。
     */
    private final String operator;

    /**
     * 使用记录字段创建 AudienceSnapshotLockCommand。
     */
    public AudienceSnapshotLockCommand(
            Long audienceId,
            Long canvasId,
            Long canvasVersionId,
            String nodeId,
            String operator) {
        this.audienceId = audienceId;
        this.canvasId = canvasId;
        this.canvasVersionId = canvasVersionId;
        this.nodeId = nodeId;
        this.operator = operator;
    }

    /**
     * 返回人群标识。
     */
    public Long audienceId() {
        return audienceId;
    }

    /**
     * 返回画布标识。
     */
    public Long canvasId() {
        return canvasId;
    }

    /**
     * 返回画布版本标识。
     */
    public Long canvasVersionId() {
        return canvasVersionId;
    }

    /**
     * 返回节点标识。
     */
    public String nodeId() {
        return nodeId;
    }

    /**
     * 返回operator。
     */
    public String operator() {
        return operator;
    }

    /**
     * 按所有字段比较 AudienceSnapshotLockCommand。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudienceSnapshotLockCommand that = (AudienceSnapshotLockCommand) o;
        return java.util.Objects.equals(audienceId, that.audienceId)
                && java.util.Objects.equals(canvasId, that.canvasId)
                && java.util.Objects.equals(canvasVersionId, that.canvasVersionId)
                && java.util.Objects.equals(nodeId, that.nodeId)
                && java.util.Objects.equals(operator, that.operator);
    }

    /**
     * 根据所有字段计算 AudienceSnapshotLockCommand 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(audienceId, canvasId, canvasVersionId, nodeId, operator);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "AudienceSnapshotLockCommand[" + "audienceId=" + audienceId + ", canvasId=" + canvasId + ", canvasVersionId=" + canvasVersionId + ", nodeId=" + nodeId + ", operator=" + operator + "]";
    }
}
