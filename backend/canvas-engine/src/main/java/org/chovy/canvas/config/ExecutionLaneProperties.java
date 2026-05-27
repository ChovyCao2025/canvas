package org.chovy.canvas.config;

import lombok.Data;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution-lane")
public class ExecutionLaneProperties {

    private Lane light = new Lane(600, 2000);
    private Lane standard = new Lane(1800, 10000);
    private Lane heavy = new Lane(300, 1000);
    private Lane retry = new Lane(300, 3000);

    public int limitFor(ExecutionLane lane) {
        return laneConfig(lane).getMaxConcurrency();
    }

    public int queueLimitFor(ExecutionLane lane) {
        return laneConfig(lane).getQueueLimit();
    }

    public int totalMaxConcurrency() {
        return light.maxConcurrency + standard.maxConcurrency + heavy.maxConcurrency + retry.maxConcurrency;
    }

    public Lane laneConfig(ExecutionLane lane) {
        return switch (lane) {
            case LIGHT -> light;
            case STANDARD -> standard;
            case HEAVY -> heavy;
            case RETRY -> retry;
        };
    }

    @Data
    public static class Lane {
        private int maxConcurrency;
        private int queueLimit;

        public Lane() {
        }

        public Lane(int maxConcurrency, int queueLimit) {
            this.maxConcurrency = maxConcurrency;
            this.queueLimit = queueLimit;
        }
    }
}
