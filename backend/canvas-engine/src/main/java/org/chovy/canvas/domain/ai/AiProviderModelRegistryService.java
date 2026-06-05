package org.chovy.canvas.domain.ai;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiProviderModelRegistryService {

    private final AtomicLong providerIdSequence = new AtomicLong(100L);
    private final AtomicLong modelIdSequence = new AtomicLong(1000L);
    private final ConcurrentMap<Long, ProviderRegistration> providers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ModelRegistration> models = new ConcurrentHashMap<>();

    public AiProviderModelRegistryService() {
        ProviderRegistration provider = new ProviderRegistration(
                1L,
                null,
                "mock-ai",
                "Mock AI Provider",
                "MOCK",
                "mock://local",
                null,
                null,
                true);
        providers.put(provider.id(), provider);
        models.put(1L, new ModelRegistration(
                1L,
                null,
                provider.id(),
                "mock-marketing-v1",
                "Mock Marketing V1",
                "TEXT_JSON",
                8192,
                true));
    }

    public List<ProviderView> listProviders(Long tenantId) {
        return providers.values().stream()
                .filter(provider -> visibleToTenant(provider.tenantId(), tenantId))
                .sorted(Comparator.comparing(ProviderRegistration::tenantId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(ProviderRegistration::id))
                .map(provider -> toView(provider, listModels(tenantId, provider.id())))
                .toList();
    }

    public ProviderView createProvider(Long tenantId, ProviderCreateRequest req) {
        Long scopedTenantId = normalizeTenantId(tenantId);
        String apiKey = trimToNull(req.apiKey());
        long id = providerIdSequence.incrementAndGet();
        ProviderRegistration provider = new ProviderRegistration(
                id,
                scopedTenantId,
                requireText(req.providerKey(), "providerKey"),
                requireText(req.displayName(), "displayName"),
                blankToDefault(req.providerType(), "OPENAI_COMPATIBLE"),
                requireText(req.endpoint(), "endpoint"),
                maskSecret(apiKey),
                digestSecret(apiKey),
                req.enabled() == null || req.enabled());
        providers.put(id, provider);
        if (req.models() != null) {
            req.models().stream()
                    .filter(model -> trimToNull(model.modelKey()) != null)
                    .forEach(model -> addModel(scopedTenantId, id, model));
        }
        return toView(provider, listModels(scopedTenantId, id));
    }

    public ProviderView getProvider(Long tenantId, Long providerId) {
        ProviderRegistration provider = requireVisibleProvider(tenantId, providerId);
        return toView(provider, listModels(tenantId, provider.id()));
    }

    public ProviderView updateProvider(Long tenantId, Long providerId, ProviderUpdateRequest req) {
        ProviderRegistration existing = requireVisibleProvider(tenantId, providerId);
        Long scopedTenantId = normalizeTenantId(tenantId);
        if (!Objects.equals(existing.tenantId(), scopedTenantId)) {
            throw new IllegalArgumentException("Built-in AI providers cannot be updated");
        }
        String apiKey = trimToNull(req.apiKey());
        ProviderRegistration updated = new ProviderRegistration(
                existing.id(),
                existing.tenantId(),
                blankToDefault(req.providerKey(), existing.providerKey()),
                blankToDefault(req.displayName(), existing.displayName()),
                blankToDefault(req.providerType(), existing.providerType()),
                blankToDefault(req.endpoint(), existing.endpoint()),
                apiKey == null ? existing.maskedApiKey() : maskSecret(apiKey),
                apiKey == null ? existing.secretDigest() : digestSecret(apiKey),
                req.enabled() == null ? existing.enabled() : req.enabled());
        providers.put(providerId, updated);
        return toView(updated, listModels(tenantId, providerId));
    }

    public void disableProvider(Long tenantId, Long providerId) {
        ProviderRegistration existing = requireVisibleProvider(tenantId, providerId);
        Long scopedTenantId = normalizeTenantId(tenantId);
        if (!Objects.equals(existing.tenantId(), scopedTenantId)) {
            throw new IllegalArgumentException("Built-in AI providers cannot be disabled");
        }
        providers.put(providerId, new ProviderRegistration(
                existing.id(),
                existing.tenantId(),
                existing.providerKey(),
                existing.displayName(),
                existing.providerType(),
                existing.endpoint(),
                existing.maskedApiKey(),
                existing.secretDigest(),
                false));
    }

    public ProviderView requireEnabledProvider(Long tenantId, Long providerId) {
        ProviderRegistration provider = requireVisibleProvider(tenantId, providerId);
        if (!provider.enabled()) {
            throw new IllegalArgumentException("AI provider is disabled: " + providerId);
        }
        return toView(provider, listModels(tenantId, provider.id()));
    }

    public List<ModelView> listModels(Long tenantId, Long providerId) {
        return models.values().stream()
                .filter(model -> visibleToTenant(model.tenantId(), tenantId))
                .filter(model -> providerId == null || Objects.equals(model.providerId(), providerId))
                .sorted(Comparator.comparing(ModelRegistration::tenantId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(ModelRegistration::id))
                .map(this::toView)
                .toList();
    }

    public String defaultModelKey(Long tenantId, Long providerId) {
        return listModels(tenantId, providerId).stream()
                .filter(ModelView::enabled)
                .map(ModelView::modelKey)
                .findFirst()
                .orElse("mock-marketing-v1");
    }

    private void addModel(Long tenantId, Long providerId, ModelCreateRequest req) {
        long id = modelIdSequence.incrementAndGet();
        models.put(id, new ModelRegistration(
                id,
                tenantId,
                providerId,
                requireText(req.modelKey(), "modelKey"),
                blankToDefault(req.displayName(), req.modelKey()),
                blankToDefault(req.capability(), "TEXT_JSON"),
                req.contextWindow() == null ? 8192 : req.contextWindow(),
                req.enabled() == null || req.enabled()));
    }

    private ProviderRegistration requireVisibleProvider(Long tenantId, Long providerId) {
        ProviderRegistration provider = providers.get(providerId);
        if (provider == null || !visibleToTenant(provider.tenantId(), tenantId)) {
            throw new IllegalArgumentException("AI provider not found: " + providerId);
        }
        return provider;
    }

    private ProviderView toView(ProviderRegistration provider, List<ModelView> providerModels) {
        return new ProviderView(
                provider.id(),
                provider.tenantId(),
                provider.providerKey(),
                provider.displayName(),
                provider.providerType(),
                provider.endpoint(),
                provider.maskedApiKey(),
                provider.enabled(),
                providerModels);
    }

    private ModelView toView(ModelRegistration model) {
        return new ModelView(
                model.id(),
                model.tenantId(),
                model.providerId(),
                model.modelKey(),
                model.displayName(),
                model.capability(),
                model.contextWindow(),
                model.enabled());
    }

    static boolean visibleToTenant(Long ownerTenantId, Long tenantId) {
        return ownerTenantId == null || Objects.equals(ownerTenantId, normalizeTenantId(tenantId));
    }

    static Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    static String maskSecret(String secret) {
        String value = trimToNull(secret);
        if (value == null) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    private static String digestSecret(String secret) {
        String value = trimToNull(secret);
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return trimmed;
    }

    private static String blankToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ProviderRegistration(
            Long id,
            Long tenantId,
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String maskedApiKey,
            String secretDigest,
            boolean enabled) {
    }

    private record ModelRegistration(
            Long id,
            Long tenantId,
            Long providerId,
            String modelKey,
            String displayName,
            String capability,
            Integer contextWindow,
            boolean enabled) {
    }

    public record ProviderCreateRequest(
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String apiKey,
            Boolean enabled,
            List<ModelCreateRequest> models) {
    }

    public record ProviderUpdateRequest(
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String apiKey,
            Boolean enabled) {
    }

    public record ModelCreateRequest(
            String modelKey,
            String displayName,
            String capability,
            Integer contextWindow,
            Boolean enabled) {
    }

    public record ProviderView(
            Long id,
            Long tenantId,
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String maskedApiKey,
            boolean enabled,
            List<ModelView> models) {
    }

    public record ModelView(
            Long id,
            Long tenantId,
            Long providerId,
            String modelKey,
            String displayName,
            String capability,
            Integer contextWindow,
            boolean enabled) {
    }
}
