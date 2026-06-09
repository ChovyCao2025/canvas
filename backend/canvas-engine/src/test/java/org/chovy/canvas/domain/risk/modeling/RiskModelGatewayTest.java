package org.chovy.canvas.domain.risk.modeling;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RiskModelGatewayTest {

    private final RiskModelRegistryService registry = new RiskModelRegistryService();
    private final RecordingClient client = new RecordingClient();
    private final RiskModelGateway gateway = new RiskModelGateway(registry, client);

    @Test
    void selectsLatestActiveModelVersion() {
        registry.register(model("payment-risk", 1, true));
        registry.register(model("payment-risk", 2, true));
        client.response = """
                {"score":87,"explanations":["velocity","device"],"modelVersion":2}
                """;

        RiskModelResult result = gateway.score(request("payment-risk"));

        assertThat(result.modelVersion()).isEqualTo(2);
        assertThat(client.calls.getFirst().endpoint()).isEqualTo("https://model.example.com/payment-risk/v2");
    }

    @Test
    void parsesScoreAndExplanations() {
        registry.register(model("payment-risk", 3, true));
        client.response = """
                {"score":91.5,"explanations":["chargeback_cluster","ip_velocity"],"modelVersion":3}
                """;

        RiskModelResult result = gateway.score(request("payment-risk"));

        assertThat(result.score()).isEqualTo(92);
        assertThat(result.explanations()).containsExactly("chargeback_cluster", "ip_velocity");
        assertThat(result.fallbackUsed()).isFalse();
    }

    @Test
    void timeoutUsesConfiguredFallback() {
        registry.register(model("payment-risk", 1, true).withFallbackScore(35));
        client.timeout = true;

        RiskModelResult result = gateway.score(request("payment-risk"));

        assertThat(result.score()).isEqualTo(35);
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.explanations()).contains("MODEL_TIMEOUT");
    }

    @Test
    void doesNotSendRawPiiUnlessApprovedByRegistry() {
        registry.register(model("payment-risk", 1, false));
        client.response = "{\"score\":50,\"explanations\":[],\"modelVersion\":1}";

        gateway.score(request("payment-risk"));

        assertThat(client.calls.getFirst().payload().toString())
                .doesNotContain("user@example.com")
                .doesNotContain("+15551234567")
                .contains("u***3")
                .contains("***4567");
    }

    @Test
    void sendsRawPiiWhenRegistryExplicitlyApprovesIt() {
        registry.register(model("payment-risk", 1, true));
        client.response = "{\"score\":50,\"explanations\":[],\"modelVersion\":1}";

        gateway.score(request("payment-risk"));

        assertThat(client.calls.getFirst().payload().toString())
                .contains("user@example.com")
                .contains("+15551234567");
    }

    private RiskModelDefinition model(String modelKey, int version, boolean piiApproved) {
        return new RiskModelDefinition(
                modelKey,
                version,
                true,
                "https://model.example.com/" + modelKey + "/v" + version,
                Duration.ofMillis(50),
                0,
                piiApproved,
                Map.of("score", "NUMBER"),
                Map.of("score", "NUMBER", "explanations", "STRING_ARRAY"));
    }

    private RiskModelRequest request(String modelKey) {
        return new RiskModelRequest(
                7L,
                modelKey,
                Map.of("amount", 199),
                orderedMap("userId", "user-123", "email", "user@example.com", "phone", "+15551234567"));
    }

    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static final class RecordingClient implements RiskModelClient {
        private final List<RiskModelClientCall> calls = new java.util.ArrayList<>();
        private String response = "{\"score\":0,\"explanations\":[],\"modelVersion\":1}";
        private boolean timeout;

        @Override
        public String score(RiskModelClientCall call) {
            calls.add(call);
            if (timeout) {
                throw new RiskModelTimeoutException(call.modelKey());
            }
            return response;
        }
    }
}
