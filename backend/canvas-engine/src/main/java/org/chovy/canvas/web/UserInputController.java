package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.canvas.UserInputService;
import org.chovy.canvas.dto.canvas.UserInputSubmitReq;
import org.chovy.canvas.dto.canvas.UserInputSubmitResp;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * UserInputController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/user-input")
public class UserInputController {

    private final UserInputService service;

    /**
     * 创建 UserInputController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public UserInputController(UserInputService service) {
        this.service = service;
    }
    /**
     * 提交用户输入请求接口，对应 POST /responses/{responseId}/submit。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 副作用：会提交业务请求。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param responseId response ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含提交用户输入请求后的业务数据。
     */
    @PostMapping("/responses/{responseId}/submit")
    public Mono<R<UserInputSubmitResp>> submit(
            @PathVariable Long responseId,
            @RequestBody UserInputSubmitReq req) {
        return Mono.fromCallable(() -> service.submit(responseId, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
