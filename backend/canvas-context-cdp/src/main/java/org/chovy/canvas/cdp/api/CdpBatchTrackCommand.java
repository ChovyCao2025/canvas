package org.chovy.canvas.cdp.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 表示 CdpBatchTrackCommand 的业务数据或处理组件。
 */
public final class CdpBatchTrackCommand {

    /**
     * batch。
     */
    private final List<CdpTrackEventCommand> batch;

    /**
     * 发送时间。
     */
    private final OffsetDateTime sentAt;

    /**
     * 使用记录字段创建 CdpBatchTrackCommand。
     */
    public CdpBatchTrackCommand(
            List<CdpTrackEventCommand> batch,
            OffsetDateTime sentAt) {
        this.batch = batch;
        this.sentAt = sentAt;
    }

    /**
     * 返回batch。
     */
    public List<CdpTrackEventCommand> batch() {
        return batch;
    }

    /**
     * 返回发送时间。
     */
    public OffsetDateTime sentAt() {
        return sentAt;
    }

    /**
     * 按所有字段比较 CdpBatchTrackCommand。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpBatchTrackCommand that = (CdpBatchTrackCommand) o;
        return java.util.Objects.equals(batch, that.batch)
                && java.util.Objects.equals(sentAt, that.sentAt);
    }

    /**
     * 根据所有字段计算 CdpBatchTrackCommand 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(batch, sentAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpBatchTrackCommand[" + "batch=" + batch + ", sentAt=" + sentAt + "]";
    }
}
