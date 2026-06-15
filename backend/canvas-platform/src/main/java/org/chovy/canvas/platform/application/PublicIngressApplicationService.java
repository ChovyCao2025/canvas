package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.platform.api.PublicIngressFacade;
import org.chovy.canvas.platform.domain.PublicIngressCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicIngressApplicationService implements PublicIngressFacade {

    private final PublicIngressCatalog catalog;

    public PublicIngressApplicationService() {
        this(new PublicIngressCatalog());
    }

    public PublicIngressApplicationService(PublicIngressCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> publicMarketingForm(String publicKey) {
        return catalog.publicMarketingForm(publicKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                                   Map<String, String> headers) {
        return catalog.submitMarketingForm(publicKey, safePayload(payload), safeHeaders(headers));
    }

    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> response,
                                                   Map<String, Object> utm, String anonymousId,
                                                   String idempotencyKey, String consentChannel,
                                                   String consentStatus, String userAgent, String ipHash) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("response", safePayload(response));
        payload.put("utm", safePayload(utm));
        payload.put("anonymousId", anonymousId);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("consentChannel", consentChannel);
        payload.put("consentStatus", consentStatus);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent);
        headers.put("X-Canvas-IP-Hash", ipHash);
        return catalog.submitMarketingForm(publicKey, payload, headers);
    }

    @Override
    public String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge) {
        return catalog.verifyWhatsApp(tenantId, mode, verifyToken, challenge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody) {
        return catalog.receiveWhatsApp(tenantId, signature, rawBody);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                          String signature, String rawBody) {
        return catalog.receiveAssetUploadCallback(tenantId, provider, timestamp, signature, rawBody);
    }

    public Map<String, Object> handleAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                         String signature, String rawBody) {
        return receiveAssetUploadCallback(tenantId, provider, timestamp, signature, rawBody);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                        String signature, String rawBody) {
        return catalog.receiveMonitoringWebhook(tenantId, sourceKey, timestamp, signature, rawBody);
    }

    public Map<String, Object> ingestMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                       String signature, String rawBody) {
        return receiveMonitoringWebhook(tenantId, sourceKey, timestamp, signature, rawBody);
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static Map<String, String> safeHeaders(Map<String, String> headers) {
        return headers == null ? Map.of() : headers;
    }
}
