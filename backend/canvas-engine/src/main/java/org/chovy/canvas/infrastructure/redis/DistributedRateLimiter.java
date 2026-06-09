package org.chovy.canvas.infrastructure.redis;

import java.time.Duration;

/**
 * DistributedRateLimiter 定义 infrastructure.redis 场景中的扩展契约。
 */
public interface DistributedRateLimiter {

    /**
     * 执行 tryAcquire 流程，围绕 try acquire 完成校验、计算或结果组装。
     *
     * @param scope scope 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param cost cost 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param window window 参数，用于 tryAcquire 流程中的校验、计算或对象转换。
     * @return 返回 try acquire 的布尔判断结果。
     */
    boolean tryAcquire(String scope, String operator, int cost, int limit, Duration window);
}
