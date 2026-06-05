package org.chovy.canvas.testsupport;

import org.junit.jupiter.api.Assumptions;
import org.springframework.test.context.DynamicPropertyRegistry;

public abstract class CanvasRocketMqTestSupport extends CanvasIntegrationTestBase {

    private static final String DEFAULT_NAME_SERVER = "localhost:9876";

    protected static String rocketMqNameServer() {
        String systemProperty = System.getProperty("canvas.test.rocketmq.name-server");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        String env = System.getenv("ROCKETMQ_NAME_SERVER");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return DEFAULT_NAME_SERVER;
    }

    protected static void assumeRocketMqSubstituteAvailable() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("canvas.test.rocketmq.enabled",
                System.getenv().getOrDefault("CANVAS_TEST_ROCKETMQ_ENABLED", "false")));
        Assumptions.assumeTrue(enabled,
                "RocketMQ integration uses docker-compose.local.yml until a dedicated RocketMQ Testcontainer is stable");
    }

    @org.springframework.test.context.DynamicPropertySource
    static void registerRocketMqProperties(DynamicPropertyRegistry registry) {
        registry.add("rocketmq.name-server", CanvasRocketMqTestSupport::rocketMqNameServer);
    }
}
