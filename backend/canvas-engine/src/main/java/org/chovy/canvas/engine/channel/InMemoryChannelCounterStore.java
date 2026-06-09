package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

/**
 * InMemoryChannelCounterStore 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class InMemoryChannelCounterStore extends ProviderBackpressureService.InMemoryCounterStore {
}
