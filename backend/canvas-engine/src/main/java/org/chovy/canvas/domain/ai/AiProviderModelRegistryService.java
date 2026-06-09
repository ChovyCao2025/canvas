package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AiModelRegistryDO;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.chovy.canvas.dal.mapper.AiModelRegistryMapper;
import org.chovy.canvas.dal.mapper.AiProviderMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
    private final AiProviderMapper providerMapper;
    private final AiModelRegistryMapper modelMapper;
    private final SecretCipher secretCipher;

    public AiProviderModelRegistryService() {
        this(null, null, null, true);
    }

    @Autowired
    public AiProviderModelRegistryService(AiProviderMapper providerMapper,
                                          AiModelRegistryMapper modelMapper,
                                          SecretCipher secretCipher) {
        this(providerMapper, modelMapper, secretCipher, false);
    }

    private AiProviderModelRegistryService(AiProviderMapper providerMapper,
                                           AiModelRegistryMapper modelMapper,
                                           SecretCipher secretCipher,
                                           boolean seedMemory) {
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.secretCipher = secretCipher;
        if (seedMemory) {
            seedInMemory();
        }
    }

    public List<ProviderView> listProviders(Long tenantId) {
        if (mapperBacked()) {
            return providerMapper.selectList(providerScope(tenantId))
                    .stream()
                    .filter(provider -> visibleToTenant(provider.getTenantId(), tenantId))
                    .sorted(Comparator.comparing(AiProviderDO::getTenantId, Comparator.nullsFirst(Long::compareTo))
                            .thenComparing(AiProviderDO::getId))
                    .map(provider -> toView(provider, listModels(tenantId, provider.getId())))
                    .toList();
        }
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
        if (mapperBacked()) {
            AiProviderDO row = new AiProviderDO();
            row.setTenantId(scopedTenantId);
            row.setProviderKey(requireText(req.providerKey(), "providerKey"));
            row.setDisplayName(requireText(req.displayName(), "displayName"));
            row.setProviderType(blankToDefault(req.providerType(), "OPENAI_COMPATIBLE"));
            row.setEndpoint(requireText(req.endpoint(), "endpoint"));
            row.setEncryptedApiKey(encrypt(apiKey));
            row.setDefaultModel(defaultModel(req.models()));
            row.setEnabled(req.enabled() == null || req.enabled() ? 1 : 0);
            providerMapper.insert(row);
            if (req.models() != null) {
                req.models().stream()
                        .filter(model -> trimToNull(model.modelKey()) != null)
                        .forEach(model -> insertModel(scopedTenantId, row.getId(), model));
            }
            return toView(row, listModels(scopedTenantId, row.getId()), maskSecret(apiKey));
        }
        long id = providerIdSequence.incrementAndGet();
        ProviderRegistration provider = new ProviderRegistration(
                id,
                scopedTenantId,
                requireText(req.providerKey(), "providerKey"),
                requireText(req.displayName(), "displayName"),
                blankToDefault(req.providerType(), "OPENAI_COMPATIBLE"),
                requireText(req.endpoint(), "endpoint"),
                maskSecret(apiKey),
                apiKey,
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
        if (mapperBacked()) {
            AiProviderDO provider = requireVisibleProviderRow(tenantId, providerId);
            return toView(provider, listModels(tenantId, provider.getId()));
        }
        ProviderRegistration provider = requireVisibleProvider(tenantId, providerId);
        return toView(provider, listModels(tenantId, provider.id()));
    }

    public ProviderView updateProvider(Long tenantId, Long providerId, ProviderUpdateRequest req) {
        Long scopedTenantId = normalizeTenantId(tenantId);
        if (mapperBacked()) {
            AiProviderDO existing = requireVisibleProviderRow(tenantId, providerId);
            if (!Objects.equals(existing.getTenantId(), scopedTenantId)) {
                throw new IllegalArgumentException("Built-in AI providers cannot be updated");
            }
            String apiKey = trimToNull(req.apiKey());
            existing.setProviderKey(blankToDefault(req.providerKey(), existing.getProviderKey()));
            existing.setDisplayName(blankToDefault(req.displayName(), existing.getDisplayName()));
            existing.setProviderType(blankToDefault(req.providerType(), existing.getProviderType()));
            existing.setEndpoint(blankToDefault(req.endpoint(), existing.getEndpoint()));
            if (apiKey != null) {
                existing.setEncryptedApiKey(encrypt(apiKey));
            }
            existing.setEnabled(req.enabled() == null ? existing.getEnabled() : (req.enabled() ? 1 : 0));
            providerMapper.updateById(existing);
            String maskedApiKey = apiKey == null
                    ? maskPersistedSecret(existing.getEncryptedApiKey())
                    : maskSecret(apiKey);
            return toView(existing, listModels(tenantId, providerId), maskedApiKey);
        }
        ProviderRegistration existing = requireVisibleProvider(tenantId, providerId);
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
                apiKey == null ? existing.apiKey() : apiKey,
                req.enabled() == null ? existing.enabled() : req.enabled());
        providers.put(providerId, updated);
        return toView(updated, listModels(tenantId, providerId));
    }

    public void disableProvider(Long tenantId, Long providerId) {
        Long scopedTenantId = normalizeTenantId(tenantId);
        if (mapperBacked()) {
            AiProviderDO existing = requireVisibleProviderRow(tenantId, providerId);
            if (!Objects.equals(existing.getTenantId(), scopedTenantId)) {
                throw new IllegalArgumentException("Built-in AI providers cannot be disabled");
            }
            existing.setEnabled(0);
            providerMapper.updateById(existing);
            return;
        }
        ProviderRegistration existing = requireVisibleProvider(tenantId, providerId);
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
                existing.apiKey(),
                false));
    }

    public ProviderView requireEnabledProvider(Long tenantId, Long providerId) {
        if (mapperBacked()) {
            AiProviderDO provider = requireVisibleProviderRow(tenantId, providerId);
            if (!enabled(provider.getEnabled())) {
                throw new IllegalArgumentException("AI provider is disabled: " + providerId);
            }
            return toView(provider, listModels(tenantId, provider.getId()));
        }
        ProviderRegistration provider = requireVisibleProvider(tenantId, providerId);
        if (!provider.enabled()) {
            throw new IllegalArgumentException("AI provider is disabled: " + providerId);
        }
        return toView(provider, listModels(tenantId, provider.id()));
    }

    public ProviderCallView requireEnabledProviderForCall(Long tenantId, Long providerId) {
        if (mapperBacked()) {
            AiProviderDO provider = requireVisibleProviderRow(tenantId, providerId);
            if (!enabled(provider.getEnabled())) {
                throw new IllegalArgumentException("AI provider is disabled: " + providerId);
            }
            return toCallView(provider, listModels(tenantId, provider.getId()));
        }
        ProviderRegistration provider = requireVisibleProvider(tenantId, providerId);
        if (!provider.enabled()) {
            throw new IllegalArgumentException("AI provider is disabled: " + providerId);
        }
        return toCallView(provider, listModels(tenantId, provider.id()));
    }

    public List<ModelView> listModels(Long tenantId, Long providerId) {
        if (modelMapper != null) {
            return modelMapper.selectList(modelScope(tenantId, providerId))
                    .stream()
                    .filter(model -> visibleToTenant(model.getTenantId(), tenantId))
                    .filter(model -> providerId == null || Objects.equals(model.getProviderId(), providerId))
                    .sorted(Comparator.comparing(AiModelRegistryDO::getTenantId, Comparator.nullsFirst(Long::compareTo))
                            .thenComparing(AiModelRegistryDO::getId))
                    .map(this::toView)
                    .toList();
        }
        return models.values().stream()
                .filter(model -> visibleToTenant(model.tenantId(), tenantId))
                .filter(model -> providerId == null || Objects.equals(model.providerId(), providerId))
                .sorted(Comparator.comparing(ModelRegistration::tenantId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(ModelRegistration::id))
                .map(this::toView)
                .toList();
    }

    public String defaultModelKey(Long tenantId, Long providerId) {
        if (mapperBacked()) {
            AiProviderDO row = requireVisibleProviderRow(tenantId, providerId);
            if (trimToNull(row.getDefaultModel()) != null) {
                return row.getDefaultModel().trim();
            }
        }
        return listModels(tenantId, providerId).stream()
                .filter(ModelView::enabled)
                .map(ModelView::modelKey)
                .findFirst()
                .orElse("mock-marketing-v1");
    }

    private void insertModel(Long tenantId, Long providerId, ModelCreateRequest req) {
        AiModelRegistryDO row = new AiModelRegistryDO();
        row.setTenantId(tenantId);
        row.setProviderId(providerId);
        row.setModelKey(requireText(req.modelKey(), "modelKey"));
        row.setDisplayName(blankToDefault(req.displayName(), req.modelKey()));
        row.setCapability(blankToDefault(req.capability(), "TEXT_JSON"));
        row.setContextWindow(req.contextWindow() == null ? 8192 : req.contextWindow());
        row.setEnabled(req.enabled() == null || req.enabled() ? 1 : 0);
        modelMapper.insert(row);
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

    private AiProviderDO requireVisibleProviderRow(Long tenantId, Long providerId) {
        AiProviderDO provider = providerMapper.selectById(providerId);
        if (provider == null || !visibleToTenant(provider.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("AI provider not found: " + providerId);
        }
        return provider;
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

    private ProviderView toView(AiProviderDO provider, List<ModelView> providerModels) {
        return toView(provider, providerModels, maskPersistedSecret(provider.getEncryptedApiKey()));
    }

    private ProviderView toView(AiProviderDO provider, List<ModelView> providerModels, String maskedApiKey) {
        return new ProviderView(
                provider.getId(),
                provider.getTenantId(),
                provider.getProviderKey(),
                provider.getDisplayName(),
                provider.getProviderType(),
                provider.getEndpoint(),
                maskedApiKey,
                enabled(provider.getEnabled()),
                providerModels);
    }

    private ProviderCallView toCallView(AiProviderDO provider, List<ModelView> providerModels) {
        return new ProviderCallView(
                provider.getId(),
                provider.getTenantId(),
                provider.getProviderKey(),
                provider.getDisplayName(),
                provider.getProviderType(),
                provider.getEndpoint(),
                decrypt(provider.getEncryptedApiKey()),
                enabled(provider.getEnabled()),
                providerModels);
    }

    private ProviderCallView toCallView(ProviderRegistration provider, List<ModelView> providerModels) {
        return new ProviderCallView(
                provider.id(),
                provider.tenantId(),
                provider.providerKey(),
                provider.displayName(),
                provider.providerType(),
                provider.endpoint(),
                provider.apiKey(),
                provider.enabled(),
                providerModels);
    }

    private ModelView toView(AiModelRegistryDO model) {
        return new ModelView(
                model.getId(),
                model.getTenantId(),
                model.getProviderId(),
                model.getModelKey(),
                model.getDisplayName(),
                model.getCapability(),
                model.getContextWindow(),
                enabled(model.getEnabled()));
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

    private LambdaQueryWrapper<AiProviderDO> providerScope(Long tenantId) {
        Long scopedTenantId = normalizeTenantId(tenantId);
        return new LambdaQueryWrapper<AiProviderDO>()
                .and(wrapper -> wrapper
                        .isNull(AiProviderDO::getTenantId)
                        .or()
                        .eq(AiProviderDO::getTenantId, 0L)
                        .or()
                        .eq(AiProviderDO::getTenantId, scopedTenantId));
    }

    private LambdaQueryWrapper<AiModelRegistryDO> modelScope(Long tenantId, Long providerId) {
        Long scopedTenantId = normalizeTenantId(tenantId);
        LambdaQueryWrapper<AiModelRegistryDO> wrapper = new LambdaQueryWrapper<AiModelRegistryDO>()
                .and(nested -> nested
                        .isNull(AiModelRegistryDO::getTenantId)
                        .or()
                        .eq(AiModelRegistryDO::getTenantId, 0L)
                        .or()
                        .eq(AiModelRegistryDO::getTenantId, scopedTenantId));
        if (providerId != null) {
            wrapper.eq(AiModelRegistryDO::getProviderId, providerId);
        }
        return wrapper;
    }

    private boolean mapperBacked() {
        return providerMapper != null && modelMapper != null && secretCipher != null;
    }

    private String encrypt(String secret) {
        String value = trimToNull(secret);
        return value == null ? null : secretCipher.encrypt(value);
    }

    private String decrypt(String ciphertext) {
        if (secretCipher == null) {
            return null;
        }
        String value = secretCipher.decrypt(ciphertext);
        return trimToNull(value);
    }

    private static boolean enabled(Integer enabled) {
        return enabled == null || enabled != 0;
    }

    static boolean visibleToTenant(Long ownerTenantId, Long tenantId) {
        return ownerTenantId == null
                || ownerTenantId == 0L
                || Objects.equals(ownerTenantId, normalizeTenantId(tenantId));
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

    static String maskPersistedSecret(String encryptedSecret) {
        return trimToNull(encryptedSecret) == null ? null : "****";
    }

    private static String defaultModel(List<ModelCreateRequest> models) {
        if (models == null) {
            return null;
        }
        return models.stream()
                .map(ModelCreateRequest::modelKey)
                .filter(value -> trimToNull(value) != null)
                .map(String::trim)
                .findFirst()
                .orElse(null);
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

    private void seedInMemory() {
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

    private record ProviderRegistration(
            Long id,
            Long tenantId,
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String maskedApiKey,
            String apiKey,
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

    public record ProviderCallView(
            Long id,
            Long tenantId,
            String providerKey,
            String displayName,
            String providerType,
            String endpoint,
            String apiKey,
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
