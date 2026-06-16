package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MauticInsightFacade;
import org.chovy.canvas.marketing.domain.MauticInsightCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排MauticInsight相关的应用层用例。
 */
@Service
public class MauticInsightApplicationService implements MauticInsightFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final MauticInsightCatalog catalog;

    /**
     * 创建MauticInsightApplicationService实例。
     */
    public MauticInsightApplicationService() {
        this(new MauticInsightCatalog());
    }

    /**
     * 创建MauticInsightApplicationService实例。
     */
    public MauticInsightApplicationService(MauticInsightCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行audienceMembership业务操作。
     */
    @Override
    public Map<String, Object> audienceMembership(Long audienceId, String userId) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId is required");
        }
        return catalog.audienceMembership(audienceId, required(userId, "userId"));
    }

    /**
     * 执行journeyPath业务操作。
     */
    @Override
    public Map<String, Object> journeyPath(String executionId) {
        return catalog.journeyPath(required(executionId, "executionId"));
    }

    /**
     * 执行channelPreference业务操作。
     */
    @Override
    public Map<String, Object> channelPreference(String userId, String preferredChannel) {
        return catalog.channelPreference(required(userId, "userId"), preferredChannel);
    }

    /**
     * 执行suppressionTimeline业务操作。
     */
    @Override
    public Map<String, Object> suppressionTimeline(String userId) {
        return catalog.suppressionTimeline(required(userId, "userId"));
    }

    /**
     * 执行publishHealth业务操作。
     */
    @Override
    public Map<String, Object> publishHealth(Long canvasId) {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        return catalog.publishHealth(canvasId);
    }

    /**
     * 执行frequencyTemplates业务操作。
     */
    @Override
    public List<Map<String, Object>> frequencyTemplates() {
        return catalog.frequencyTemplates();
    }

    /**
     * 校验并返回d必填值。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
