package org.chovy.canvas.engine.channel;

import java.util.Map;

public interface WhatsAppCloudApiClient {

    Map<String, Object> sendMessage(String phoneNumberId,
                                    String accessToken,
                                    Map<String, Object> payload);
}
