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

@RestController
@RequestMapping("/user-input")
public class UserInputController {

    private final UserInputService service;

    public UserInputController(UserInputService service) {
        this.service = service;
    }

    @PostMapping("/responses/{responseId}/submit")
    public Mono<R<UserInputSubmitResp>> submit(
            @PathVariable Long responseId,
            @RequestBody UserInputSubmitReq req) {
        return Mono.fromCallable(() -> service.submit(responseId, req))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
