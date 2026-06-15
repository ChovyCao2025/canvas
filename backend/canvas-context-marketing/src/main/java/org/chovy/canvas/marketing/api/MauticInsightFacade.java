package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MauticInsightFacade {

    Map<String, Object> audienceMembership(Long audienceId, String userId);

    Map<String, Object> journeyPath(String executionId);

    Map<String, Object> channelPreference(String userId, String preferredChannel);

    Map<String, Object> suppressionTimeline(String userId);

    Map<String, Object> publishHealth(Long canvasId);

    List<Map<String, Object>> frequencyTemplates();
}
