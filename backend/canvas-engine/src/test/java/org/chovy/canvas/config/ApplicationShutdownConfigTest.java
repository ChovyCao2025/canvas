package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationShutdownConfigTest {

    @Test
    void applicationYamlEnablesGracefulShutdownAndDrainTimeouts() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
        assertThat(properties.getProperty("canvas.execution.shutdown-drain-timeout-ms")).isEqualTo("10000");
        assertThat(properties.getProperty("canvas.background-tasks.max-in-flight")).isEqualTo("1000");
    }
}
