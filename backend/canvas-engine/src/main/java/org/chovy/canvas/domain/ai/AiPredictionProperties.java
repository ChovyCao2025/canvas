package org.chovy.canvas.domain.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "canvas.ai.prediction")
public class AiPredictionProperties {

    private boolean enabled = false;
    private int batchSize = 500;
    private int defaultBestSendHour = 20;
    private int sparseHistoryMinEvents = 3;
    private String modelVersion = "baseline_v1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getDefaultBestSendHour() {
        return defaultBestSendHour;
    }

    public void setDefaultBestSendHour(int defaultBestSendHour) {
        this.defaultBestSendHour = defaultBestSendHour;
    }

    public int getSparseHistoryMinEvents() {
        return sparseHistoryMinEvents;
    }

    public void setSparseHistoryMinEvents(int sparseHistoryMinEvents) {
        this.sparseHistoryMinEvents = sparseHistoryMinEvents;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
