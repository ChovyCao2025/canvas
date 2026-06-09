package org.chovy.canvas.config;

import jakarta.annotation.PostConstruct;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ExecutionLanePropertiesValidator 提供 config 场景的 Spring 配置或启动校验。
 */
@Component
public class ExecutionLanePropertiesValidator {

    private final ExecutionLaneProperties properties;
    private final int globalMaxConcurrency;

    /**
     * 创建 ExecutionLanePropertiesValidator 实例并注入 config 场景依赖。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param globalMaxConcurrency global max concurrency 参数，用于 ExecutionLanePropertiesValidator 流程中的校验、计算或对象转换。
     */
    public ExecutionLanePropertiesValidator(
            ExecutionLaneProperties properties,
            @Value("${canvas.execution.max-concurrency:3000}") int globalMaxConcurrency) {
        this.properties = properties;
        this.globalMaxConcurrency = globalMaxConcurrency;
    }

    /**
     * validate 校验或转换 config 场景的数据。
     */
    @PostConstruct
    public void validate() {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ExecutionLane lane : ExecutionLane.values()) {
            int limit = properties.limitFor(lane);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (limit <= 0) {
                throw new IllegalStateException(
                        "canvas.execution-lane." + lane.key() + ".max-concurrency must be positive");
            }
        }
        int laneTotal = properties.totalMaxConcurrency();
        if (laneTotal > globalMaxConcurrency) {
            throw new IllegalStateException(
                    "canvas.execution-lane lane total " + laneTotal
                            + " exceeds canvas.execution.max-concurrency " + globalMaxConcurrency);
        }
    }
}
