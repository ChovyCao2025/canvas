package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MauticInsightFacade;
import org.chovy.canvas.marketing.domain.MauticInsightCatalog;
import org.springframework.stereotype.Service;

@Service
public class MauticInsightApplicationService implements MauticInsightFacade {

    private final MauticInsightCatalog catalog;

    public MauticInsightApplicationService() {
        this(new MauticInsightCatalog());
    }

    public MauticInsightApplicationService(MauticInsightCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> audienceMembership(Long audienceId, String userId) {
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId is required");
        }
        return catalog.audienceMembership(audienceId, required(userId, "userId"));
    }

    @Override
    public Map<String, Object> journeyPath(String executionId) {
        return catalog.journeyPath(required(executionId, "executionId"));
    }

    @Override
    public Map<String, Object> channelPreference(String userId, String preferredChannel) {
        return catalog.channelPreference(required(userId, "userId"), preferredChannel);
    }

    @Override
    public Map<String, Object> suppressionTimeline(String userId) {
        return catalog.suppressionTimeline(required(userId, "userId"));
    }

    @Override
    public Map<String, Object> publishHealth(Long canvasId) {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        return catalog.publishHealth(canvasId);
    }

    @Override
    public List<Map<String, Object>> frequencyTemplates() {
        return catalog.frequencyTemplates();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
