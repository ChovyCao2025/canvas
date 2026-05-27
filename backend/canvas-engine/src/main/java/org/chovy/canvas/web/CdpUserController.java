package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CdpUserDirectoryService;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserInsightService;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CanvasUserDetailDTO;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
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

/**
 * CDP 用户 HTTP 控制器，根路由为 {@code /cdp/users}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/cdp/users")
@RequiredArgsConstructor
public class CdpUserController {

    private final CdpUserDirectoryService directoryService;
    private final CdpUserInsightService insightService;
    private final CdpUserService userService;
    private final CdpTagService tagService;

    @GetMapping
    public Mono<R<List<CanvasUserRowDTO>>> list(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        return Mono.fromCallable(() -> R.ok(directoryService.listUsers(keyword)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}")
    public Mono<R<CdpUserDetailDTO>> get(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(userService.toDetail(userService.getRequiredProfile(userId))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{userId}/insight")
    public Mono<R<CanvasUserDetailDTO>> getInsight(@PathVariable String userId) {
        return Mono.fromCallable(() -> R.ok(insightService.getUserInsight(userId)))
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
