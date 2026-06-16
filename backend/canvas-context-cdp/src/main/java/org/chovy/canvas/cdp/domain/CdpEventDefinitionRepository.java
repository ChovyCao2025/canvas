package org.chovy.canvas.cdp.domain;

/**
 * 定义 CdpEventDefinition 的持久化访问契约。
 */
public interface CdpEventDefinitionRepository {

    /**
     * 查找Published By Code。
     */
    CdpEventDefinition findPublishedByCode(String eventCode);
}
