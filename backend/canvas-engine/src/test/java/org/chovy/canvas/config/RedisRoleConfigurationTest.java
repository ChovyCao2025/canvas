package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisRoleConfigurationTest {

    @Test
    void readinessModeRejectsSingleRedisRoleConnection() {
        RedisRoleConfiguration config = new RedisRoleConfiguration(
                true,
                "redis://localhost:6379/0",
                "redis://localhost:6379/0",
                "redis://localhost:6379/0",
                "redis://localhost:6379/0");

        assertThatThrownBy(config::validate)
                .hasMessageContaining("4000 readiness requires separated Redis role connections");
    }

    @Test
    void readinessModeAcceptsSeparatedRoleConnections() {
        RedisRoleConfiguration config = new RedisRoleConfiguration(
                true,
                "redis://localhost:6379/0",
                "redis://localhost:6379/1",
                "redis://localhost:6379/2",
                "redis://localhost:6379/3");

        assertThatCode(config::validate).doesNotThrowAnyException();
    }

    @Test
    void readinessModeRejectsPartialRedisRoleSharing() {
        RedisRoleConfiguration config = new RedisRoleConfiguration(
                true,
                "redis://localhost:6379/0",
                "redis://localhost:6379/0",
                "redis://localhost:6379/2",
                "redis://localhost:6379/3");

        assertThatThrownBy(config::validate)
                .hasMessageContaining("4000 readiness requires separated Redis role connections");
    }

    @Test
    void disabledReadinessModeKeepsExistingSingleRedisConnectionPath() {
        RedisRoleConfiguration config = new RedisRoleConfiguration(
                false,
                "redis://localhost:6379/0",
                "redis://localhost:6379/0",
                "redis://localhost:6379/0",
                "redis://localhost:6379/0");

        assertThatCode(config::validate).doesNotThrowAnyException();
    }
}
