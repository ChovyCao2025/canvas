package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Coordinates one-shot timeout scheduling for waiting DAG nodes.
 */
@Component
public class NodeTimeoutCoordinator {

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param ctx ctx 参数，用于 scheduleOnce 流程中的校验、计算或对象转换。
     * @param timerKey 业务键，用于在同一租户下定位资源。
     * @param startTimeKey 业务键，用于在同一租户下定位资源。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @param scheduler scheduler 参数，用于 scheduleOnce 流程中的校验、计算或对象转换。
     * @param reactiveTaskRegistry reactive task registry 参数，用于 scheduleOnce 流程中的校验、计算或对象转换。
     * @param timeoutAction 时间参数，用于计算窗口、过期或审计时间。
     * @param onError on error 参数，用于 scheduleOnce 流程中的校验、计算或对象转换。
     * @return 返回 schedule once 的布尔判断结果。
     */
    boolean scheduleOnce(ExecutionContext ctx,
                         String timerKey,
                         String startTimeKey,
                         String taskName,
                         int timeoutSec,
                         Scheduler scheduler,
                         TrackedReactiveTaskRegistry reactiveTaskRegistry,
                         Runnable timeoutAction,
                         Consumer<Throwable> onError) {
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) {
            return false;
        }
        ctx.getHubStartTimes().putIfAbsent(startTimeKey, System.currentTimeMillis());
        reactiveTaskRegistry.submit(
                taskName,
                Mono.delay(Duration.ofSeconds(timeoutSec), scheduler)
                        .doOnNext(__ -> timeoutAction.run())
                        .then(),
                onError);
        return true;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param ctx ctx 参数，用于 hasElapsed 流程中的校验、计算或对象转换。
     * @param startTimeKey 业务键，用于在同一租户下定位资源。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    boolean hasElapsed(ExecutionContext ctx, String startTimeKey, int timeoutSec) {
        return hasElapsed(ctx, startTimeKey, timeoutSec, System.currentTimeMillis());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param ctx ctx 参数，用于 hasElapsed 流程中的校验、计算或对象转换。
     * @param startTimeKey 业务键，用于在同一租户下定位资源。
     * @param timeoutSec 时间参数，用于计算窗口、过期或审计时间。
     * @param nowMs now ms 参数，用于 hasElapsed 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    boolean hasElapsed(ExecutionContext ctx, String startTimeKey, int timeoutSec, long nowMs) {
        long start = ctx.getHubStartTimes().getOrDefault(startTimeKey, nowMs);
        return nowMs - start > (long) timeoutSec * 1000;
    }
}
