package org.chovy.canvas.marketing.api;
import java.util.Objects;

/**
 * 承载MetaOptionView返回给调用方的只读视图。
 */
public final class MetaOptionView {

    /**
     * 选项键。
     */
    private final String key;

    /**
     * 选项展示文案。
     */
    private final String label;

    /**
     * 创建MetaOptionView实例。
     */
    public MetaOptionView(String key, String label) {
        this.key = key;
        this.label = label;
    }

    /**
     * 返回选项键。
     */
    public String key() {
        return key;
    }

    /**
     * 返回选项展示文案。
     */
    public String label() {
        return label;
    }

    /**
     * 比较两个实例的组件值是否一致。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaOptionView that = (MetaOptionView) o;
        return                 Objects.equals(key, that.key) &&
                Objects.equals(label, that.label);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, label);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "MetaOptionView[key=" + key + ", label=" + label + "]";
    }
}
