package org.chovy.canvas.domain.monitoring;

public interface MarketingMonitorPollClient {

    boolean supports(String sourceType);

    MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request);
}
