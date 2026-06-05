package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlTest {

    @Test
    void gracefulShutdownIsConfiguredWithBoundedDrainTimeout() {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
        assertThat(properties.getProperty("canvas.shutdown.background-task-drain-timeout-ms")).isEqualTo("30000");
        assertThat(properties.getProperty("canvas.shutdown.background-subscription-drain-timeout-ms")).isEqualTo("30000");
        assertThat(properties.getProperty("canvas.execution.local-registry-max-entries")).isEqualTo("10000");
    }

    private Properties applicationProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
