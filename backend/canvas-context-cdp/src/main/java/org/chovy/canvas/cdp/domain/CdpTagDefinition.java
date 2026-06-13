package org.chovy.canvas.cdp.domain;

public record CdpTagDefinition(
        String tagCode,
        String name,
        String valueType,
        boolean enabled,
        boolean manualEnabled,
        Integer defaultTtlDays) {
}
