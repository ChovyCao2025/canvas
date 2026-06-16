package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.ProgrammaticDspFacade;
import org.chovy.canvas.marketing.domain.ProgrammaticDspCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排ProgrammaticDsp相关的应用层用例。
 */
@Service
public class ProgrammaticDspApplicationService implements ProgrammaticDspFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final ProgrammaticDspCatalog catalog;

    /**
     * 创建ProgrammaticDspApplicationService实例。
     */
    public ProgrammaticDspApplicationService() {
        this(Clock.systemDefaultZone());
    }

    ProgrammaticDspApplicationService(Clock clock) {
        this.catalog = new ProgrammaticDspCatalog(clock);
    }

    /**
     * 执行upsertSeat业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSeat(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSeat(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行upsertCampaign业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertCampaign(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行upsertLineItem业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertLineItem(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertLineItem(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行upsertSupplyPath业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertSupplyPath(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertSupplyPath(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行recordSnapshot业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.recordSnapshot(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行summary业务操作。
     */
    @Override
    public Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        return catalog.summary(safeTenantId(tenantId), query);
    }

    /**
     * 执行proposeMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.proposeMutation(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 执行approveMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.approveMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    /**
     * 执行executeMutation业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        return catalog.executeMutation(safeTenantId(tenantId), mutationId, payload, actorOrDefault(actor));
    }

    /**
     * 查询mutations列表。
     */
    @Override
    public List<Map<String, Object>> listMutations(Long tenantId, Map<String, Object> query) {
        return catalog.listMutations(safeTenantId(tenantId), query);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
