package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CanvasUserQueryService;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/{id}/users")
@RequiredArgsConstructor
public class CanvasUserController {

    private final CanvasUserQueryService service;

    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.listUsers(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}")
    public Mono<R<CanvasUserRowDTO>> get(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.getUserInCanvas(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/executions")
    public Mono<R<List<CanvasExecution>>> executions(@PathVariable Long id, @PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(service.listExecutions(id, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
