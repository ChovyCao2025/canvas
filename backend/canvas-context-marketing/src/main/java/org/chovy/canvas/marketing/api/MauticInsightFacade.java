package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MauticInsightFacade的营销上下文访问契约。
 */
public interface MauticInsightFacade {

    /**
     * 执行audienceMembership业务操作。
     */
    Map<String, Object> audienceMembership(Long audienceId, String userId);

    /**
     * 执行journeyPath业务操作。
     */
    Map<String, Object> journeyPath(String executionId);

    /**
     * 执行channelPreference业务操作。
     */
    Map<String, Object> channelPreference(String userId, String preferredChannel);

    /**
     * 执行suppressionTimeline业务操作。
     */
    Map<String, Object> suppressionTimeline(String userId);

    /**
     * 执行publishHealth业务操作。
     */
    Map<String, Object> publishHealth(Long canvasId);

    /**
     * 执行frequencyTemplates业务操作。
     */
    List<Map<String, Object>> frequencyTemplates();
}
