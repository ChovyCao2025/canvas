package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.plugin.PluginRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canvas/plugins")
@RequiredArgsConstructor
public class PluginRegistryController {

    private final PluginRegistryService service;

    @GetMapping
    public Mono<R<Map<String, List<PluginRegistryService.Plugin>>>> catalog() {
        return Mono.fromCallable(service::groupedCatalog)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{pluginKey}/enabled")
    public Mono<R<Void>> setEnabled(
            @PathVariable String pluginKey,
            @RequestHeader(name = "X-Canvas-Version", defaultValue = "1.0.0") String canvasVersion,
            @RequestBody EnableRequest request) {
        return Mono.<Void>fromRunnable(() -> service.setEnabled(pluginKey, request.enabled(), canvasVersion))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    public record EnableRequest(boolean enabled) {}
}
