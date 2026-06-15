package org.chovy.canvas.web.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MetaOptionFacade;
import org.chovy.canvas.marketing.api.MetaOptionView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MetaOptionControllerCompatibilityTest {

    @Test
    void exposesOptionsAndBizLinesWithLegacyEnvelopeAndKeyLabelShape() {
        RecordingMetaOptionFacade facade = new RecordingMetaOptionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/meta/options?category=channel")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].key").isEqualTo("sms")
                .jsonPath("$.data[0].label").isEqualTo("SMS")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/meta/biz-lines")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("growth")
                .jsonPath("$.data[0].label").isEqualTo("Growth");

        assertThat(facade.optionCalls).containsExactly("channel@7", "biz_line@42");
    }

    @Test
    void batchOptionsPreserveFirstCategoryOrderAfterDeduplication() {
        RecordingMetaOptionFacade facade = new RecordingMetaOptionFacade();

        webClient(facade)
                .get()
                .uri("/meta/options/batch?categories=channel&categories=biz_line&categories=channel")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.channel[0].key").isEqualTo("sms")
                .jsonPath("$.data.biz_line[0].key").isEqualTo("growth");

        assertThat(facade.batchCategories).containsExactly("channel", "biz_line");
        assertThat(facade.batchTenantId).isEqualTo(7L);
    }

    @Test
    void exposesAbExperimentsAndGroupsWithLegacyMissingExperimentFallback() {
        RecordingMetaOptionFacade facade = new RecordingMetaOptionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/meta/ab-experiments")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("signup-copy")
                .jsonPath("$.data[0].label").isEqualTo("Signup Copy");

        client.get()
                .uri("/meta/ab-experiments/signup-copy/groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("control")
                .jsonPath("$.data[0].label").isEqualTo("Control");

        client.get()
                .uri("/meta/ab-experiments/missing/groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(0);

        assertThat(facade.groupKeys).containsExactly("signup-copy", "missing");
    }

    @Test
    void exposesRemainingLegacyMetaAliasesWithEnvelopeAndSpecializedFields() {
        RecordingMetaOptionFacade facade = new RecordingMetaOptionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/meta/ai-models?providerId=11")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].key").isEqualTo("gpt-4.1-mini")
                .jsonPath("$.data[0].label").isEqualTo("GPT-4.1 mini");

        client.get()
                .uri("/meta/ai-providers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("11")
                .jsonPath("$.data[0].label").isEqualTo("OpenAI (openai)");

        client.get()
                .uri("/meta/ai-templates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("9001")
                .jsonPath("$.data[0].label").isEqualTo("Winback (MARKETING)");

        client.get()
                .uri("/meta/coupon-types")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("DISCOUNT");

        client.get()
                .uri("/meta/behavior-strategy-types")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("ALLOW");

        client.get()
                .uri("/meta/api-definitions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].value").isEqualTo("send-coupon")
                .jsonPath("$.data[0].label").isEqualTo("Send coupon")
                .jsonPath("$.data[0].requestSchema").isEqualTo("[{\"name\":\"couponId\",\"type\":\"STRING\"}]");

        client.get()
                .uri("/meta/event-definitions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].value").isEqualTo("user.created")
                .jsonPath("$.data[0].requestSchema").isEqualTo("[{\"name\":\"source\",\"type\":\"STRING\"}]");

        client.get()
                .uri("/meta/context-fields")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].fieldKey").isEqualTo("userId")
                .jsonPath("$.data[0].sourceNodeType").isEqualTo("trigger");

        client.get()
                .uri("/meta/canvas-context-fields?eventCodes=user.created&apiKeys=send-coupon&outputPrefixes=coupon")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].fieldKey").isEqualTo("userId")
                .jsonPath("$.data[1].sourceNodeType").isEqualTo("EVENT_TRIGGER")
                .jsonPath("$.data[2].fieldKey").isEqualTo("coupon.status");

        client.get()
                .uri("/meta/identity-types?allowImport=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("phone")
                .jsonPath("$.data[0].label").isEqualTo("Phone");

        assertThat(facade.modelTenantId).isEqualTo(42L);
        assertThat(facade.modelProviderId).isEqualTo(11L);
        assertThat(facade.identityAllowImport).isEqualTo(0);
        assertThat(facade.optionCalls).contains("coupon_type@7", "behavior_strategy_type@7");
    }

    @Test
    void exposesMessageMqReachAndTaggerLegacyRoutesThroughMetaOptionFacade() {
        RecordingMetaOptionFacade facade = new RecordingMetaOptionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/meta/message-codes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data[0].key").isEqualTo("IN_APP_WELCOME");

        client.get()
                .uri("/meta/message-codes?type=MQ")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("MQ_COUPON_GRANTED");

        client.get()
                .uri("/meta/reach-scenes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("WELCOME");

        client.get()
                .uri("/meta/mq-topics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("canvas.user.changed");

        client.get()
                .uri("/meta/mq-definitions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].value").isEqualTo("MQ_COUPON_GRANTED")
                .jsonPath("$.data[0].label").isEqualTo("Coupon granted")
                .jsonPath("$.data[0].requestSchema").isEqualTo("[{\"name\":\"couponId\",\"type\":\"STRING\"}]");

        client.get()
                .uri("/meta/tagger-tags?type=offline")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("market_identity")
                .jsonPath("$.data[0].label").isEqualTo("Market identity");

        client.get()
                .uri("/meta/tagger-tag-values?tagCode=market_identity")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].key").isEqualTo("new_user")
                .jsonPath("$.data[0].label").isEqualTo("New user");

        assertThat(facade.optionCalls).containsExactly(
                "message_code_in_app@7",
                "message_code_mq@7",
                "reach_scene@7",
                "mq_topic_legacy@7");
        assertThat(facade.taggerTagType).isEqualTo("offline");
        assertThat(facade.taggerValueCode).isEqualTo("market_identity");
    }

    private static WebTestClient webClient(MetaOptionFacade facade) {
        return WebTestClient.bindToController(new MetaOptionController(facade)).build();
    }

    private static final class RecordingMetaOptionFacade implements MetaOptionFacade {
        private final List<String> optionCalls = new java.util.ArrayList<>();
        private List<String> batchCategories = List.of();
        private Long batchTenantId;
        private final List<String> groupKeys = new java.util.ArrayList<>();
        private Long modelTenantId;
        private Long modelProviderId;
        private Integer identityAllowImport;
        private String taggerTagType;
        private String taggerValueCode;

        @Override
        public Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories) {
            batchCategories = List.copyOf(categories);
            batchTenantId = tenantId;
            Map<String, List<MetaOptionView>> result = new LinkedHashMap<>();
            result.put("channel", List.of(new MetaOptionView("sms", "SMS")));
            result.put("biz_line", List.of(new MetaOptionView("growth", "Growth")));
            return result;
        }

        @Override
        public List<MetaOptionView> abExperiments(Long tenantId) {
            return List.of(new MetaOptionView("signup-copy", "Signup Copy"));
        }

        @Override
        public List<MetaOptionView> abExperimentGroups(Long tenantId, String key) {
            groupKeys.add(key);
            if ("signup-copy".equals(key)) {
                return List.of(new MetaOptionView("control", "Control"));
            }
            return List.of();
        }

        @Override
        public List<MetaOptionView> options(Long tenantId, String category) {
            optionCalls.add(category + "@" + tenantId);
            if ("biz_line".equals(category)) {
                return List.of(new MetaOptionView("growth", "Growth"));
            }
            if ("coupon_type".equals(category)) {
                return List.of(new MetaOptionView("DISCOUNT", "Discount coupon"));
            }
            if ("behavior_strategy_type".equals(category)) {
                return List.of(new MetaOptionView("ALLOW", "Allow"));
            }
            if ("message_code_in_app".equals(category)) {
                return List.of(new MetaOptionView("IN_APP_WELCOME", "In-app welcome"));
            }
            if ("message_code_mq".equals(category)) {
                return List.of(new MetaOptionView("MQ_COUPON_GRANTED", "Coupon granted"));
            }
            if ("reach_scene".equals(category)) {
                return List.of(new MetaOptionView("WELCOME", "Welcome journey"));
            }
            if ("mq_topic_legacy".equals(category)) {
                return List.of(new MetaOptionView("canvas.user.changed", "User changed"));
            }
            return List.of(new MetaOptionView("sms", "SMS"));
        }

        @Override
        public List<MetaOptionView> bizLines(Long tenantId) {
            return options(tenantId, "biz_line");
        }

        @Override
        public List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey) {
            return List.of(new MetaOptionView("send-coupon", "Send coupon"));
        }

        @Override
        public List<MetaOptionView> aiProviders(Long tenantId) {
            return List.of(new MetaOptionView("11", "OpenAI (openai)"));
        }

        @Override
        public List<MetaOptionView> aiTemplates(Long tenantId) {
            return List.of(new MetaOptionView("9001", "Winback (MARKETING)"));
        }

        @Override
        public List<MetaOptionView> aiModels(Long tenantId, Long providerId) {
            modelTenantId = tenantId;
            modelProviderId = providerId;
            return List.of(new MetaOptionView("gpt-4.1-mini", "GPT-4.1 mini"));
        }

        @Override
        public List<MetaOptionView> identityTypes(Integer allowImport) {
            identityAllowImport = allowImport;
            return List.of(new MetaOptionView("phone", "Phone"));
        }

        @Override
        public List<Map<String, Object>> apiDefinitions() {
            Map<String, Object> definition = new LinkedHashMap<>();
            definition.put("value", "send-coupon");
            definition.put("label", "Send coupon");
            definition.put("requestSchema", "[{\"name\":\"couponId\",\"type\":\"STRING\"}]");
            definition.put("includeContextPayload", 1);
            return List.of(definition);
        }

        @Override
        public List<Map<String, Object>> eventDefinitions() {
            Map<String, Object> definition = new LinkedHashMap<>();
            definition.put("value", "user.created");
            definition.put("label", "User created");
            definition.put("requestSchema", "[{\"name\":\"source\",\"type\":\"STRING\"}]");
            return List.of(definition);
        }

        @Override
        public List<Map<String, Object>> contextFields() {
            return List.of(field("userId", "User ID", "STRING", "trigger"));
        }

        @Override
        public List<Map<String, Object>> canvasContextFields(
                List<String> eventCodes,
                List<String> apiKeys,
                List<String> outputPrefixes) {
            return List.of(
                    field("userId", "User ID", "STRING", "trigger"),
                    field("source", "Source (User created)", "STRING", "EVENT_TRIGGER"),
                    field("coupon.status", "Status (Send coupon)", "STRING", "API_CALL"));
        }

        @Override
        public List<Map<String, Object>> mqDefinitions() {
            Map<String, Object> definition = new LinkedHashMap<>();
            definition.put("value", "MQ_COUPON_GRANTED");
            definition.put("label", "Coupon granted");
            definition.put("requestSchema", "[{\"name\":\"couponId\",\"type\":\"STRING\"}]");
            return List.of(definition);
        }

        @Override
        public List<MetaOptionView> taggerTags(String type) {
            taggerTagType = type;
            return List.of(new MetaOptionView("market_identity", "Market identity"));
        }

        @Override
        public List<MetaOptionView> taggerTagValues(String tagCode) {
            taggerValueCode = tagCode;
            return List.of(new MetaOptionView("new_user", "New user"));
        }

        private static Map<String, Object> field(String key, String name, String type, String source) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("fieldKey", key);
            field.put("fieldName", name);
            field.put("dataType", type);
            field.put("sourceNodeType", source);
            return field;
        }
    }
}
