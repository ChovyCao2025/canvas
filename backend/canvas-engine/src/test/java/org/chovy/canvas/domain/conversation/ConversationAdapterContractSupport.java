package org.chovy.canvas.domain.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ConversationAdapterContractSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String FIXTURE_DIR = "conversation/adapter-contracts/";
    private ConversationAdapterContractSupport() {
    }

    record RawContractCase(
            String name,
            String resourceName,
            ConversationReplyAdapter<?> adapter,
            String adapterKey,
            Map<String, Object> rawPayload,
            String expectedChannel,
            String expectedProvider,
            String expectedMessageType,
            String expectedText,
            String expectedExternalMessageId,
            String expectedEventId,
            Map<String, Object> expectedAttributes,
            String missingPayloadMessage) {

        RawContractCase {
            expectedAttributes = expectedAttributes == null ? Map.of() : Map.copyOf(expectedAttributes);
        }

        String displayName() {
            return name + " [" + resourceName + "]";
        }
    }

    private record FixtureContractCase(
            String name,
            String adapterKey,
            Map<String, Object> rawPayload,
            String expectedChannel,
            String expectedProvider,
            String expectedMessageType,
            String expectedText,
            String expectedExternalMessageId,
            String expectedEventId,
            Map<String, Object> expectedAttributes,
            String missingPayloadMessage) {
    }

    record FixtureDefinition(
            String resourceName,
            String name,
            String adapterKey,
            Map<String, Object> rawPayload,
            String expectedChannel,
            String expectedProvider,
            String expectedMessageType,
            String expectedText,
            String expectedExternalMessageId,
            String expectedEventId,
            Map<String, Object> expectedAttributes,
            String missingPayloadMessage) {

        FixtureDefinition {
            expectedAttributes = expectedAttributes == null ? Map.of() : Map.copyOf(expectedAttributes);
        }
    }

    static void assertRawIngressContract(RawContractCase contractCase) {
        ConversationIngressReq req = captureRawIngress(
                contractCase.adapter(),
                contractCase.adapterKey(),
                contractCase.rawPayload(),
                "operator-1");

        assertThat(req.channel()).isEqualTo(contractCase.expectedChannel());
        assertThat(req.provider()).isEqualTo(contractCase.expectedProvider());
        assertThat(req.messageType()).isEqualTo(contractCase.expectedMessageType());
        assertThat(req.text()).isEqualTo(contractCase.expectedText());
        assertThat(req.externalMessageId()).isEqualTo(contractCase.expectedExternalMessageId());
        assertThat(req.eventId()).isEqualTo(contractCase.expectedEventId());
        assertThat(contractCase.rawPayload())
                .as("%s rawPayload", contractCase.resourceName())
                .containsKey("userId");
        assertThat(req.userId())
                .as("%s userId must match rawPayload.userId", contractCase.resourceName())
                .isEqualTo(textValue(contractCase.rawPayload().get("userId")));
        assertOptionalRawPayloadLongField(contractCase, "canvasId", req.canvasId());
        assertOptionalRawPayloadLongField(contractCase, "versionId", req.versionId());
        assertOptionalRawPayloadTextField(contractCase, "executionId", req.executionId());
        assertOptionalRawPayloadTextField(contractCase, "intent", req.intent());
        assertOptionalRawPayloadDateTimeField(contractCase, "occurredAt", req.occurredAt());
        assertThat(req.attributes()).containsExactlyInAnyOrderEntriesOf(contractCase.expectedAttributes());
        assertRejectsMissingPayload(contractCase.adapter(), contractCase.missingPayloadMessage());
    }

    static List<RawContractCase> fixtureCases(Map<String, ConversationReplyAdapter<?>> adaptersByKey) {
        Map<String, ConversationReplyAdapter<?>> normalizedAdapters = normalizeAdapters(adaptersByKey);
        List<RawContractCase> contractCases = new ArrayList<>();
        for (FixtureDefinition fixture : fixtureDefinitions()) {
            String adapterKey = normalizeAdapterKey(fixture.adapterKey());
            ConversationReplyAdapter<?> adapter = normalizedAdapters.get(adapterKey);
            if (adapter == null) {
                throw new AssertionError("Conversation adapter fixture references an unregistered adapter: "
                        + adapterKey + " in " + fixture.resourceName());
            }
            contractCases.add(new RawContractCase(
                    fixture.name(),
                    fixture.resourceName(),
                    adapter,
                    fixture.adapterKey(),
                    fixture.rawPayload(),
                    fixture.expectedChannel(),
                    fixture.expectedProvider(),
                    fixture.expectedMessageType(),
                    fixture.expectedText(),
                    fixture.expectedExternalMessageId(),
                    fixture.expectedEventId(),
                    fixture.expectedAttributes(),
                    fixture.missingPayloadMessage()));
        }
        return List.copyOf(contractCases);
    }

    static List<FixtureDefinition> fixtureDefinitions() {
        List<FixtureDefinition> fixtures = new ArrayList<>();
        for (String fixtureResource : fixtureResources()) {
            FixtureContractCase fixture = readResource(fixtureResource, FixtureContractCase.class);
            fixtures.add(new FixtureDefinition(
                    fixtureResource,
                    fixture.name(),
                    fixture.adapterKey(),
                    fixture.rawPayload(),
                    fixture.expectedChannel(),
                    fixture.expectedProvider(),
                    fixture.expectedMessageType(),
                    fixture.expectedText(),
                    fixture.expectedExternalMessageId(),
                    fixture.expectedEventId(),
                    fixture.expectedAttributes(),
                    fixture.missingPayloadMessage()));
        }
        return List.copyOf(fixtures);
    }

    static List<String> requiredRawPayloadRoutingKeys() {
        return List.of("userId", "provider", "externalMessageId", "eventId");
    }

    static Set<String> commonProviderPayloadComponents() {
        return Set.of(
                "canvasId",
                "versionId",
                "executionId",
                "userId",
                "provider",
                "externalMessageId",
                "eventId",
                "text",
                "intent",
                "attributes",
                "occurredAt");
    }

    static Set<String> commonSessionRawPayloadFields() {
        return Set.of("canvasId", "versionId", "executionId", "intent", "occurredAt");
    }

    static Set<String> supportedMessageTypes() {
        return Set.of("TEXT", "IMAGE", "INTERACTIVE", "UNKNOWN");
    }

    static void assertFixtureResourceNameStartsWithAdapterKeyPrefix(FixtureDefinition fixture) {
        String expectedPrefix = normalizeAdapterKey(fixture.adapterKey()).toLowerCase(Locale.ROOT).replace('_', '-');
        String fileName = fixture.resourceName().substring(fixture.resourceName().lastIndexOf('/') + 1);
        assertThat(fileName)
                .as("%s file name must start with adapter key prefix", fixture.resourceName())
                .startsWith(expectedPrefix + "-");
    }

    static void assertFixtureAdapterKeyUsesCanonicalAlias(FixtureDefinition fixture) {
        assertThat(fixture.adapterKey())
                .as("%s adapterKey", fixture.resourceName())
                .isEqualTo(fixture.expectedChannel().toLowerCase(Locale.ROOT));
    }

    static void assertExpectedFieldsAreNormalized(FixtureDefinition fixture) {
        assertUppercaseTrimmedFixtureField(fixture, "expectedChannel", fixture.expectedChannel());
        assertUppercaseTrimmedFixtureField(fixture, "expectedProvider", fixture.expectedProvider());
        assertUppercaseTrimmedFixtureField(fixture, "expectedMessageType", fixture.expectedMessageType());
    }

    static void assertProviderAdapterDeclaresConcretePayloadType(ConversationReplyAdapter<?> adapter) {
        Class<?> payloadType = adapter.payloadType();
        assertThat(payloadType)
                .as("%s payloadType", adapter.adapterKey())
                .isNotNull()
                .isNotEqualTo(Object.class);
        assertThat(payloadType.isRecord())
                .as("%s payloadType must be a record: %s", adapter.adapterKey(), payloadType.getName())
                .isTrue();
    }

    static void assertProviderAdapterPayloadTypeImplementsProviderPayloadContract(ConversationReplyAdapter<?> adapter) {
        assertProviderAdapterDeclaresConcretePayloadType(adapter);
        Class<?> payloadType = adapter.payloadType();
        assertThat(ProviderConversationReplyPayload.class.isAssignableFrom(payloadType))
                .as("%s payloadType must implement %s: %s",
                        adapter.adapterKey(),
                        ProviderConversationReplyPayload.class.getSimpleName(),
                        payloadType.getName())
                .isTrue();
    }

    static void assertProviderAdapterNamesMatchAdapterKey(ConversationReplyAdapter<?> adapter) {
        String adapterKeyName = traceableNameToken(adapter.adapterKey());
        String adapterClassName = stripSuffix(adapter.getClass().getSimpleName(), "ConversationReplyAdapter");
        assertThat(traceableNameToken(adapterClassName))
                .as("%s adapter class name must match adapterKey", adapter.adapterKey())
                .isEqualTo(adapterKeyName);

        assertProviderAdapterDeclaresConcretePayloadType(adapter);
        String payloadTypeName = stripSuffix(adapter.payloadType().getSimpleName(), "ConversationReplyPayload");
        assertThat(traceableNameToken(payloadTypeName))
                .as("%s payload type name must match adapterKey", adapter.adapterKey())
                .isEqualTo(adapterKeyName);
    }

    static void assertProviderAdapterKeyIsNormalizedConstant(ConversationReplyAdapter<?> adapter) {
        assertThat(adapter.adapterKey())
                .as("%s adapterKey", adapter.getClass().getSimpleName())
                .isNotBlank()
                .isEqualTo(adapter.adapterKey().trim())
                .isEqualTo(adapter.adapterKey().toUpperCase(Locale.ROOT));
    }

    static void assertProviderAdapterUsesSharedProviderBaseClass(ConversationReplyAdapter<?> adapter) {
        assertThat(adapter)
                .as("%s adapter must extend %s",
                        adapter.adapterKey(),
                        AbstractProviderConversationReplyAdapter.class.getSimpleName())
                .isInstanceOf(AbstractProviderConversationReplyAdapter.class);
    }

    static void assertProviderAdapterDoesNotDeclareCustomIngressConstruction(ConversationReplyAdapter<?> adapter) {
        List<String> customIngressMethods = new ArrayList<>();
        for (Method method : adapter.getClass().getDeclaredMethods()) {
            if (!method.isSynthetic()
                    && !method.isBridge()
                    && ConversationIngressReq.class.equals(method.getReturnType())) {
                customIngressMethods.add(method.getName());
            }
        }
        assertThat(customIngressMethods)
                .as("%s must rely on %s for provider ingress construction",
                        adapter.adapterKey(),
                        AbstractProviderConversationReplyAdapter.class.getSimpleName())
                .isEmpty();
    }

    static void assertProviderAdapterUsesConstructorDeclaredProviderMapping(ConversationReplyAdapter<?> adapter) {
        List<String> declaredMethods = new ArrayList<>();
        for (Method method : adapter.getClass().getDeclaredMethods()) {
            if (!method.isSynthetic() && !method.isBridge()) {
                declaredMethods.add(method.getName());
            }
        }
        assertThat(declaredMethods)
                .as("%s must declare provider mapping through the %s constructor only",
                        adapter.adapterKey(),
                        AbstractProviderConversationReplyAdapter.class.getSimpleName())
                .isEmpty();
    }

    static void assertProviderAdapterIsStatelessNoArgDefinition(ConversationReplyAdapter<?> adapter) {
        List<String> declaredFields = new ArrayList<>();
        for (Field field : adapter.getClass().getDeclaredFields()) {
            if (!field.isSynthetic()) {
                declaredFields.add(field.getName());
            }
        }
        assertThat(declaredFields)
                .as("%s provider adapter must not declare fields", adapter.adapterKey())
                .isEmpty();

        List<Constructor<?>> constructors = List.of(adapter.getClass().getDeclaredConstructors());
        assertThat(constructors)
                .as("%s provider adapter constructors", adapter.adapterKey())
                .hasSize(1);
        Constructor<?> constructor = constructors.getFirst();
        assertThat(Modifier.isPublic(constructor.getModifiers()))
                .as("%s provider adapter constructor must be public", adapter.adapterKey())
                .isTrue();
        assertThat(constructor.getParameterCount())
                .as("%s provider adapter constructor parameter count", adapter.adapterKey())
                .isZero();
    }

    static void assertProviderAdapterIsSpringComponent(ConversationReplyAdapter<?> adapter) {
        assertThat(adapter.getClass().isAnnotationPresent(Component.class))
                .as("%s provider adapter must be annotated with @%s for runtime catalog registration",
                        adapter.adapterKey(),
                        Component.class.getSimpleName())
                .isTrue();
    }

    static void assertProviderAdapterPayloadTypeDeclaresRoutingComponents(ConversationReplyAdapter<?> adapter) {
        assertProviderAdapterDeclaresConcretePayloadType(adapter);
        Class<?> payloadType = adapter.payloadType();
        LinkedHashSet<String> componentNames = payloadRecordComponentNames(payloadType);
        assertThat(componentNames)
                .as("%s payloadType routing components: %s", adapter.adapterKey(), payloadType.getName())
                .contains(requiredRawPayloadRoutingKeys().toArray(String[]::new));
    }

    static void assertFixtureRawPayloadKeysBindToPayloadRecord(
            FixtureDefinition fixture,
            Map<String, ConversationReplyAdapter<?>> adaptersByKey) {
        Map<String, ConversationReplyAdapter<?>> normalizedAdapters = normalizeAdapters(adaptersByKey);
        String adapterKey = normalizeAdapterKey(fixture.adapterKey());
        ConversationReplyAdapter<?> adapter = normalizedAdapters.get(adapterKey);
        if (adapter == null) {
            throw new AssertionError("Conversation adapter fixture references an unregistered adapter: "
                    + adapterKey + " in " + fixture.resourceName());
        }
        assertProviderAdapterDeclaresConcretePayloadType(adapter);
        Class<?> payloadType = adapter.payloadType();
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull();
        assertThat(payloadRecordComponentNames(payloadType))
                .as("%s rawPayload keys must bind to %s", fixture.resourceName(), payloadType.getName())
                .contains(fixture.rawPayload().keySet().toArray(String[]::new));
    }

    static void assertProviderSpecificPayloadComponentsCoveredByFixtures(
            ConversationReplyAdapter<?> adapter,
            List<FixtureDefinition> fixtures) {
        assertProviderAdapterDeclaresConcretePayloadType(adapter);
        LinkedHashSet<String> providerSpecificComponents = payloadRecordComponentNames(adapter.payloadType());
        providerSpecificComponents.removeAll(commonProviderPayloadComponents());

        LinkedHashSet<String> fixtureRawKeys = new LinkedHashSet<>();
        for (FixtureDefinition fixture : fixtures == null ? List.<FixtureDefinition>of() : fixtures) {
            if (normalizeAdapterKey(fixture.adapterKey()).equals(normalizeAdapterKey(adapter.adapterKey()))
                    && fixture.rawPayload() != null) {
                fixtureRawKeys.addAll(fixture.rawPayload().keySet());
            }
        }

        assertThat(fixtureRawKeys)
                .as("%s fixtures must cover provider-specific payload components", adapter.adapterKey())
                .contains(providerSpecificComponents.toArray(String[]::new));
    }

    static void assertCommonSessionFieldsCoveredByFixtures(
            ConversationReplyAdapter<?> adapter,
            List<FixtureDefinition> fixtures) {
        LinkedHashSet<String> fixtureRawKeys = new LinkedHashSet<>();
        for (FixtureDefinition fixture : fixtures == null ? List.<FixtureDefinition>of() : fixtures) {
            if (normalizeAdapterKey(fixture.adapterKey()).equals(normalizeAdapterKey(adapter.adapterKey()))
                    && fixture.rawPayload() != null) {
                fixtureRawKeys.addAll(fixture.rawPayload().keySet());
            }
        }

        assertThat(fixtureRawKeys)
                .as("%s fixtures must cover common session raw payload fields", adapter.adapterKey())
                .contains(commonSessionRawPayloadFields().toArray(String[]::new));
    }

    static void assertRawPayloadShapeMatchesExpectedMessageType(FixtureDefinition fixture) {
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull();
        if ("TEXT".equals(fixture.expectedMessageType())) {
            assertThat(textValue(fixture.rawPayload().get("text")))
                    .as("%s rawPayload.text", fixture.resourceName())
                    .isNotBlank();
            return;
        }
        if ("INTERACTIVE".equals(fixture.expectedMessageType())) {
            assertThat(hasAnyTextValue(fixture.rawPayload(),
                    "interactiveReplyId",
                    "interactiveReplyTitle",
                    "quickReplyPayload",
                    "quickReplyTitle",
                    "actionId",
                    "actionLabel",
                    "suggestionReplyId",
                    "suggestionText"))
                    .as("%s rawPayload interactive marker", fixture.resourceName())
                    .isTrue();
        }
    }

    static void assertExpectedRoutingFieldsBindToRawPayload(FixtureDefinition fixture) {
        assertThat(fixture.expectedChannel())
                .as("%s expectedChannel must match adapterKey", fixture.resourceName())
                .isEqualTo(normalizeAdapterKey(fixture.adapterKey()));
        assertThat(rawPayloadTextField(fixture, "provider").toUpperCase(Locale.ROOT))
                .as("%s expectedProvider must match rawPayload.provider", fixture.resourceName())
                .isEqualTo(fixture.expectedProvider());
        assertThat(rawPayloadTextField(fixture, "externalMessageId"))
                .as("%s expectedExternalMessageId must match rawPayload.externalMessageId", fixture.resourceName())
                .isEqualTo(fixture.expectedExternalMessageId());
        assertThat(rawPayloadTextField(fixture, "eventId"))
                .as("%s expectedEventId must match rawPayload.eventId", fixture.resourceName())
                .isEqualTo(fixture.expectedEventId());
    }

    static void assertExpectedAttributesBindToRawPayload(FixtureDefinition fixture) {
        assertThat(fixture.expectedAttributes())
                .as("%s expectedAttributes", fixture.resourceName())
                .isNotNull();
        for (Map.Entry<String, Object> attribute : fixture.expectedAttributes().entrySet()) {
            if ("adapter".equals(attribute.getKey())) {
                continue;
            }
            Object rawValue = rawPayloadAttributeValue(fixture, attribute.getKey());
            assertAttributeValueMatchesRawPayload(fixture, attribute.getKey(), rawValue, attribute.getValue());
        }
    }

    static void assertProviderSpecificRawPayloadFieldsBecomeExpectedAttributes(FixtureDefinition fixture) {
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull();
        assertThat(fixture.expectedAttributes())
                .as("%s expectedAttributes", fixture.resourceName())
                .isNotNull();
        for (String rawPayloadKey : fixture.rawPayload().keySet()) {
            if (!commonProviderPayloadComponents().contains(rawPayloadKey)) {
                assertThat(fixture.expectedAttributes())
                        .as("%s expectedAttributes must include provider-specific rawPayload.%s",
                                fixture.resourceName(),
                                rawPayloadKey)
                        .containsKey(rawPayloadKey);
            }
        }
    }

    static void assertExpectedTextBindsToRawPayloadContent(FixtureDefinition fixture) {
        assertThat(fixture.expectedText())
                .as("%s expectedText", fixture.resourceName())
                .isNotBlank();
        if ("TEXT".equals(fixture.expectedMessageType())) {
            assertThat(fixture.expectedText())
                    .as("%s expectedText must match rawPayload.text", fixture.resourceName())
                    .isEqualTo(rawPayloadTextField(fixture, "text"));
            return;
        }
        if ("INTERACTIVE".equals(fixture.expectedMessageType())) {
            assertThat(fixture.expectedText())
                    .as("%s expectedText must match raw interactive display text", fixture.resourceName())
                    .isEqualTo(firstRawPayloadTextField(fixture,
                            "text",
                            "interactiveReplyTitle",
                            "quickReplyTitle",
                            "actionLabel",
                            "suggestionText"));
        }
    }

    static void assertRawPayloadRoutingValuesAreUsable(FixtureDefinition fixture) {
        for (String key : requiredRawPayloadRoutingKeys()) {
            assertThat(fixture.rawPayload())
                    .as("%s rawPayload", fixture.resourceName())
                    .isNotNull()
                    .containsKey(key);
            Object value = fixture.rawPayload().get(key);
            assertThat(value)
                    .as("%s rawPayload.%s", fixture.resourceName(), key)
                    .isInstanceOf(String.class);
            assertThat(((String) value).trim())
                    .as("%s rawPayload.%s", fixture.resourceName(), key)
                    .isNotEmpty();
        }
    }

    static List<String> fixtureResources() {
        return discoveredFixtureResources();
    }

    static void assertFixtureIndexFileIsAbsent() {
        URL fixtureIndex = Thread.currentThread().getContextClassLoader().getResource(FIXTURE_DIR + "index.json");
        assertThat(fixtureIndex)
                .as("adapter contract fixture index has been retired; add JSON fixtures directly under %s",
                        FIXTURE_DIR)
                .isNull();
    }

    static List<String> discoveredFixtureResources() {
        URL fixtureDirectory = Thread.currentThread().getContextClassLoader().getResource(FIXTURE_DIR);
        if (fixtureDirectory == null) {
            throw new AssertionError("Conversation adapter fixture directory not found: " + FIXTURE_DIR);
        }
        if (!"file".equals(fixtureDirectory.getProtocol())) {
            throw new AssertionError("Conversation adapter fixture directory must be available as files: "
                    + fixtureDirectory);
        }
        File directory = new File(URI.create(fixtureDirectory.toString()));
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json") && !"index.json".equals(name));
        List<String> resources = new ArrayList<>();
        for (File file : files == null ? new File[0] : files) {
            resources.add(FIXTURE_DIR + file.getName());
        }
        Collections.sort(resources);
        return List.copyOf(resources);
    }

    static Set<String> contractKeys(List<RawContractCase> contractCases) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (RawContractCase contractCase : contractCases == null ? List.<RawContractCase>of() : contractCases) {
            keys.add(new ConversationAdapterCatalog(List.of(contractCase.adapter())).keys().getFirst());
        }
        return Collections.unmodifiableSet(keys);
    }

    static Set<String> providerAdapterKeys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(providerAdapterKeyDeclarations()));
    }

    static List<String> providerAdapterKeyDeclarations() {
        List<String> declarations = new ArrayList<>();
        for (Class<? extends ConversationReplyAdapter<?>> adapterClass : providerAdapterClasses()) {
            declarations.add(instantiate(adapterClass).adapterKey().trim().toUpperCase(Locale.ROOT));
        }
        return List.copyOf(declarations);
    }

    static Map<String, ConversationReplyAdapter<?>> providerAdapterRegistry() {
        Map<String, ConversationReplyAdapter<?>> adapters = new LinkedHashMap<>();
        for (Class<? extends ConversationReplyAdapter<?>> adapterClass : providerAdapterClasses()) {
            ConversationReplyAdapter<?> adapter = instantiate(adapterClass);
            adapters.put(normalizeAdapterKey(adapter.adapterKey()), adapter);
        }
        return Collections.unmodifiableMap(adapters);
    }

    static ConversationIngressReq captureRawIngress(ConversationReplyAdapter<?> adapter,
                                                    String adapterKey,
                                                    Map<String, Object> rawPayload,
                                                    String operator) {
        ConversationAdapterCatalog catalog = new ConversationAdapterCatalog(List.of(adapter));
        ConversationReplyAdapter<Object> resolvedAdapter = catalog.require(adapterKey);
        Object payload = OBJECT_MAPPER.convertValue(
                rawPayload == null ? Map.of() : rawPayload,
                resolvedAdapter.payloadType());
        return resolvedAdapter.toIngress(payload, new ConversationAdapterContext(8L, operator));
    }

    static void assertRejectsMissingPayload(ConversationReplyAdapter<?> adapter, String expectedMessage) {
        assertThatThrownBy(() -> adapter.toIngress(null, new ConversationAdapterContext(8L, "system")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    private static Map<String, ConversationReplyAdapter<?>> normalizeAdapters(
            Map<String, ConversationReplyAdapter<?>> adaptersByKey) {
        Map<String, ConversationReplyAdapter<?>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, ConversationReplyAdapter<?>> entry
                : adaptersByKey == null ? Map.<String, ConversationReplyAdapter<?>>of().entrySet()
                : adaptersByKey.entrySet()) {
            normalized.put(normalizeAdapterKey(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(normalized);
    }

    private static String normalizeAdapterKey(String adapterKey) {
        if (adapterKey == null || adapterKey.isBlank()) {
            throw new AssertionError("conversation adapter fixture adapterKey is required");
        }
        return adapterKey.trim().toUpperCase(Locale.ROOT);
    }

    private static void assertUppercaseTrimmedFixtureField(FixtureDefinition fixture, String fieldName, String value) {
        assertThat(value)
                .as("%s %s", fixture.resourceName(), fieldName)
                .isNotBlank()
                .isEqualTo(value.trim().toUpperCase(Locale.ROOT));
    }

    private static LinkedHashSet<String> payloadRecordComponentNames(Class<?> payloadType) {
        LinkedHashSet<String> componentNames = new LinkedHashSet<>();
        for (java.lang.reflect.RecordComponent component : payloadType.getRecordComponents()) {
            componentNames.add(component.getName());
        }
        return componentNames;
    }

    private static boolean hasAnyTextValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (!textValue(payload.get(key)).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String rawPayloadTextField(FixtureDefinition fixture, String key) {
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull()
                .containsKey(key);
        return textValue(fixture.rawPayload().get(key));
    }

    private static String firstRawPayloadTextField(FixtureDefinition fixture, String... keys) {
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull();
        for (String key : keys) {
            String value = textValue(fixture.rawPayload().get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Object rawPayloadAttributeValue(FixtureDefinition fixture, String key) {
        assertThat(fixture.rawPayload())
                .as("%s rawPayload", fixture.resourceName())
                .isNotNull();
        if (fixture.rawPayload().containsKey(key)) {
            return fixture.rawPayload().get(key);
        }
        Object nestedAttributes = fixture.rawPayload().get("attributes");
        if (nestedAttributes instanceof Map<?, ?> attributes && attributes.containsKey(key)) {
            return attributes.get(key);
        }
        throw new AssertionError("%s expectedAttributes.%s must bind to rawPayload.%s or rawPayload.attributes.%s"
                .formatted(fixture.resourceName(), key, key, key));
    }

    private static void assertAttributeValueMatchesRawPayload(
            FixtureDefinition fixture,
            String key,
            Object rawValue,
            Object expectedValue) {
        if (rawValue instanceof String rawText && expectedValue instanceof String expectedText) {
            assertThat(rawText.trim())
                    .as("%s expectedAttributes.%s", fixture.resourceName(), key)
                    .isEqualTo(expectedText.trim());
            return;
        }
        assertThat(rawValue)
                .as("%s expectedAttributes.%s", fixture.resourceName(), key)
                .isEqualTo(expectedValue);
    }

    private static void assertOptionalRawPayloadLongField(RawContractCase contractCase, String key, Long actualValue) {
        if (!contractCase.rawPayload().containsKey(key)) {
            return;
        }
        Object rawValue = contractCase.rawPayload().get(key);
        if (rawValue == null) {
            assertThat(actualValue)
                    .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                    .isNull();
            return;
        }
        assertThat(rawValue)
                .as("%s rawPayload.%s", contractCase.resourceName(), key)
                .isInstanceOf(Number.class);
        assertThat(actualValue)
                .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                .isEqualTo(((Number) rawValue).longValue());
    }

    private static void assertOptionalRawPayloadTextField(RawContractCase contractCase, String key, String actualValue) {
        if (!contractCase.rawPayload().containsKey(key)) {
            return;
        }
        Object rawValue = contractCase.rawPayload().get(key);
        if (rawValue == null) {
            assertThat(actualValue)
                    .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                    .isNull();
            return;
        }
        assertThat(rawValue)
                .as("%s rawPayload.%s", contractCase.resourceName(), key)
                .isInstanceOf(String.class);
        assertThat(actualValue)
                .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                .isEqualTo(((String) rawValue).trim());
    }

    private static void assertOptionalRawPayloadDateTimeField(
            RawContractCase contractCase,
            String key,
            LocalDateTime actualValue) {
        if (!contractCase.rawPayload().containsKey(key)) {
            return;
        }
        Object rawValue = contractCase.rawPayload().get(key);
        if (rawValue == null) {
            assertThat(actualValue)
                    .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                    .isNull();
            return;
        }

        LocalDateTime expectedValue;
        if (rawValue instanceof LocalDateTime rawDateTime) {
            expectedValue = rawDateTime;
        } else {
            assertThat(rawValue)
                    .as("%s rawPayload.%s", contractCase.resourceName(), key)
                    .isInstanceOf(String.class);
            expectedValue = LocalDateTime.parse(((String) rawValue).trim());
        }

        assertThat(actualValue)
                .as("%s %s must match rawPayload.%s", contractCase.resourceName(), key, key)
                .isEqualTo(expectedValue);
    }

    private static String textValue(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private static String stripSuffix(String value, String suffix) {
        if (value != null && value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        }
        return value == null ? "" : value;
    }

    private static String traceableNameToken(String value) {
        StringBuilder token = new StringBuilder();
        for (char character : value == null ? new char[0] : value.toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                token.append(Character.toUpperCase(character));
            }
        }
        return token.toString();
    }

    private static <T> T readResource(String resourceName, Class<T> type) {
        try (InputStream input = resourceStream(resourceName)) {
            return OBJECT_MAPPER.readValue(input, type);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read conversation adapter fixture resource: " + resourceName, ex);
        }
    }

    private static InputStream resourceStream(String resourceName) {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        if (input == null) {
            throw new AssertionError("Conversation adapter fixture resource not found: " + resourceName);
        }
        return input;
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends ConversationReplyAdapter<?>>> providerAdapterClasses() {
        String packageName = ConversationReplyAdapter.class.getPackageName();
        String packagePath = packageName.replace('.', '/');
        try {
            List<Class<? extends ConversationReplyAdapter<?>>> classes = new ArrayList<>();
            LinkedHashSet<String> classNames = new LinkedHashSet<>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            for (URL resource : Collections.list(classLoader.getResources(packagePath))) {
                if (!"file".equals(resource.getProtocol())) {
                    continue;
                }
                File directory = new File(URI.create(resource.toString()));
                File[] files = directory.listFiles((dir, name) -> name.endsWith("ConversationReplyAdapter.class"));
                for (File file : files == null ? new File[0] : files) {
                    String simpleName = file.getName().replace(".class", "");
                    String className = packageName + "." + simpleName;
                    if (!classNames.add(className)) {
                        continue;
                    }
                    Class<?> candidate = Class.forName(className);
                    if (isProviderAdapter(candidate)) {
                        classes.add((Class<? extends ConversationReplyAdapter<?>>) candidate);
                    }
                }
            }
            classes.sort((left, right) -> left.getSimpleName().compareTo(right.getSimpleName()));
            return classes;
        } catch (Exception ex) {
            throw new AssertionError("Could not discover conversation provider adapters", ex);
        }
    }

    private static boolean isProviderAdapter(Class<?> candidate) {
        return ConversationReplyAdapter.class.isAssignableFrom(candidate)
                && !candidate.isInterface()
                && !Modifier.isAbstract(candidate.getModifiers())
                && !SandboxConversationReplyAdapter.class.equals(candidate);
    }

    private static ConversationReplyAdapter<?> instantiate(Class<? extends ConversationReplyAdapter<?>> adapterClass) {
        try {
            return adapterClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new AssertionError("Conversation provider adapter must be stateless and test-instantiable: "
                    + adapterClass.getName(), ex);
        }
    }

}
