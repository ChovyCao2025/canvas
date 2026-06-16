package org.chovy.canvas.cdp.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.cdp.api.CdpWriteKeyAuthenticationFacade;
import org.chovy.canvas.cdp.api.CdpWriteKeyFacade;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.chovy.canvas.cdp.domain.CdpWriteKeyCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排 CdpWriteKey 的应用服务流程。
 */
@Service
public class CdpWriteKeyApplicationService implements CdpWriteKeyFacade, CdpWriteKeyAuthenticationFacade {

    /**
     * 执行 CdpWriteKeyCatalog 对应的 CDP 业务操作。
     */
    private final CdpWriteKeyCatalog catalog = new CdpWriteKeyCatalog();
    private final Map<String, CdpWriteKeyView> activeKeys = new ConcurrentHashMap<>();
    private final Map<Long, String> rawKeysById = new ConcurrentHashMap<>();

    /**
     * 执行 authenticate 对应的 CDP 业务操作。
     */
    @Override
    public CdpWriteKeyView authenticate(String authorizationHeader) {
        CdpWriteKeyView key = activeKeys.get(extractBasicWriteKey(authorizationHeader));
        if (key == null) {
            throw new IllegalArgumentException("CDP write key is invalid");
        }
        return key;
    }

    /**
     * 查询list列表。
     */
    @Override
    public List<KeyRow> list(Long tenantId) {
        return catalog.list(safeTenantId(tenantId));
    }

    /**
     * 创建create。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResult create(Long tenantId, CreateCommand command, String actor) {
        Long normalizedTenantId = safeTenantId(tenantId);
        CreateResult result = catalog.create(normalizedTenantId, command, actor);
        activeKeys.put(result.rawKey(), new CdpWriteKeyView(result.id(), normalizedTenantId, result.rawKey(),
                result.platform(), result.rateLimitQps(), null));
        rawKeysById.put(result.id(), result.rawKey());
        return result;
    }

    /**
     * 执行 disable 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long tenantId, Long id) {
        catalog.disable(safeTenantId(tenantId), id);
        String rawKey = rawKeysById.remove(id);
        if (rawKey != null) {
            activeKeys.remove(rawKey);
        }
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 extractBasicWriteKey 对应的 CDP 业务操作。
     */
    private static String extractBasicWriteKey(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException("CDP write key is required");
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            String raw = colon >= 0 ? decoded.substring(0, colon) : decoded;
            if (raw.isBlank()) {
                throw new IllegalArgumentException("CDP write key is required");
            }
            return raw;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("CDP write key is malformed", ex);
        }
    }
}
