package org.chovy.canvas.config;

import lombok.Data;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ExecutionLaneProperties 提供 config 场景的 Spring 配置或启动校验。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution-lane")
public class ExecutionLaneProperties {

    private Lane light = new Lane(600, 2000);
    private Lane standard = new Lane(1800, 10000);
    private Lane heavy = new Lane(300, 1000);
    private Lane retry = new Lane(300, 3000);

    /**
     * limitFor 处理 config 场景的业务逻辑。
     * @param lane lane 参数，用于 limitFor 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public int limitFor(ExecutionLane lane) {
        return laneConfig(lane).getMaxConcurrency();
    }

    /**
     * queueLimitFor 创建或触发 config 场景的业务处理。
     * @param lane lane 参数，用于 queueLimitFor 流程中的校验、计算或对象转换。
     * @return 返回 queue limit for 计算得到的数量、金额或指标值。
     */
    public int queueLimitFor(ExecutionLane lane) {
        return laneConfig(lane).getQueueLimit();
    }

    /**
     * totalMaxConcurrency 校验或转换 config 场景的数据。
     * @return 返回统计数量。
     */
    public int totalMaxConcurrency() {
        return light.maxConcurrency + standard.maxConcurrency + heavy.maxConcurrency + retry.maxConcurrency;
    }

    /**
     * laneConfig 处理 config 场景的业务逻辑。
     * @param lane lane 参数，用于 laneConfig 流程中的校验、计算或对象转换。
     * @return 返回 laneConfig 流程生成的业务结果。
     */
    public Lane laneConfig(ExecutionLane lane) {
        return switch (lane) {
            case LIGHT -> light;
            case STANDARD -> standard;
            case HEAVY -> heavy;
            case RETRY -> retry;
        };
    }

    /**
     * 创建 Lane 实例并注入 config 场景依赖。
     */
    @Data
    public static class Lane {
        private int maxConcurrency;
        private int queueLimit;

        /**
         * 创建可由配置绑定填充的空执行泳道配置。
         */
        public Lane() {
        }

        /**
         * 创建 Lane 实例并注入 config 场景依赖。
         * @param maxConcurrency max concurrency 参数，用于 Lane 流程中的校验、计算或对象转换。
         * @param queueLimit queue limit 参数，用于 Lane 流程中的校验、计算或对象转换。
         */
        public Lane(int maxConcurrency, int queueLimit) {
            this.maxConcurrency = maxConcurrency;
            this.queueLimit = queueLimit;
        }
    }
}
