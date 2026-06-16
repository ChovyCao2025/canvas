package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.platform.api.PublicIngressFacade;
import org.chovy.canvas.platform.domain.PublicIngressCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公开入口应用服务，负责营销表单提交和第三方 webhook 回调接入。
 */
@Service
public class PublicIngressApplicationService implements PublicIngressFacade {

    /**
     * 保存公开入口配置、提交记录和回调事件的目录。
     */
    private final PublicIngressCatalog catalog;

    /**
     * 使用默认内存目录创建公开入口应用服务。
     */
    public PublicIngressApplicationService() {
        this(new PublicIngressCatalog());
    }

    /**
     * 使用指定目录创建公开入口应用服务。
     *
     * @param catalog 公开入口目录
     */
    public PublicIngressApplicationService(PublicIngressCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询公开营销表单配置。
     *
     * @param publicKey 公开访问键
     * @return 表单配置记录
     */
    @Override
    public Map<String, Object> publicMarketingForm(String publicKey) {
        return catalog.publicMarketingForm(publicKey);
    }

    /**
     * 提交公开营销表单。
     *
     * @param publicKey 公开访问键
     * @param payload 表单提交内容
     * @param headers 请求头快照
     * @return 表单提交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                                   Map<String, String> headers) {
        return catalog.submitMarketingForm(publicKey, safePayload(payload), safeHeaders(headers));
    }

    /**
     * 兼容按字段传递的营销表单提交入口。
     *
     * @param publicKey 公开访问键
     * @param response 表单回答内容
     * @param utm UTM 归因参数
     * @param anonymousId 匿名访客标识
     * @param idempotencyKey 幂等键
     * @param consentChannel 授权渠道
     * @param consentStatus 授权状态
     * @param userAgent 用户代理
     * @param ipHash IP 哈希值
     * @return 表单提交结果
     */
    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> response,
                                                   Map<String, Object> utm, String anonymousId,
                                                   String idempotencyKey, String consentChannel,
                                                   String consentStatus, String userAgent, String ipHash) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // 将旧调用形态组装成统一 payload，保持目录层只处理一种提交结构。
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

    /**
     * 校验 WhatsApp webhook 接入挑战。
     *
     * @param tenantId 租户标识
     * @param mode webhook 校验模式
     * @param verifyToken 校验令牌
     * @param challenge 平台下发的挑战字符串
     * @return 校验成功后返回的挑战字符串
     */
    @Override
    public String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge) {
        return catalog.verifyWhatsApp(tenantId, mode, verifyToken, challenge);
    }

    /**
     * 接收 WhatsApp webhook 事件。
     *
     * @param tenantId 租户标识
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 解析后的事件记录列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody) {
        return catalog.receiveWhatsApp(tenantId, signature, rawBody);
    }

    /**
     * 接收素材上传回调。
     *
     * @param tenantId 租户标识
     * @param provider 素材供应方
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 回调处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                          String signature, String rawBody) {
        return catalog.receiveAssetUploadCallback(tenantId, provider, timestamp, signature, rawBody);
    }

    /**
     * 兼容旧命名的素材上传回调入口。
     *
     * @param tenantId 租户标识
     * @param provider 素材供应方
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 回调处理结果
     */
    public Map<String, Object> handleAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                         String signature, String rawBody) {
        return receiveAssetUploadCallback(tenantId, provider, timestamp, signature, rawBody);
    }

    /**
     * 接收监控系统 webhook。
     *
     * @param tenantId 租户标识
     * @param sourceKey 监控来源键
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return webhook 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                        String signature, String rawBody) {
        return catalog.receiveMonitoringWebhook(tenantId, sourceKey, timestamp, signature, rawBody);
    }

    /**
     * 兼容旧命名的监控 webhook 入口。
     *
     * @param tenantId 租户标识
     * @param sourceKey 监控来源键
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return webhook 处理结果
     */
    public Map<String, Object> ingestMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                       String signature, String rawBody) {
        return receiveMonitoringWebhook(tenantId, sourceKey, timestamp, signature, rawBody);
    }

    /**
     * 将空请求体归一为空 Map。
     *
     * @param payload 原始请求体
     * @return 非空请求体
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 将空请求头归一为空 Map。
     *
     * @param headers 原始请求头
     * @return 非空请求头
     */
    private static Map<String, String> safeHeaders(Map<String, String> headers) {
        return headers == null ? Map.of() : headers;
    }
}
