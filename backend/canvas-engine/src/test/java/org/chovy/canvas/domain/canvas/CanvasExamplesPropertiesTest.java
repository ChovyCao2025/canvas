package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canvas Examples 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExamplesPropertiesTest {

    @Test
    void examplesAreEnabledByDefault() {
        CanvasExamplesProperties properties = new CanvasExamplesProperties();

        assertThat(properties.isEnabled()).isTrue();
    }
}
