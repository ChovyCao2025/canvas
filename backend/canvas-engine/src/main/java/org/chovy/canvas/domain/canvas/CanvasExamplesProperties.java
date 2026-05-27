package org.chovy.canvas.domain.canvas;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Canvas Examples 画布领域组件。
 *
 * <p>负责画布模板、示例、版本或生命周期相关业务能力，协调 Mapper、缓存、调度和执行引擎。
 * <p>该层承载画布域规则，控制器和基础设施代码不应绕过它直接修改核心状态。
 */
@ConfigurationProperties(prefix = "canvas.examples")
public class CanvasExamplesProperties {

    /**
     * true: import and show official example canvases.
     * false: skip import and hide example canvases from normal list results.
     */
    private boolean enabled = true;

    /**
     * 判断 is Enabled 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 执行 set Enabled 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param enabled enabled 方法执行所需的业务参数
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
