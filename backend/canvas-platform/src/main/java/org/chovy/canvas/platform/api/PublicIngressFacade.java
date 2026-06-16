package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供公开入口、第三方回调和营销表单提交能力的应用入口。
 */
public interface PublicIngressFacade {

    /**
     * 查询公开营销表单配置。
     *
     * @param publicKey 公开访问键
     * @return 表单配置记录
     */
    Map<String, Object> publicMarketingForm(String publicKey);

    /**
     * 提交公开营销表单数据。
     *
     * @param publicKey 公开访问键
     * @param payload 表单提交内容
     * @param headers 请求头快照
     * @return 表单提交结果
     */
    Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                            Map<String, String> headers);

    /**
     * 校验 WhatsApp webhook 接入挑战。
     *
     * @param tenantId 租户标识
     * @param mode webhook 校验模式
     * @param verifyToken 校验令牌
     * @param challenge 平台下发的挑战字符串
     * @return 校验成功后返回的挑战字符串
     */
    String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge);

    /**
     * 接收 WhatsApp webhook 事件。
     *
     * @param tenantId 租户标识
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 解析后的事件记录列表
     */
    List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody);

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
    Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                   String signature, String rawBody);

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
    Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                 String signature, String rawBody);
}
