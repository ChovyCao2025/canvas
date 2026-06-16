package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.domain.PublicIngressCatalog;
import org.junit.jupiter.api.Test;

/**
 * 覆盖公开入口应用服务的营销表单和 webhook 兼容行为。
 */
class PublicIngressApplicationServiceTest {

    /**
     * 验证公开营销、会话、素材和监控入口输出确定性数据。
     */
    @Test
    void exposesDeterministicPublicMarketingConversationAssetAndMonitoringIngress() {
        PublicIngressApplicationService service = new PublicIngressApplicationService(new PublicIngressCatalog());

        assertThat(service.publicMarketingForm("lead-capture"))
                .containsEntry("publicKey", "lead-capture")
                .containsEntry("active", true)
                .containsEntry("formName", "Public form lead-capture");

        assertThat(service.submitMarketingForm("lead-capture",
                Map.of("email", "buyer@example.test"), Map.of("utm_source", "newsletter"),
                "anon-1", "idem-1", "web", "granted", "JUnit", "hashed-ip"))
                .containsEntry("publicKey", "lead-capture")
                .containsEntry("accepted", true)
                .containsEntry("idempotencyKey", "idem-1")
                .containsEntry("consentStatus", "granted");

        assertThat(service.verifyWhatsApp(42L, "subscribe", "tenant-token", "challenge-1"))
                .isEqualTo("challenge-1");

        List<Map<String, Object>> responses = service.receiveWhatsApp(42L, "sha256=test", """
                {"entry":[{"id":"wa-entry"}]}
                """);
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst())
                .containsEntry("tenantId", 42L)
                .containsEntry("channel", "WHATSAPP")
                .containsEntry("source", "whatsapp-webhook");

        assertThat(service.handleAssetUploadCallback(42L, "s3", "100", "sig", """
                {"uploadToken":"upload-1","provider":"s3","status":"READY"}
                """))
                .containsEntry("tenantId", 42L)
                .containsEntry("provider", "s3")
                .containsEntry("uploadToken", "upload-1")
                .containsEntry("status", "READY");

        assertThat(service.ingestMonitoringWebhook(42L, "reviews", "100", "sig", """
                {"eventId":"evt-1","sentiment":"negative"}
                """))
                .containsEntry("tenantId", 42L)
                .containsEntry("sourceKey", "reviews")
                .containsEntry("accepted", true);
    }

    /**
     * 验证公开入口错误请求会被兼容性校验拒绝。
     */
    @Test
    void validatesPublicIngressInputsForCompatibilityBadRequests() {
        PublicIngressApplicationService service = new PublicIngressApplicationService(new PublicIngressCatalog());

        assertThatThrownBy(() -> service.publicMarketingForm(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publicKey is required");

        assertThatThrownBy(() -> service.handleAssetUploadCallback(42L, "s3", "100", "sig", """
                {"provider":"oss"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("asset upload provider does not match route");

        assertThatThrownBy(() -> service.receiveWhatsApp(42L, "sig", "not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("whatsapp webhook payload must be JSON");
    }
}
