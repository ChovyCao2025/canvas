package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 表示 CdpIngestionResult 的业务数据或处理组件。
 */
public final class CdpIngestionResult {

    /**
     * accepted。
     */
    private final int accepted;

    /**
     * rejected。
     */
    private final int rejected;

    /**
     * errors。
     */
    private final List<CdpIngestionError> errors;

    /**
     * 使用记录字段创建 CdpIngestionResult。
     */
    public CdpIngestionResult(
            int accepted,
            int rejected,
            List<CdpIngestionError> errors) {
        this.accepted = accepted;
        this.rejected = rejected;
        this.errors = errors;
    }

    /**
     * 返回accepted。
     */
    public int accepted() {
        return accepted;
    }

    /**
     * 返回rejected。
     */
    public int rejected() {
        return rejected;
    }

    /**
     * 返回errors。
     */
    public List<CdpIngestionError> errors() {
        return errors;
    }

    /**
     * 按所有字段比较 CdpIngestionResult。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpIngestionResult that = (CdpIngestionResult) o;
        return java.util.Objects.equals(accepted, that.accepted)
                && java.util.Objects.equals(rejected, that.rejected)
                && java.util.Objects.equals(errors, that.errors);
    }

    /**
     * 根据所有字段计算 CdpIngestionResult 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(accepted, rejected, errors);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpIngestionResult[" + "accepted=" + accepted + ", rejected=" + rejected + ", errors=" + errors + "]";
    }
}
