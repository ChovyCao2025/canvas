package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface PublicIngressFacade {

    Map<String, Object> publicMarketingForm(String publicKey);

    Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                            Map<String, String> headers);

    String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge);

    List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody);

    Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                   String signature, String rawBody);

    Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                 String signature, String rawBody);
}
