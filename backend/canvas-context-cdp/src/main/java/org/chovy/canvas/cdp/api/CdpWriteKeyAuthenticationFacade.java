package org.chovy.canvas.cdp.api;

/**
 * 定义 CdpWriteKeyAuthenticationFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWriteKeyAuthenticationFacade {

    /**
     * 执行 authenticate 对应的 CDP 业务操作。
     */
    CdpWriteKeyView authenticate(String authorizationHeader);
}
