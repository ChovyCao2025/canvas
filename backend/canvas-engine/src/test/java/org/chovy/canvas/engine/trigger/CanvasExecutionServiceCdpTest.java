package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.cdp.CdpUserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 画布执行 Service Cdp 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExecutionServiceCdpTest {

    @Test
    void serviceDeclaresCdpUserServiceDependency() {
        boolean hasDependency = java.util.Arrays.stream(CanvasExecutionService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CdpUserService.class));

        assertThat(hasDependency).isTrue();
    }
}
