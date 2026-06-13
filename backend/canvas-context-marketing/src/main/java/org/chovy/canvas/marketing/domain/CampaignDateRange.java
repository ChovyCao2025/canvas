package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;

public record CampaignDateRange(LocalDateTime startAt, LocalDateTime endAt) {

    public CampaignDateRange {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
    }

    public static CampaignDateRange of(LocalDateTime startAt, LocalDateTime endAt) {
        return new CampaignDateRange(startAt, endAt);
    }
}
