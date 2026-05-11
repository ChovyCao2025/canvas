package org.chovy.canvas.auth.controller;

import org.chovy.canvas.auth.domain.*;
import org.chovy.canvas.auth.dto.*;
import org.chovy.canvas.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserService userService;

    @GetMapping
    public Mono<R<List<SysUser>>> list() {
        return Mono.fromCallable(userService::listAll)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PostMapping
    public Mono<R<SysUser>> create(@RequestBody UserCreateReq req) {
        return Mono.fromCallable(() ->
                userService.create(req.getUsername(), req.getPassword(),
                        req.getDisplayName(), req.getRole()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody UserUpdateReq req) {
        return Mono.<Void>fromRunnable(() ->
                userService.update(id, req.getDisplayName(), req.getPassword(), req.getRole()))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }

    @PutMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return Mono.<Void>fromRunnable(() -> userService.disable(id))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.<Void>ok());
    }
}
