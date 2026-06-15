package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.TestUserFacade;
import org.chovy.canvas.platform.domain.TestUserCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestUserApplicationService implements TestUserFacade {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";

    private final TestUserCatalog catalog;

    public TestUserApplicationService() {
        this(new TestUserCatalog());
    }

    public TestUserApplicationService(TestUserCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> listSets(Long tenantId) {
        return catalog.listSets(tenantIdOrDefault(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSet(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createSet(tenantIdOrDefault(tenantId), requireText(payload, "name", "name is required"),
                text(payload, "description"), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listUsers(Long tenantId, Long setId) {
        return catalog.listUsers(tenantIdOrDefault(tenantId), requireId(setId, "setId is required"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(Long tenantId, Long setId, Map<String, Object> payload) {
        return catalog.createUser(tenantIdOrDefault(tenantId), requireId(setId, "setId is required"),
                requireText(payload, "userId", "userId is required"), text(payload, "displayName"),
                mapValue(payload, "profile"), mapValue(payload, "inputParams"));
    }

    @Override
    public Map<String, Object> getUser(Long tenantId, Long id) {
        return catalog.getUser(tenantIdOrDefault(tenantId), requireId(id, "test user id is required"));
    }

    @Override
    public Map<String, Object> preview(Long tenantId, Long id) {
        return catalog.preview(tenantIdOrDefault(tenantId), requireId(id, "test user id is required"));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    private static String requireText(Map<String, Object> payload, String key, String message) {
        String value = text(payload, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String text(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> payload, String key) {
        if (payload == null || !(payload.get(key) instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return (Map<String, Object>) map;
    }
}
