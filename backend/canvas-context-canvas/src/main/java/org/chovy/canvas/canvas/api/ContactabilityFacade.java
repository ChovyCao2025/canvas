package org.chovy.canvas.canvas.api;

import java.time.Duration;
import java.util.List;

public interface ContactabilityFacade {

    Report explain(Request request);

    record Request(
            String userId,
            String channel,
            boolean requireExplicitConsent,
            String quietStart,
            String quietEnd,
            String quietTimezone,
            Long canvasId,
            String nodeId,
            String frequencyScope,
            int frequencyMax,
            Duration frequencyWindow) {
    }

    record Report(
            String userId,
            String channel,
            boolean allowed,
            List<Check> checks) {
    }

    record Check(
            String checkKey,
            boolean allowed,
            String reasonCode,
            String reasonMessage) {
    }
}
