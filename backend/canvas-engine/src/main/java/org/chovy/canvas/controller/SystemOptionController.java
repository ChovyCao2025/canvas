package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.SystemOption;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/admin/system-options")
@RequiredArgsConstructor
public class SystemOptionController {

    private final SystemOptionService service;

    @GetMapping
    public Mono<R<PageResult<SystemOption>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String keyword) {
        return Mono.fromCallable(() -> {
                    List<SystemOption> rows = service.listForAdmin(category, enabled, keyword);
                    return PageResult.of(rows.size(), rows);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody SystemOption body) {
        return Mono.<Void>fromRunnable(() -> service.updateEditable(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
