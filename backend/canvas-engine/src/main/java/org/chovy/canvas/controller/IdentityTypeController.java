package org.chovy.canvas.controller;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.IdentityType;
import org.chovy.canvas.domain.meta.IdentityTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/identity-types")
@RequiredArgsConstructor
public class IdentityTypeController {

    private final IdentityTypeService identityTypeService;

    @GetMapping
    public Mono<R<PageResult<IdentityType>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Integer allowImport) {
        return Mono.fromCallable(() -> {
            List<IdentityType> all = identityTypeService.list(enabled, allowImport);
            int fromIndex = Math.max((page - 1) * size, 0);
            int toIndex = Math.min(fromIndex + size, all.size());
            List<IdentityType> pageList = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
            return R.ok(PageResult.of(all.size(), pageList));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<IdentityType>> create(@RequestBody IdentityType body) {
        return Mono.fromCallable(() -> R.ok(identityTypeService.create(body)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody IdentityType body) {
        return Mono.<Void>fromRunnable(() -> identityTypeService.update(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> identityTypeService.delete(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
