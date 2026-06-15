package org.chovy.canvas.cdp.api;

public interface CdpWriteKeyAuthenticationFacade {

    CdpWriteKeyView authenticate(String authorizationHeader);
}
