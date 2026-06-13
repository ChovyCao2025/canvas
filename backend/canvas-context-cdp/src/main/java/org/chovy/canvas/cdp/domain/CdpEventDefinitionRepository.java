package org.chovy.canvas.cdp.domain;

public interface CdpEventDefinitionRepository {

    CdpEventDefinition findPublishedByCode(String eventCode);
}
