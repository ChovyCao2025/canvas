package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQueryGateCommand(
        BiQueryCommand query,
        LocalDateTime from,
        LocalDateTime to,
        String mode,
        boolean allowWarn) {
}
