package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

@Component
public class InMemoryChannelCounterStore extends ProviderBackpressureService.InMemoryCounterStore {
}
