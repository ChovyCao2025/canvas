package org.chovy.canvas.config;

import jakarta.annotation.PostConstruct;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExecutionLanePropertiesValidator {

    private final ExecutionLaneProperties properties;
    private final int globalMaxConcurrency;

    public ExecutionLanePropertiesValidator(
            ExecutionLaneProperties properties,
            @Value("${canvas.execution.max-concurrency:3000}") int globalMaxConcurrency) {
        this.properties = properties;
        this.globalMaxConcurrency = globalMaxConcurrency;
    }

    @PostConstruct
    public void validate() {
        for (ExecutionLane lane : ExecutionLane.values()) {
            int limit = properties.limitFor(lane);
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
