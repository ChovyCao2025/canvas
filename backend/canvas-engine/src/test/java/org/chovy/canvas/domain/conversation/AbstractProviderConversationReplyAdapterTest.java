package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractProviderConversationReplyAdapterTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 6, 16, 30);

    @Test
    void exposesAdapterMetadataAndRejectsMissingPayloadBeforeMapping() {
        TestProviderAdapter adapter = new TestProviderAdapter();

        assertThat(adapter.adapterKey()).isEqualTo("TEST");
        assertThat(adapter.payloadType()).isEqualTo(TestProviderPayload.class);
        assertThatThrownBy(() -> adapter.toIngress(null, new ConversationAdapterContext(8L, "system")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test provider payload is required");
        assertThat(adapter.attributesMapped).isFalse();
        assertThat(adapter.fallbackMapped).isFalse();
        assertThat(adapter.markersMapped).isFalse();
    }

    @Test
    void buildsProviderIngressFromDefaultConstructorMapping() {
        TestProviderAdapter adapter = new TestProviderAdapter();
        TestProviderPayload payload = new TestProviderPayload(
                10L,
                20L,
                "exec-1",
                "user-1",
                " provider ",
                "external-1",
                "event-1",
                " provider message ",
                "reply-1",
                "Reply One",
                "GREETING",
                Map.of("locale", "en-US"),
                OCCURRED_AT);

        ConversationIngressReq req = adapter.toIngress(payload, new ConversationAdapterContext(8L, "system"));

        assertThat(adapter.attributesMapped).isFalse();
        assertThat(adapter.fallbackMapped).isFalse();
        assertThat(adapter.markersMapped).isFalse();
        assertThat(req.channel()).isEqualTo("TEST");
        assertThat(req.provider()).isEqualTo("PROVIDER");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("provider message");
        assertThat(req.userId()).isEqualTo("user-1");
        assertThat(req.attributes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "adapter", "TEST",
                "locale", "en-US"));
    }

    @Test
    void buildsProviderIngressFromConstructorMapping() {
        ConstructorMappedProviderAdapter adapter = new ConstructorMappedProviderAdapter();
        TestProviderPayload payload = new TestProviderPayload(
                10L,
                20L,
                "exec-1",
                "user-1",
                "provider",
                "external-1",
                "event-1",
                " ",
                "reply-1",
                "Reply One",
                "GREETING",
                Map.of("locale", "en-US"),
                OCCURRED_AT);

        ConversationIngressReq req = adapter.toIngress(payload, new ConversationAdapterContext(8L, "system"));

        assertThat(req.channel()).isEqualTo("DECLARATIVE");
        assertThat(req.messageType()).isEqualTo("INTERACTIVE");
        assertThat(req.text()).isEqualTo("Reply One");
        assertThat(req.attributes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "adapter", "DECLARATIVE",
                "locale", "en-US",
                "replyId", "reply-1",
                "replyLabel", "Reply One"));
    }

    @Test
    void providerMappingHooksArePrivateImplementationDetails() throws NoSuchMethodException {
        List<Method> mappingMethods = List.of(
                AbstractProviderConversationReplyAdapter.class.getDeclaredMethod(
                        "providerAttributes", ProviderConversationReplyPayload.class),
                AbstractProviderConversationReplyAdapter.class.getDeclaredMethod(
                        "fallbackText", ProviderConversationReplyPayload.class),
                AbstractProviderConversationReplyAdapter.class.getDeclaredMethod(
                        "interactiveMarkers", ProviderConversationReplyPayload.class));

        assertThat(mappingMethods).allSatisfy(method ->
                assertThat(Modifier.isPrivate(method.getModifiers()))
                        .as("%s should not be an override hook", method.getName())
                        .isTrue());
    }

    private static final class ConstructorMappedProviderAdapter
            extends AbstractProviderConversationReplyAdapter<TestProviderPayload> {

        private ConstructorMappedProviderAdapter() {
            super(
                    "DECLARATIVE",
                    TestProviderPayload.class,
                    "constructor mapped payload is required",
                    List.of(
                            providerAttribute("replyId", TestProviderPayload::replyId),
                            providerAttribute("replyLabel", TestProviderPayload::replyLabel)),
                    TestProviderPayload::replyLabel,
                    List.of(TestProviderPayload::replyId, TestProviderPayload::replyLabel));
        }
    }

    private static final class TestProviderAdapter
            extends AbstractProviderConversationReplyAdapter<TestProviderPayload> {

        private boolean attributesMapped;
        private boolean fallbackMapped;
        private boolean markersMapped;

        private TestProviderAdapter() {
            super("TEST", TestProviderPayload.class, "test provider payload is required");
        }
    }

    private record TestProviderPayload(
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String provider,
            String externalMessageId,
            String eventId,
            String text,
            String replyId,
            String replyLabel,
            String intent,
            Map<String, Object> attributes,
            LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
    }
}
