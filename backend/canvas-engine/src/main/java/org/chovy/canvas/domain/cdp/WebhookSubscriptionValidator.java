package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.common.OutboundUrlValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebhookSubscriptionValidator {

    public void validate(String callbackUrl, List<String> eventTypes) {
        try {
            OutboundUrlValidator.validateHttpUrl(callbackUrl);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("callbackUrl is not allowed: " + ex.getMessage(), ex);
        }
        List<String> normalized = eventTypes == null ? List.of() : eventTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventTypes cannot be empty");
        }
    }
}
