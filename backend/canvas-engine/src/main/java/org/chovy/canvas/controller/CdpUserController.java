package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagHistoryDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/cdp/users")
@RequiredArgsConstructor
public class CdpUserController {

    private final CdpUserService userService;
    private final CdpTagService tagService;

    @GetMapping("/{userId}")
    public Mono<R<CdpUserDetailDTO>> get(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(userService.toDetail(userService.getRequiredProfile(userId))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/tags")
    public Mono<R<List<CdpUserTagDTO>>> listTags(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(tagService.listCurrentTags(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/tag-history")
    public Mono<R<List<CdpUserTagHistoryDTO>>> listTagHistory(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(tagService.listHistory(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{userId}/tags")
    public Mono<R<Void>> addTag(@PathVariable String userId, @RequestBody CdpTagWriteReq req) {
        return Mono.fromCallable(() -> {
            tagService.setTag(userId, req);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{userId}/tags/{tagCode}")
    public Mono<R<Void>> removeTag(@PathVariable String userId, @PathVariable String tagCode) {
        return Mono.fromCallable(() -> {
            tagService.removeTag(userId, tagCode, "用户详情移除标签", null);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
