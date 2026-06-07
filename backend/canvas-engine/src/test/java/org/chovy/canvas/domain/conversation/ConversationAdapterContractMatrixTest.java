package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationAdapterContractMatrixTest {

    @Test
    void contractMatrixCoversEveryProviderAdapter() {
        assertThat(ConversationAdapterContractSupport.contractKeys(cases()))
                .containsExactlyInAnyOrderElementsOf(ConversationAdapterContractSupport.providerAdapterKeys());
    }

    @Test
    void contractMatrixIncludesFixtureBackedCases() {
        assertThat(cases())
                .extracting(ConversationAdapterContractSupport.RawContractCase::name)
                .contains("whatsapp text fixture", "whatsapp interactive fixture");
    }

    @Test
    void contractFixturesCoverEveryProviderAdapter() {
        assertThat(ConversationAdapterContractSupport.contractKeys(ConversationAdapterContractSupport.fixtureCases(
                ConversationAdapterContractSupport.providerAdapterRegistry())))
                .containsExactlyInAnyOrderElementsOf(ConversationAdapterContractSupport.providerAdapterKeys());
    }

    @Test
    void contractMatrixUsesDiscoveredFixturesAsRawPayloadCases() {
        assertThat(cases())
                .extracting(ConversationAdapterContractSupport.RawContractCase::resourceName)
                .containsExactlyElementsOf(ConversationAdapterContractSupport.fixtureResources());
    }

    @Test
    void contractFixtureDiscoveryDefinesEveryFixtureResource() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .extracting(ConversationAdapterContractSupport.FixtureDefinition::resourceName)
                .containsExactlyElementsOf(ConversationAdapterContractSupport.discoveredFixtureResources());
    }

    @Test
    void contractFixtureResourcesAreUnique() {
        assertThat(ConversationAdapterContractSupport.fixtureResources()).doesNotHaveDuplicates();
    }

    @Test
    void contractFixtureIndexFileIsRetired() {
        ConversationAdapterContractSupport.assertFixtureIndexFileIsAbsent();
    }

    @Test
    void contractFixturesDeclareRequiredMetadata() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions()).allSatisfy(fixture -> {
            assertThat(fixture.resourceName()).startsWith("conversation/adapter-contracts/");
            assertThat(fixture.name()).isNotBlank();
            assertThat(fixture.adapterKey()).isNotBlank();
            assertThat(fixture.rawPayload()).isNotEmpty();
            assertThat(fixture.expectedChannel()).isNotBlank();
            assertThat(fixture.expectedProvider()).isNotBlank();
            assertThat(fixture.expectedMessageType()).isNotBlank();
            assertThat(fixture.expectedExternalMessageId()).isNotBlank();
            assertThat(fixture.expectedEventId()).isNotBlank();
            assertThat(fixture.expectedAttributes()).containsEntry("adapter", fixture.expectedChannel());
            assertThat(fixture.missingPayloadMessage()).isNotBlank();
        });
    }

    @Test
    void contractFixtureResourceNamesStartWithAdapterKeyPrefix() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertFixtureResourceNameStartsWithAdapterKeyPrefix);
    }

    @Test
    void contractFixtureAdapterKeysUseCanonicalAliases() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertFixtureAdapterKeyUsesCanonicalAlias);
    }

    @Test
    void contractFixturesCoverTextAndInteractiveMessageTypesForEveryProviderAdapter() {
        Map<String, Set<String>> messageTypesByAdapter = ConversationAdapterContractSupport.fixtureDefinitions()
                .stream()
                .collect(Collectors.groupingBy(
                        fixture -> fixture.adapterKey().trim().toUpperCase(Locale.ROOT),
                        Collectors.mapping(ConversationAdapterContractSupport.FixtureDefinition::expectedMessageType,
                                Collectors.toSet())));

        assertThat(ConversationAdapterContractSupport.providerAdapterKeys()).allSatisfy(adapterKey ->
                assertThat(messageTypesByAdapter.get(adapterKey))
                        .as("%s adapter fixture message types", adapterKey)
                        .contains("TEXT", "INTERACTIVE"));
    }

    @Test
    void contractFixturesMatchRawPayloadShapeToExpectedMessageType() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertRawPayloadShapeMatchesExpectedMessageType);
    }

    @Test
    void contractFixturesDeclareExpectedTextContent() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions()).allSatisfy(fixture ->
                assertThat(fixture.expectedText())
                        .as("%s expectedText", fixture.resourceName())
                        .isNotBlank());
    }

    @Test
    void contractFixtureExpectedTextBindsToRawPayloadContent() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertExpectedTextBindsToRawPayloadContent);
    }

    @Test
    void contractFixtureExpectedRoutingFieldsBindToRawPayload() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertExpectedRoutingFieldsBindToRawPayload);
    }

    @Test
    void contractFixtureExpectedAttributesBindToRawPayload() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertExpectedAttributesBindToRawPayload);
    }

    @Test
    void contractFixtureProviderSpecificRawFieldsBecomeExpectedAttributes() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderSpecificRawPayloadFieldsBecomeExpectedAttributes);
    }

    @Test
    void contractFixturesDeclareNormalizedExpectedFields() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertExpectedFieldsAreNormalized);
    }

    @Test
    void contractFixturesUseSupportedMessageTypes() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions()).allSatisfy(fixture ->
                assertThat(ConversationAdapterContractSupport.supportedMessageTypes())
                        .as("%s expectedMessageType", fixture.resourceName())
                        .contains(fixture.expectedMessageType()));
    }

    @Test
    void providerAdaptersDeclareConcretePayloadTypes() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterDeclaresConcretePayloadType);
    }

    @Test
    void providerAdapterPayloadTypesImplementProviderPayloadContract() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterPayloadTypeImplementsProviderPayloadContract);
    }

    @Test
    void providerAdapterClassAndPayloadNamesMatchAdapterKeys() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterNamesMatchAdapterKey);
    }

    @Test
    void providerAdapterKeysAreNormalizedConstants() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterKeyIsNormalizedConstant);
    }

    @Test
    void providerAdapterKeysAreUnique() {
        assertThat(ConversationAdapterContractSupport.providerAdapterKeyDeclarations()).doesNotHaveDuplicates();
    }

    @Test
    void providerAdaptersUseSharedProviderBaseClass() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterUsesSharedProviderBaseClass);
    }

    @Test
    void providerAdaptersDoNotDeclareCustomIngressConstruction() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterDoesNotDeclareCustomIngressConstruction);
    }

    @Test
    void providerAdaptersUseConstructorDeclaredProviderMapping() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterUsesConstructorDeclaredProviderMapping);
    }

    @Test
    void providerAdaptersAreStatelessNoArgDefinitions() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterIsStatelessNoArgDefinition);
    }

    @Test
    void providerAdaptersAreSpringComponents() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterIsSpringComponent);
    }

    @Test
    void providerAdapterPayloadTypesDeclareRequiredRoutingComponents() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values())
                .allSatisfy(ConversationAdapterContractSupport::assertProviderAdapterPayloadTypeDeclaresRoutingComponents);
    }

    @Test
    void contractFixtureRawPayloadKeysBindToPayloadRecordComponents() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions()).allSatisfy(fixture ->
                ConversationAdapterContractSupport.assertFixtureRawPayloadKeysBindToPayloadRecord(
                        fixture,
                        ConversationAdapterContractSupport.providerAdapterRegistry()));
    }

    @Test
    void contractFixturesCoverEveryProviderSpecificPayloadComponent() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values()).allSatisfy(adapter ->
                ConversationAdapterContractSupport.assertProviderSpecificPayloadComponentsCoveredByFixtures(
                        adapter,
                        ConversationAdapterContractSupport.fixtureDefinitions()));
    }

    @Test
    void contractFixturesCoverCommonSessionFieldsForEveryProviderAdapter() {
        assertThat(ConversationAdapterContractSupport.providerAdapterRegistry().values()).allSatisfy(adapter ->
                ConversationAdapterContractSupport.assertCommonSessionFieldsCoveredByFixtures(
                        adapter,
                        ConversationAdapterContractSupport.fixtureDefinitions()));
    }

    @Test
    void contractFixtureRawPayloadsDeclareRoutingKeys() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions()).allSatisfy(fixture ->
                assertThat(fixture.rawPayload())
                        .containsKeys(ConversationAdapterContractSupport.requiredRawPayloadRoutingKeys()
                                .toArray(String[]::new)));
    }

    @Test
    void contractFixtureRawPayloadRoutingValuesAreUsableText() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .allSatisfy(ConversationAdapterContractSupport::assertRawPayloadRoutingValuesAreUsable);
    }

    @Test
    void contractFixtureNamesAreUnique() {
        assertThat(ConversationAdapterContractSupport.fixtureDefinitions())
                .extracting(ConversationAdapterContractSupport.FixtureDefinition::name)
                .doesNotHaveDuplicates();
    }

    @Test
    void rawPayloadIngressDynamicTestsIncludeFixtureResourceNames() {
        assertThat(providerAdaptersSatisfyRawPayloadIngressContract().map(DynamicTest::getDisplayName).toList())
                .allSatisfy(displayName -> assertThat(displayName)
                        .contains(" [conversation/adapter-contracts/")
                        .endsWith(".json]"));
    }

    @Test
    void rawPayloadIngressContractRejectsUserIdThatDoesNotBindToRawPayload() {
        ConversationReplyAdapter<Map<String, Object>> adapter = new ConversationReplyAdapter<>() {
            @Override
            public String adapterKey() {
                return "TEST";
            }

            @Override
            public ConversationIngressReq toIngress(Map<String, Object> payload, ConversationAdapterContext context) {
                if (payload == null) {
                    throw new IllegalArgumentException("test payload is required");
                }
                return new ConversationIngressReq(
                        null,
                        null,
                        null,
                        "hardcoded-user",
                        "TEST",
                        "PROVIDER",
                        "external-1",
                        "event-1",
                        "TEXT",
                        "hello",
                        null,
                        Map.of("adapter", "TEST"),
                        null);
            }
        };
        ConversationAdapterContractSupport.RawContractCase contractCase =
                new ConversationAdapterContractSupport.RawContractCase(
                        "test hardcoded user",
                        "conversation/adapter-contracts/test-hardcoded-user.json",
                        adapter,
                        "TEST",
                        Map.of(
                                "userId", "raw-user",
                                "provider", "provider",
                                "externalMessageId", "external-1",
                                "eventId", "event-1",
                                "text", "hello"),
                        "TEST",
                        "PROVIDER",
                        "TEXT",
                        "hello",
                        "external-1",
                        "event-1",
                        Map.of("adapter", "TEST"),
                        "test payload is required");

        assertThatThrownBy(() -> ConversationAdapterContractSupport.assertRawIngressContract(contractCase))
                .hasMessageContaining("userId")
                .hasMessageContaining("rawPayload.userId");
    }

    @Test
    void rawPayloadIngressContractRejectsCommonFieldsThatDoNotBindToRawPayload() {
        ConversationReplyAdapter<Map<String, Object>> adapter = new ConversationReplyAdapter<>() {
            @Override
            public String adapterKey() {
                return "TEST";
            }

            @Override
            public ConversationIngressReq toIngress(Map<String, Object> payload, ConversationAdapterContext context) {
                if (payload == null) {
                    throw new IllegalArgumentException("test payload is required");
                }
                return new ConversationIngressReq(
                        99L,
                        100L,
                        "wrong-exec",
                        "raw-user",
                        "TEST",
                        "PROVIDER",
                        "external-1",
                        "event-1",
                        "TEXT",
                        "hello",
                        "WRONG_INTENT",
                        Map.of("adapter", "TEST"),
                        null);
            }
        };
        ConversationAdapterContractSupport.RawContractCase contractCase =
                new ConversationAdapterContractSupport.RawContractCase(
                        "test hardcoded common fields",
                        "conversation/adapter-contracts/test-hardcoded-common-fields.json",
                        adapter,
                        "TEST",
                        Map.of(
                                "canvasId", 10,
                                "versionId", 20,
                                "executionId", "exec-1",
                                "userId", "raw-user",
                                "provider", "provider",
                                "externalMessageId", "external-1",
                                "eventId", "event-1",
                                "text", "hello",
                                "intent", "GREETING"),
                        "TEST",
                        "PROVIDER",
                        "TEXT",
                        "hello",
                        "external-1",
                        "event-1",
                        Map.of("adapter", "TEST"),
                        "test payload is required");

        assertThatThrownBy(() -> ConversationAdapterContractSupport.assertRawIngressContract(contractCase))
                .hasMessageContaining("canvasId")
                .hasMessageContaining("rawPayload.canvasId");
    }

    @Test
    void rawPayloadIngressContractRejectsOccurredAtThatDoesNotBindToRawPayload() {
        ConversationReplyAdapter<Map<String, Object>> adapter = new ConversationReplyAdapter<>() {
            @Override
            public String adapterKey() {
                return "TEST";
            }

            @Override
            public ConversationIngressReq toIngress(Map<String, Object> payload, ConversationAdapterContext context) {
                if (payload == null) {
                    throw new IllegalArgumentException("test payload is required");
                }
                return new ConversationIngressReq(
                        null,
                        null,
                        null,
                        "raw-user",
                        "TEST",
                        "PROVIDER",
                        "external-1",
                        "event-1",
                        "TEXT",
                        "hello",
                        null,
                        Map.of("adapter", "TEST"),
                        LocalDateTime.of(2026, 6, 3, 4, 0));
            }
        };
        ConversationAdapterContractSupport.RawContractCase contractCase =
                new ConversationAdapterContractSupport.RawContractCase(
                        "test hardcoded occurredAt",
                        "conversation/adapter-contracts/test-hardcoded-occurred-at.json",
                        adapter,
                        "TEST",
                        Map.of(
                                "userId", "raw-user",
                                "provider", "provider",
                                "externalMessageId", "external-1",
                                "eventId", "event-1",
                                "text", "hello",
                                "occurredAt", "2026-06-02T04:00:00"),
                        "TEST",
                        "PROVIDER",
                        "TEXT",
                        "hello",
                        "external-1",
                        "event-1",
                        Map.of("adapter", "TEST"),
                        "test payload is required");

        assertThatThrownBy(() -> ConversationAdapterContractSupport.assertRawIngressContract(contractCase))
                .hasMessageContaining("occurredAt")
                .hasMessageContaining("rawPayload.occurredAt");
    }

    @Test
    void rawPayloadIngressContractRejectsBroadMissingPayloadMessageFixture() {
        ConversationReplyAdapter<Map<String, Object>> adapter = new ConversationReplyAdapter<>() {
            @Override
            public String adapterKey() {
                return "TEST";
            }

            @Override
            public ConversationIngressReq toIngress(Map<String, Object> payload, ConversationAdapterContext context) {
                if (payload == null) {
                    throw new IllegalArgumentException("test payload is required");
                }
                return new ConversationIngressReq(
                        null,
                        null,
                        null,
                        "raw-user",
                        "TEST",
                        "PROVIDER",
                        "external-1",
                        "event-1",
                        "TEXT",
                        "hello",
                        null,
                        Map.of("adapter", "TEST"),
                        null);
            }
        };
        ConversationAdapterContractSupport.RawContractCase contractCase =
                new ConversationAdapterContractSupport.RawContractCase(
                        "test broad missing payload message",
                        "conversation/adapter-contracts/test-broad-missing-payload-message.json",
                        adapter,
                        "TEST",
                        Map.of(
                                "userId", "raw-user",
                                "provider", "provider",
                                "externalMessageId", "external-1",
                                "eventId", "event-1",
                                "text", "hello"),
                        "TEST",
                        "PROVIDER",
                        "TEXT",
                        "hello",
                        "external-1",
                        "event-1",
                        Map.of("adapter", "TEST"),
                        "payload");

        assertThatThrownBy(() -> ConversationAdapterContractSupport.assertRawIngressContract(contractCase))
                .hasMessageContaining("Expecting message to be:")
                .hasMessageContaining("\"payload\"")
                .hasMessageContaining("test payload is required");
    }

    @TestFactory
    Stream<DynamicTest> providerAdaptersSatisfyRawPayloadIngressContract() {
        return cases().stream()
                .map(contractCase -> DynamicTest.dynamicTest(
                        contractCase.displayName(),
                        () -> ConversationAdapterContractSupport.assertRawIngressContract(contractCase)));
    }

    private List<ConversationAdapterContractSupport.RawContractCase> cases() {
        return ConversationAdapterContractSupport.fixtureCases(ConversationAdapterContractSupport.providerAdapterRegistry());
    }

    private List<String> caseNames(List<ConversationAdapterContractSupport.RawContractCase> contractCases) {
        return contractCases.stream()
                .map(ConversationAdapterContractSupport.RawContractCase::name)
                .toList();
    }
}
