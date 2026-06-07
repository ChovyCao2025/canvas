package org.chovy.canvas.domain.programmatic;

import java.util.Map;

public record ProgrammaticDspSeatCommand(
        String provider,
        String seatKey,
        String displayName,
        String advertiserAccountId,
        String currency,
        String timezone,
        String supplyChainEnforcement,
        Boolean enabled,
        Map<String, Object> metadata) {
}
