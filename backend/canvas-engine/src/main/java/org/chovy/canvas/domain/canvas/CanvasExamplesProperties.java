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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
