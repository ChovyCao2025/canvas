package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

public record BiEmailDeliveryRequest(
        String from,
        List<String> to,
        String subject,
        String text,
        List<BiEmailAttachment> attachments
) {
    public BiEmailDeliveryRequest(String from, List<String> to, String subject, String text) {
        this(from, to, subject, text, List.of());
    }

    public BiEmailDeliveryRequest {
        to = to == null ? List.of() : List.copyOf(to);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
