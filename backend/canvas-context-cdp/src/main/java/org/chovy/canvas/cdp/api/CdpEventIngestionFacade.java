package org.chovy.canvas.cdp.api;

/**
 * 定义 CdpEventIngestionFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpEventIngestionFacade {

    /**
     * 执行 ingestBatch 对应的 CDP 业务操作。
     */
    CdpIngestionResult ingestBatch(CdpWriteKeyView writeKey, CdpBatchTrackCommand command);
}
