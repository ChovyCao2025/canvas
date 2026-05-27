package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
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

/**
 * 系统选项 HTTP 控制器，根路由为 {@code /admin/system-options}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/admin/system-options")
@RequiredArgsConstructor
public class SystemOptionController {

    private final SystemOptionService service;

    @GetMapping
    public Mono<R<PageResult<SystemOptionDO>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String keyword) {
        return Mono.fromCallable(() -> {
                    List<SystemOptionDO> rows = service.listForAdmin(category, enabled, keyword);
                    return PageResult.of(rows.size(), rows);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody SystemOptionDO body) {
        return Mono.<Void>fromRunnable(() -> service.updateEditable(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
