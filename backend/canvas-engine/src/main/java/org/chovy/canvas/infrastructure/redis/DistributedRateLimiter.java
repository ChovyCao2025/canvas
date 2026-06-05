package org.chovy.canvas.infrastructure.redis;

import java.time.Duration;

public interface DistributedRateLimiter {

    boolean tryAcquire(String scope, String operator, int cost, int limit, Duration window);
}
