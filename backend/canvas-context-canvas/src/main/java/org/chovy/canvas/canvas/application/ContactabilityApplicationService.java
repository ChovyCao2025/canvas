package org.chovy.canvas.canvas.application;

import java.util.List;
import java.util.Locale;

import org.chovy.canvas.canvas.api.ContactabilityFacade;
import org.springframework.stereotype.Service;

@Service
public class ContactabilityApplicationService implements ContactabilityFacade {

    @Override
    public Report explain(Request request) {
        String channel = normalize(request.channel());
        List<Check> checks = List.of(
                new Check("CONSENT", true, null, null),
                new Check("SUPPRESSION", true, null, null),
                new Check("CHANNEL", true, null, null),
                new Check("QUIET_HOURS", true, null, null),
                new Check("FREQUENCY", true, null, null));
        return new Report(request.userId(), channel, true, checks);
    }

    private static String normalize(String channel) {
        return channel == null || channel.isBlank() ? "ALL" : channel.toUpperCase(Locale.ROOT);
    }
}
