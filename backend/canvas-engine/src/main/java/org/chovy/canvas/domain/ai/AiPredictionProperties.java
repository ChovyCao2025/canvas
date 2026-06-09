package org.chovy.canvas.domain.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AiPredictionProperties 编排 domain.ai 场景的领域业务规则。
 */
@Component
@ConfigurationProperties(prefix = "canvas.ai.prediction")
public class AiPredictionProperties {

    private boolean enabled = false;
    private int batchSize = 500;
    private int defaultBestSendHour = 20;
    private int sparseHistoryMinEvents = 3;
    private String modelVersion = "baseline_v1";

    /**
     * isEnabled 校验或转换 domain.ai 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * setEnabled 处理 domain.ai 场景的业务逻辑。
     * @param enabled enabled 参数，用于 setEnabled 流程中的校验、计算或对象转换。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * getBatchSize 查询 domain.ai 场景的业务数据。
     * @return 返回 get batch size 计算得到的数量、金额或指标值。
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * setBatchSize 处理 domain.ai 场景的业务逻辑。
     * @param batchSize batch size 参数，用于 setBatchSize 流程中的校验、计算或对象转换。
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * getDefaultBestSendHour 查询 domain.ai 场景的业务数据。
     * @return 返回 get default best send hour 计算得到的数量、金额或指标值。
     */
    public int getDefaultBestSendHour() {
        return defaultBestSendHour;
    }

    /**
     * setDefaultBestSendHour 处理 domain.ai 场景的业务逻辑。
     * @param defaultBestSendHour default best send hour 参数，用于 setDefaultBestSendHour 流程中的校验、计算或对象转换。
     */
    public void setDefaultBestSendHour(int defaultBestSendHour) {
        this.defaultBestSendHour = defaultBestSendHour;
    }

    /**
     * getSparseHistoryMinEvents 查询 domain.ai 场景的业务数据。
     * @return 返回 get sparse history min events 计算得到的数量、金额或指标值。
     */
    public int getSparseHistoryMinEvents() {
        return sparseHistoryMinEvents;
    }

    /**
     * setSparseHistoryMinEvents 处理 domain.ai 场景的业务逻辑。
     * @param sparseHistoryMinEvents sparse history min events 参数，用于 setSparseHistoryMinEvents 流程中的校验、计算或对象转换。
     */
    public void setSparseHistoryMinEvents(int sparseHistoryMinEvents) {
        this.sparseHistoryMinEvents = sparseHistoryMinEvents;
    }

    /**
     * getModelVersion 查询 domain.ai 场景的业务数据。
     * @return 返回 get model version 生成的文本或业务键。
     */
    public String getModelVersion() {
        return modelVersion;
    }

    /**
     * setModelVersion 处理 domain.ai 场景的业务逻辑。
     * @param modelVersion model version 参数，用于 setModelVersion 流程中的校验、计算或对象转换。
     */
    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
