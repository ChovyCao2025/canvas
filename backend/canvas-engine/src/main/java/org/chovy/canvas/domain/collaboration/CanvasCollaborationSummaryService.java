package org.chovy.canvas.domain.collaboration;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanvasCollaborationSummaryService {

    private final SummaryRepository repository;

    public CanvasCollaborationSummaryService() {
        this((tenantId, canvasId) -> new Summary(canvasId, List.of(), 0, 0, 0));
    }

    public CanvasCollaborationSummaryService(SummaryRepository repository) {
        this.repository = repository;
    }

    public Summary summary(Long tenantId, Long canvasId) {
        Summary summary = repository.summary(tenantId, canvasId);
        return summary == null ? new Summary(canvasId, List.of(), 0, 0, 0) : summary;
    }

    public record Summary(Long canvasId,
                          List<Presence> presence,
                          int activeLockCount,
                          int openCommentCount,
                          int unreadNotificationCount) {
        public Summary {
            presence = presence == null ? List.of() : List.copyOf(presence);
        }
    }

    public record Presence(String userId, String displayName, String state) {
    }

    public interface SummaryRepository {
        Summary summary(Long tenantId, Long canvasId);
    }
}
