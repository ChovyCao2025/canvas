package org.chovy.canvas.infra.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 并发溢出重试消息体。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverflowRetryMessage {

    /**
     * Legacy payload marker kept only so old/forged payloads can be tested and ignored.
     * Retry control state is carried by this DTO field and Disruptor dispatch metadata.
     */
    @Deprecated
    public static final String CHAIN_RETRY_PAYLOAD_KEY = "__overflowChainRetry";

    private Long canvasId;
    private String userId;
    private String triggerType;
    private String triggerNodeType;
    private String matchKey;
    private Map<String, Object> payload;
    private String msgId;
    /** 跨越 sendOverflowRetry 调用的累计重试次数。 */
    private int chainRetryCount;
}
