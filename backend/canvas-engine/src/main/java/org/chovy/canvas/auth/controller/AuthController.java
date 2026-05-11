package org.chovy.canvas.auth.controller;

import org.chovy.canvas.auth.domain.*;
import org.chovy.canvas.auth.dto.*;
import org.chovy.canvas.common.R;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<R<LoginResp>> login(@RequestBody LoginReq req) {
        return Mono.fromCallable(() -> {
            SysUser user = userService.findByUsername(req.getUsername());
            if (user == null || user.getEnabled() == 0)
                throw new IllegalArgumentException("用户名或密码错误");
            if (!userService.checkPassword(user, req.getPassword()))
                throw new IllegalArgumentException("用户名或密码错误");

            String token = jwtUtil.generate(user);
            LoginResp resp = new LoginResp();
            resp.setToken(token);
            resp.setUserId(user.getId());
            resp.setUsername(user.getUsername());
            resp.setDisplayName(user.getDisplayName());
            resp.setRole(user.getRole());
            return R.ok(resp);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/logout")
    public Mono<R<Void>> logout() {
        // 前端清 token 即可；如需服务端黑名单，可在此写入 Redis
        return Mono.just(R.ok());
    }

    @GetMapping("/me")
    public Mono<R<LoginResp>> me() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Claims.class)
                .flatMap(claims -> Mono.fromCallable(() -> {
                    Long userId = Long.parseLong(claims.getSubject());
                    SysUser user = userService.findById(userId);
                    if (user == null) throw new IllegalArgumentException("用户不存在");
                    LoginResp resp = new LoginResp();
                    resp.setUserId(user.getId());
                    resp.setUsername(user.getUsername());
                    resp.setDisplayName(user.getDisplayName());
                    resp.setRole(user.getRole());
                    return R.ok(resp);
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
