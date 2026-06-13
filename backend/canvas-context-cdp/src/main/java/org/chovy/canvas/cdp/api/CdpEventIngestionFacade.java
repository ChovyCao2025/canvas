package org.chovy.canvas.cdp.api;

public interface CdpEventIngestionFacade {

    CdpIngestionResult ingestBatch(CdpWriteKeyView writeKey, CdpBatchTrackCommand command);
}
