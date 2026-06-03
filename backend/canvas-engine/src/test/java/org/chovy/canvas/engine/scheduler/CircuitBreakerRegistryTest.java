package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerRegistryTest {

    @Test
    void transitionMethodsAreSynchronized() throws Exception {
        Class<CircuitBreakerRegistry.CircuitBreaker> type = CircuitBreakerRegistry.CircuitBreaker.class;

        assertThat(Modifier.isSynchronized(type.getMethod("checkState").getModifiers())).isTrue();
        assertThat(Modifier.isSynchronized(type.getMethod("recordSuccess").getModifiers())).isTrue();
        assertThat(Modifier.isSynchronized(type.getMethod("recordFailure").getModifiers())).isTrue();
    }

    @Test
    void constructorRejectsInvalidThresholds() {
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 0, 30, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failureThreshold");
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 5, 0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openDurationSec");
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 5, 30, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("halfOpenAttempts");
    }

    @Test
    void opensAfterThresholdAndRejectsBeforeCooldownElapses() {
        CircuitBreakerRegistry.CircuitBreaker breaker =
                new CircuitBreakerRegistry.CircuitBreaker("api", 2, 30, 1);

        breaker.recordFailure();
        breaker.checkState();
        breaker.recordFailure();

        assertThatThrownBy(breaker::checkState)
                .isInstanceOf(CircuitBreakerRegistry.CircuitBreakerOpenException.class)
                .hasMessageContaining("api");
    }
}
