package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagDefinitionFacade;
import org.chovy.canvas.cdp.domain.CdpTagDefinitionCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpTagDefinitionApplicationService implements CdpTagDefinitionFacade {

    private final CdpTagDefinitionCatalog catalog;
    private final Clock clock;

    public CdpTagDefinitionApplicationService() {
        this(new CdpTagDefinitionCatalog(), Clock.systemDefaultZone());
    }

    CdpTagDefinitionApplicationService(Clock clock) {
        this(new CdpTagDefinitionCatalog(), clock);
    }

    CdpTagDefinitionApplicationService(CdpTagDefinitionCatalog catalog, Clock clock) {
        this.catalog = catalog;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public Map<String, Object> list(Long tenantId, Integer page, Integer size, String tagType, Integer enabled) {
        return catalog.listDefinitions(tenantIdOrDefault(tenantId), pageOrDefault(page), sizeOrDefault(size), tagType,
                enabled);
    }

    @Override
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createDefinition(tenantIdOrDefault(tenantId), safePayload(payload), actorOrDefault(actor),
                now());
    }

    @Override
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateDefinition(tenantIdOrDefault(tenantId), requirePositiveId(id), safePayload(payload),
                actorOrDefault(actor), now());
    }

    @Override
    public Map<String, Object> delete(Long tenantId, Long id, String actor) {
        return catalog.deleteDefinition(tenantIdOrDefault(tenantId), requirePositiveId(id), actorOrDefault(actor),
                now());
    }

    @Override
    public Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled) {
        return catalog.listValues(tenantIdOrDefault(tenantId), requireText(tagCode, "tagCode"), enabled);
    }

    @Override
    public Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload, String actor) {
        return catalog.createValue(tenantIdOrDefault(tenantId), requireText(tagCode, "tagCode"), safePayload(payload),
                actorOrDefault(actor), now());
    }

    @Override
    public Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateValue(tenantIdOrDefault(tenantId), requirePositiveId(id), safePayload(payload),
                actorOrDefault(actor), now());
    }

    @Override
    public Map<String, Object> deleteValue(Long tenantId, Long id, String actor) {
        return catalog.deleteValue(tenantIdOrDefault(tenantId), requirePositiveId(id), actorOrDefault(actor), now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static Integer pageOrDefault(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static Integer sizeOrDefault(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 200);
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static Long requirePositiveId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
