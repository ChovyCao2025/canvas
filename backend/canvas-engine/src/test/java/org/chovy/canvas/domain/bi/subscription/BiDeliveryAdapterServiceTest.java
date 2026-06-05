package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BiDeliveryAdapterServiceTest {

    @Test
    void deliverPostsLarkWebhookTextPayload() {
        TestDeliveryAdapterService service = new TestDeliveryAdapterService(200, "ok");

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "ALERT",
                41L,
                "success-rate-alert",
                "DATASET",
                11L,
                "LARK",
                Map.of("larkWebhookUrl", "https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                Map.of("title", "Success Rate Alert", "message", "BI alert threshold matched", "url", "/bi"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(service.url).isEqualTo("https://open.feishu.cn/open-apis/bot/v2/hook/abc");
        assertThat(service.body).containsEntry("msg_type", "text");
        assertThat((Map<?, ?>) service.body.get("content"))
                .extracting(content -> content.get("text"))
                .asString()
                .contains("Success Rate Alert", "BI alert threshold matched");
    }

    @Test
    void deliverGenericWebhookFailsOnNonSuccessHttp() {
        TestDeliveryAdapterService service = new TestDeliveryAdapterService(500, "server error");

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "WEBHOOK",
                Map.of("webhookUrl", "https://example.test/webhook"),
                Map.of("title", "Canvas Daily", "message", "BI subscription delivery is ready"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.message()).contains("HTTP 500");
        assertThat(service.body).containsEntry("event", "BI_DELIVERY");
        assertThat(service.body).containsEntry("jobKey", "canvas-daily");
    }

    @Test
    void deliverPostsWeComWebhookTextPayload() {
        TestDeliveryAdapterService service = new TestDeliveryAdapterService(200, "ok");

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "WECOM",
                Map.of("wecomWebhookUrl", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc"),
                Map.of("title", "Canvas Daily", "message", "BI subscription delivery is ready", "url", "/bi"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(service.url).contains("qyapi.weixin.qq.com");
        assertThat(service.body).containsEntry("msgtype", "text");
        assertThat((Map<?, ?>) service.body.get("text"))
                .extracting(text -> text.get("content"))
                .asString()
                .contains("Canvas Daily", "BI subscription delivery is ready");
    }

    @Test
    void deliverEmailUsesConfiguredMailSender() {
        FakeEmailClient emailClient = new FakeEmailClient(true);
        BiDeliveryAdapterService service = new BiDeliveryAdapterService(
                WebClient.builder(),
                emailClient,
                "bi@example.test",
                new ObjectMapper());

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "EMAIL",
                Map.of("emails", "alice@example.test; bob@example.test", "users", "CURRENT_USER"),
                Map.of("title", "Canvas Daily", "message", "BI subscription delivery is ready", "url", "/bi"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(emailClient.request.from()).isEqualTo("bi@example.test");
        assertThat(emailClient.request.to()).containsExactly("alice@example.test", "bob@example.test");
        assertThat(emailClient.request.subject()).isEqualTo("Canvas Daily");
        assertThat(emailClient.request.text()).contains("BI subscription delivery is ready");
    }

    @Test
    void deliverEmailPassesGeneratedAttachmentsToMailSender() {
        FakeEmailClient emailClient = new FakeEmailClient(true);
        BiDeliveryAdapterService service = new BiDeliveryAdapterService(
                WebClient.builder(),
                emailClient,
                "bi@example.test",
                new ObjectMapper());

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "EMAIL",
                Map.of("emails", "alice@example.test"),
                Map.of("title", "Canvas Daily", "message", "BI subscription delivery is ready", "url", "/bi"),
                null,
                "alice",
                List.of(new BiEmailAttachment(
                        "canvas-daily.csv",
                        "text/csv; charset=UTF-8",
                        "key,value\njobKey,canvas-daily\n".getBytes(StandardCharsets.UTF_8)))));

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(result.message()).contains("1 attachment");
        assertThat(emailClient.request.attachments()).singleElement().satisfies(attachment -> {
            assertThat(attachment.fileName()).isEqualTo("canvas-daily.csv");
            assertThat(new String(attachment.bytes(), StandardCharsets.UTF_8)).contains("canvas-daily");
        });
    }

    @Test
    void deliverEmailPendingWhenRecipientsMissing() {
        FakeEmailClient emailClient = new FakeEmailClient(true);
        BiDeliveryAdapterService service = new BiDeliveryAdapterService(
                WebClient.builder(),
                emailClient,
                "bi@example.test",
                new ObjectMapper());

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "EMAIL",
                Map.of("users", "CURRENT_USER"),
                Map.of("title", "Canvas Daily"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("PENDING_ADAPTER");
        assertThat(result.message()).contains("recipients");
    }

    @Test
    void deliverPendingWhenWebhookUrlMissing() {
        BiDeliveryAdapterService service = new BiDeliveryAdapterService(WebClient.builder(), new ObjectMapper());

        BiDeliveryAdapterResult result = service.deliver(new BiDeliveryAdapterRequest(
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                "DASHBOARD",
                21L,
                "WEBHOOK",
                Map.of("channels", "WEBHOOK"),
                Map.of("title", "Canvas Daily"),
                null,
                "alice"));

        assertThat(result.status()).isEqualTo("PENDING_ADAPTER");
    }

    private static final class TestDeliveryAdapterService extends BiDeliveryAdapterService {
        private final int status;
        private final String responseBody;
        private String url;
        private Map<String, Object> body;

        private TestDeliveryAdapterService(int status, String responseBody) {
            super(WebClient.builder(), new ObjectMapper());
            this.status = status;
            this.responseBody = responseBody;
        }

        @Override
        protected ResponseEntity<String> postJson(String url, Map<String, Object> body) {
            this.url = url;
            this.body = body;
            return new ResponseEntity<>(responseBody, HttpStatus.valueOf(status));
        }
    }

    private static final class FakeEmailClient implements BiEmailDeliveryClient {
        private final boolean configured;
        private BiEmailDeliveryRequest request;

        private FakeEmailClient(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean configured() {
            return configured;
        }

        @Override
        public void send(BiEmailDeliveryRequest request) {
            this.request = request;
        }
    }
}
