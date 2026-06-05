package org.chovy.canvas.engine.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Composes read-only marketing policy checks into an operator-facing contactability report.
 */
@Service
@RequiredArgsConstructor
public class ContactabilityExplainerService {

    private final MarketingPolicyService policyService;

    /** Builds a full explanation without consuming frequency quota. */
    public Report explain(Request request) {
        String channel = normalize(request.channel());
        List<Check> checks = List.of(
                check("CONSENT", policyService.consentAllowed(
                        request.userId(), channel, request.requireExplicitConsent())),
                check("SUPPRESSION", policyService.suppressionAllowed(request.userId(), channel)),
                check("CHANNEL", policyService.channelAvailable(request.userId(), channel)),
                check("QUIET_HOURS", policyService.quietHoursAllowed(
                        request.userId(), request.quietStart(), request.quietEnd(), request.quietTimezone())),
                check("FREQUENCY", policyService.previewFrequency(
                        request.userId(),
                        request.canvasId(),
                        request.nodeId(),
                        request.frequencyScope(),
                        channel,
                        request.frequencyMax(),
                        request.frequencyWindow()))
        );
        boolean allowed = checks.stream().allMatch(Check::allowed);
        return new Report(request.userId(), channel, allowed, checks);
    }

    private Check check(String checkKey, MarketingPolicyService.PolicyDecision decision) {
        return new Check(checkKey, decision.allowed(), decision.reasonCode(), decision.reasonMessage());
    }

    private String normalize(String channel) {
        return channel == null || channel.isBlank() ? "ALL" : channel.toUpperCase(Locale.ROOT);
    }

    public record Request(
            String userId,
            String channel,
            boolean requireExplicitConsent,
            String quietStart,
            String quietEnd,
            String quietTimezone,
            Long canvasId,
            String nodeId,
            String frequencyScope,
            int frequencyMax,
            Duration frequencyWindow) {
    }

    public record Report(
            String userId,
            String channel,
            boolean allowed,
            List<Check> checks) {
    }

    public record Check(
            String checkKey,
            boolean allowed,
            String reasonCode,
            String reasonMessage) {
    }
}
