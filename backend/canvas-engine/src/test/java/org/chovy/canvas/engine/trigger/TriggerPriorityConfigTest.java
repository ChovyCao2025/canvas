package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trigger Priority 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TriggerPriorityConfigTest {

    @Test
    void defaultsClassifyTriggerTypesByPriority() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();

        assertThat(config.of(null)).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
        assertThat(config.of("DIRECT_CALL")).isEqualTo(TriggerPriorityConfig.Priority.HIGH);
        assertThat(config.of("EVENT")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
        assertThat(config.of("SCHEDULED")).isEqualTo(TriggerPriorityConfig.Priority.LOW);
        assertThat(config.of("UNKNOWN")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
    }

    @Test
    void defaultsExposeConcurrencyAndRetrySettings() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();

        assertThat(config.getLowRatio()).isEqualTo(0.5);
        assertThat(config.getHighMaxConcurrencyRatio()).isEqualTo(2.0);
        assertThat(config.getOverflowRetryDelayMs()).isEqualTo(5000L);
        assertThat(config.getOverflowMaxRetry()).isEqualTo(3);
    }

    @Test
    void configuredListsOverrideDefaultClassification() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();
        config.setHigh(List.of("VIP"));
        config.setNormal(List.of("STANDARD"));
        config.setLow(List.of("BATCH", "STANDARD"));

        assertThat(config.of("VIP")).isEqualTo(TriggerPriorityConfig.Priority.HIGH);
        assertThat(config.of("STANDARD")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
        assertThat(config.of("BATCH")).isEqualTo(TriggerPriorityConfig.Priority.LOW);
        assertThat(config.of("DIRECT_CALL")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
    }

    @Test
    void applicationYamlBindsPrioritySettings() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        MutablePropertySources propertySources = new MutablePropertySources();
        for (PropertySource<?> source : loader.load("application", new ClassPathResource("application.yml"))) {
            propertySources.addLast(source);
        }

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
        TriggerPriorityConfig config = binder
                .bind("canvas.execution.priority", Bindable.of(TriggerPriorityConfig.class))
                .orElseThrow(() -> new AssertionError("canvas.execution.priority should bind from application.yml"));

        assertThat(config.getHigh()).containsExactly("DIRECT_CALL");
        assertThat(config.getNormal()).containsExactly("MQ", "BEHAVIOR", "EVENT", "EVENT_TRIGGER", "API_CALL");
        assertThat(config.getLow()).containsExactly("SCHEDULED");
        assertThat(config.getLowRatio()).isEqualTo(0.5);
        assertThat(config.getHighMaxConcurrencyRatio()).isEqualTo(2.0);
        assertThat(config.getOverflowRetryDelayMs()).isEqualTo(5000L);
        assertThat(config.getOverflowMaxRetry()).isEqualTo(3);
    }
}
