package org.chovy.canvas.cdp.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpTagDefinitionFacade;
import org.chovy.canvas.cdp.domain.CdpTagDefinitionCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpTagDefinition 的应用服务流程。
 */
@Service
public class CdpTagDefinitionApplicationService implements CdpTagDefinitionFacade {

    /**
     * 领域目录组件。
     */
    private final CdpTagDefinitionCatalog catalog;

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * 创建当前组件实例。
     */
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

    /**
     * 查询list列表。
     */
    @Override
    public Map<String, Object> list(Long tenantId, Integer page, Integer size, String tagType, Integer enabled) {
        return catalog.listDefinitions(tenantIdOrDefault(tenantId), pageOrDefault(page), sizeOrDefault(size), tagType,
                enabled);
    }

    /**
     * 创建create。
     */
    @Override
    public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createDefinition(tenantIdOrDefault(tenantId), safePayload(payload), actorOrDefault(actor),
                now());
    }

    /**
     * 更新update。
     */
    @Override
    public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateDefinition(tenantIdOrDefault(tenantId), requirePositiveId(id), safePayload(payload),
                actorOrDefault(actor), now());
    }

    /**
     * 删除delete。
     */
    @Override
    public Map<String, Object> delete(Long tenantId, Long id, String actor) {
        return catalog.deleteDefinition(tenantIdOrDefault(tenantId), requirePositiveId(id), actorOrDefault(actor),
                now());
    }

    /**
     * 查询Values列表。
     */
    @Override
    public Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled) {
        return catalog.listValues(tenantIdOrDefault(tenantId), requireText(tagCode, "tagCode"), enabled);
    }

    /**
     * 创建Value。
     */
    @Override
    public Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload, String actor) {
        return catalog.createValue(tenantIdOrDefault(tenantId), requireText(tagCode, "tagCode"), safePayload(payload),
                actorOrDefault(actor), now());
    }

    /**
     * 更新Value。
     */
    @Override
    public Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateValue(tenantIdOrDefault(tenantId), requirePositiveId(id), safePayload(payload),
                actorOrDefault(actor), now());
    }

    /**
     * 删除Value。
     */
    @Override
    public Map<String, Object> deleteValue(Long tenantId, Long id, String actor) {
        return catalog.deleteValue(tenantIdOrDefault(tenantId), requirePositiveId(id), actorOrDefault(actor), now());
    }

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 执行 tenantIdOrDefault 对应的 CDP 业务操作。
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 pageOrDefault 对应的 CDP 业务操作。
     */
    private static Integer pageOrDefault(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /**
     * 执行 sizeOrDefault 对应的 CDP 业务操作。
     */
    private static Integer sizeOrDefault(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 200);
    }

    /**
     * 返回安全的Payload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 读取并校验必填的Positive Id。
     */
    private static Long requirePositiveId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }

    /**
     * 读取并校验必填的Text。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
