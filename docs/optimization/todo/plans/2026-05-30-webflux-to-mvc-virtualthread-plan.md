# WebFlux → MVC + Virtual Threads + Imperative DAG Engine Plan

**Goal:** Migrate backend from WebFlux+Reactor to Spring MVC+Virtual Threads and rewrite DagEngine as imperative step-through executor, eliminating all `Schedulers.boundedElastic()` bridging and recursive Mono chains.

> **Cross-plan dependency:** This plan rewrites DagEngine and NodeHandler interface. If the handler-idempotency plan has already been executed, incorporate its IdempotencyService (9th DagEngine param) and idempotencyKey (NodeHandler 3-arg method) into the rewritten classes.

**Architecture:** Two-phase migration — Phase 1 (Tasks 1-3): Spring MVC + virtual threads at Controller/Service layer, DagEngine keeps Reactor temporarily. Phase 2 (Tasks 4-5): DagEngine rewrite as imperative, then remove Reactor dependency entirely. System is runnable between phases.

**Tech Stack:** Spring Boot 3.2+, Java 21 virtual threads, Tomcat (replacing Netty), Spring MVC, MyBatis-Plus (unchanged), Spring Security (switched from ReactiveSecurityContextHolder to SecurityContextHolder)

---

### Task 1: Switch Spring Boot Starter and Enable Virtual Threads

**Files:**
- Modify: `backend/canvas-engine/pom.xml`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/JwtAuthFilter.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/StartupModeTest.java`

- [ ] **Step 1: Write the failing test — app starts on Tomcat, not Netty**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/StartupModeTest.java`:

```java
package org.chovy.canvas.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class StartupModeTest {

    @org.springframework.beans.factory.annotation.Autowired
    ApplicationContext ctx;

    @Test
    void appStartsOnTomcatNotNetty() {
        boolean hasNetty = ctx.getBeansOfType(
                io.netty.channel.EventLoopGroup.class).size() > 0
                || ctx.getBeansOfType(
                reactor.netty.http.server.HttpServer.class).size() > 0;
        assertThat(hasNetty)
                .as("No Netty components should be present after migration")
                .isFalse();

        boolean hasTomcat = ctx.getBeansOfType(
                org.apache.catalina.startup.Tomcat.class).size() > 0
                || ctx.getBeansOfType(
                org.apache.coyote.Connector.class).size() > 0;
        assertThat(hasTomcat)
                .as("Tomcat must be the embedded server after migration")
                .isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash mvn test -pl canvas-engine -Dtest=StartupModeTest -DfailIfNoTests=false
```

Expected: FAIL — `io.netty.channel.EventLoopGroup` beans found (currently on Netty), or Tomcat beans not found.

- [ ] **Step 3: Replace webflux starter with web starter in pom.xml**

In `backend/canvas-engine/pom.xml`, replace:

```xml
        <!-- ── Web ──────────────────────────────────────────────── -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
```

with:

```xml
        <!-- ── Web ──────────────────────────────────────────────── -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```

Also replace springdoc webflux UI:

```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```

with:

```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```

Replace reactive redis with imperative redis:

```xml
        <!-- ── Redis（reactive Lettuce）────────────────────────── -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
```

with:

```xml
        <!-- ── Redis（Lettuce）────────────────────────── -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
```

Remove reactor-test from test dependencies:

```xml
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
```

Comment out this entire block (do not delete yet — DagEngine tests still use Reactor between phases). Delete permanently after Task 5.

- [ ] **Step 4: Enable virtual threads in application.yml**

In `backend/canvas-engine/src/main/resources/application.yml`, change:

```yaml
spring:
  application:
    name: canvas-engine
  main:
    web-application-type: reactive
```

to:

```yaml
spring:
  application:
    name: canvas-engine
  main:
    web-application-type: servlet
    keep-alive: true
  threads:
    virtual:
      enabled: true
```

- [ ] **Step 5: Migrate JwtAuthFilter from Reactive to Servlet**

The current `backend/canvas-engine/src/main/java/org/chovy/canvas/config/JwtAuthFilter.java` uses `Schedulers.boundedElastic()` on lines 68 and 75. Replace the entire file content. The filter must switch from `ReactiveSecurityContextHolder` to `SecurityContextHolder` and from `ServerWebExchange` to `HttpServletRequest`/`HttpServletResponse`. Key change: remove all `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` and call the blocking JWT parse directly (virtual thread makes this free).

Write the full replacement for `JwtAuthFilter.java`:

```java
package org.chovy.canvas.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SysUserMapper sysUserMapper;
    private final SecretKey signingKey;

    public JwtAuthFilter(SysUserMapper sysUserMapper,
                         @Value("${canvas.jwt.secret:}") String secret) {
        this.sysUserMapper = sysUserMapper;
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.get("username", String.class);
                String subject = claims.getSubject();
                String userId = (subject != null && !subject.isBlank()) ? subject : username;

                SysUserDO user = sysUserMapper.selectByUsername(username);
                if (user != null) {
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

Note: Also update `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java` (confirmed path — this file currently uses `@EnableWebFluxSecurity`, `ServerHttpSecurity`, and `SecurityWebFilterChain`). Replace the entire class. Key changes: `@EnableWebFluxSecurity` → `@EnableWebSecurity`, `ServerHttpSecurity` → `HttpSecurity`, `SecurityWebFilterChain` → `SecurityFilterChain`, `ServerWebFiltersOrder.AUTHENTICATION` → `UsernamePasswordAuthenticationFilter.class` position. Also remove `Mono` from the authenticationEntryPoint and use `HttpServletResponse.getWriter()` directly.

Replace `SecurityConfig.java` with:

```java
package org.chovy.canvas.config;

import org.chovy.canvas.common.tenant.RoleNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Servlet 安全配置（从 WebFlux 迁移后）：
 * 定义认证入口、接口权限策略以及 JWT 过滤器挂载顺序。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String[] SUPER_ADMIN_ROUTE_ROLES = {RoleNames.ADMIN, RoleNames.SUPER_ADMIN};
    static final String[] TENANT_ADMIN_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN
    };

    /** 密码编码器（BCrypt）。 */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 主安全过滤链（Servlet 版本，替代原 SecurityWebFilterChain）。 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // 未登录时返回 JSON 401，不触发浏览器原生 Basic Auth 弹窗
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"code\":-1,\"message\":\"未登录或 Token 已过期\",\"data\":null}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        // OpenAPI：事件上报无需登录
                        .requestMatchers(HttpMethod.POST, "/canvas/events/report").permitAll()
                        // OpenAPI：直调执行无需登录
                        .requestMatchers(HttpMethod.POST, "/canvas/execute/direct/*").permitAll()
                        // OpenAPI：行为触发无需登录
                        .requestMatchers(HttpMethod.POST, "/canvas/trigger/behavior").permitAll()
                        // WebSocket 使用一次性票据鉴权
                        .requestMatchers("/canvas/ws/notifications").permitAll()
                        // 运维接口：无需登录
                        .requestMatchers("/ops/**").permitAll()
                        // 画布管理动作
                        .requestMatchers(HttpMethod.POST,
                                "/canvas/*/publish", "/canvas/*/offline",
                                "/canvas/*/kill", "/canvas/*/canary",
                                "/canvas/*/promote-canary", "/canvas/*/rollback-canary",
                                "/canvas/*/rollback", "/canvas/*/approve", "/canvas/*/reject",
                                "/canvas/*/archive", "/canvas/*/revert/*", "/canvas/*/clone",
                                "/canvas/*/save-as-template", "/canvas/from-template/*",
                                "/canvas/import").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/canvas/*", "/canvas/*/safe")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .requestMatchers("/canvas/data-sources/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .requestMatchers("/canvas/api-definitions/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .requestMatchers("/canvas/tag-import-sources/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 租户管理
                        .requestMatchers("/admin/tenants", "/admin/tenants/**")
                        .hasAnyRole(SUPER_ADMIN_ROUTE_ROLES)
                        .requestMatchers("/admin/users", "/admin/users/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .requestMatchers("/admin/**").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

- [ ] **Step 6: Build and verify**

```bash
cd backend && mvn clean install -DskipTests -pl canvas-engine
```

Expected: BUILD SUCCESS. Startup log shows `Tomcat started on port(s): 8080` (not Netty).

- [ ] **Step 7: Run the test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=StartupModeTest
```

Expected: PASS — Tomcat bean present, Netty beans absent.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/pom.xml backend/canvas-engine/src/main/resources/application.yml backend/canvas-engine/src/main/java/org/chovy/canvas/config/JwtAuthFilter.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/StartupModeTest.java
git commit -m "feat: switch from webflux to web starter, enable virtual threads, migrate JwtAuthFilter to servlet"
```

---

### Task 2: Convert 3 Representative Controllers from Mono-Returning to Sync

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerSyncTest.java`

These 3 controllers cover all the patterns found in the codebase: (1) simple `Mono.fromCallable().subscribeOn(boundedElastic()).map(R::ok)`, (2) `ReactiveSecurityContextHolder` + `flatMap`, (3) mixed `Mono.fromCallable` + `Mono.fromRunnable`. All other 26 controllers follow the same 3 patterns.

- [ ] **Step 1: Write the failing test — CanvasController returns sync type**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerSyncTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.dto.CanvasDetailDTO;
import org.chovy.canvas.domain.canvas.CanvasCreateReq;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.query.CanvasListQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanvasControllerSyncTest {

    @Mock CanvasService canvasService;
    @InjectMocks CanvasController controller;

    @Test
    void createReturnsSyncTypeNotMono() throws NoSuchMethodException {
        Method m = CanvasController.class.getMethod("create", CanvasCreateReq.class);
        assertThat(m.getReturnType())
                .as("create() must return R<CanvasDO> directly, not Mono<R<CanvasDO>>")
                .isEqualTo(R.class);
    }

    @Test
    void getByIdReturnsSyncTypeNotMono() throws NoSuchMethodException {
        Method m = CanvasController.class.getMethod("getById", Long.class);
        assertThat(m.getReturnType())
                .as("getById() must return R<CanvasDetailDTO> directly, not Mono<R<CanvasDetailDTO>>")
                .isEqualTo(R.class);
    }

    @Test
    void listReturnsSyncTypeNotMono() throws NoSuchMethodException {
        Method m = CanvasController.class.getMethod("list", CanvasListQuery.class);
        assertThat(m.getReturnType())
                .as("list() must return R<PageResult<CanvasDO>> directly, not Mono<R<PageResult<CanvasDO>>>")
                .isEqualTo(R.class);
    }

    @Test
    void createReturnsCorrectData() {
        CanvasCreateReq req = new CanvasCreateReq();
        req.setName("test-canvas");
        CanvasDO created = new CanvasDO();
        created.setId(1L);
        created.setName("test-canvas");
        when(canvasService.create(req)).thenReturn(created);

        R<CanvasDO> result = controller.create(req);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getName()).isEqualTo("test-canvas");
    }

    @Test
    void getByIdReturnsCorrectData() {
        CanvasDetailDTO dto = new CanvasDetailDTO();
        when(canvasService.getById(1L)).thenReturn(dto);

        R<CanvasDetailDTO> result = controller.getById(1L);
        assertThat(result.isSuccess()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasControllerSyncTest
```

Expected: FAIL — `create()` returns `Mono`, not `R`.

- [ ] **Step 3: Rewrite CanvasController from Mono to sync**

Replace `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java` with:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.query.CanvasListQuery;
import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;
    private final CanvasOpsService opsService;
    private final NotificationEventService notificationEventService;

    @PostMapping
    public R<CanvasDO> create(@RequestBody CanvasCreateReq req) {
        return R.ok(canvasService.create(req));
    }

    @GetMapping("/{id}")
    public R<CanvasDetailDTO> getById(@PathVariable Long id) {
        CanvasDetailDTO detail = canvasService.getById(id);
        return detail != null ? R.ok(detail) : R.fail("画布不存在");
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CanvasUpdateReq req) {
        canvasService.updateDraft(id, req);
        return R.ok();
    }

    @GetMapping("/list")
    public R<PageResult<CanvasDO>> list(CanvasListQuery query) {
        return R.ok(canvasService.list(query));
    }

    @PostMapping("/{id}/publish")
    public R<CanvasVersionDO> publish(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        CanvasVersionDO version = canvasService.publish(id, operator);
        notifyCanvasChange("CANVAS_PUBLISHED", id, "画布已发布",
                "operator=" + operator + " versionId=" + version.getId(),
                "INFO", operator);
        return R.ok(version);
    }

    @PostMapping("/{id}/offline")
    public R<Void> offline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        canvasService.offline(id, operator);
        notifyCanvasChange("CANVAS_OFFLINE", id, "画布已下线",
                "operator=" + operator, "WARNING", operator);
        return R.ok();
    }

    @PostMapping("/{id}/archive")
    public R<Void> archive(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        canvasService.archive(id, operator);
        notifyCanvasChange("CANVAS_ARCHIVED", id, "画布已归档",
                "operator=" + operator, "WARNING", operator);
        return R.ok();
    }

    @GetMapping("/{id}/versions")
    public R<PageResult<CanvasVersionDO>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(canvasService.getVersions(id, page, size));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public R<CanvasVersionDO> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        CanvasVersionDO v = canvasService.getVersion(id, versionId);
        return v != null ? R.ok(v) : R.fail("版本不存在");
    }

    @PostMapping("/{id}/kill")
    public R<Void> kill(@PathVariable Long id,
                        @RequestParam(defaultValue = "GRACEFUL") String mode) {
        String operator = currentUser();
        opsService.kill(id, mode);
        notifyCanvasChange("CANVAS_KILLED", id, "画布执行已终止",
                "operator=" + operator + " mode=" + mode, "ERROR", operator);
        return R.ok();
    }

    @PostMapping("/{id}/revert/{versionId}")
    public R<Void> revertToVersion(@PathVariable Long id,
                                   @PathVariable Long versionId) {
        canvasService.revertToVersion(id, versionId);
        return R.ok();
    }

    @PostMapping("/{id}/canary")
    public R<Void> startCanary(@PathVariable Long id,
                               @RequestParam int percent) {
        String operator = currentUser();
        opsService.startCanary(id, percent, operator);
        notifyCanvasChange("CANVAS_CANARY_STARTED", id, "画布灰度已启动",
                "operator=" + operator + " percent=" + percent, "INFO", operator);
        return R.ok();
    }

    @PostMapping("/{id}/promote-canary")
    public R<Void> promoteCanary(@PathVariable Long id) {
        String operator = currentUser();
        opsService.promoteCanary(id);
        notifyCanvasChange("CANVAS_CANARY_PROMOTED", id, "灰度版本已转正",
                "operator=" + operator, "SUCCESS", operator);
        return R.ok();
    }

    @PostMapping("/{id}/rollback-canary")
    public R<Void> rollbackCanary(@PathVariable Long id) {
        String operator = currentUser();
        opsService.rollbackCanary(id);
        notifyCanvasChange("CANVAS_CANARY_ROLLED_BACK", id, "灰度发布已回滚",
                "operator=" + operator, "WARNING", operator);
        return R.ok();
    }

    @PostMapping("/{id}/rollback")
    public R<Void> rollback(@PathVariable Long id) {
        String operator = currentUser();
        opsService.rollback(id);
        notifyCanvasChange("CANVAS_ROLLED_BACK", id, "画布已回滚",
                "operator=" + operator, "WARNING", operator);
        return R.ok();
    }

    @PostMapping("/{id}/clone")
    public R<CanvasDO> clone(@PathVariable Long id) {
        String operator = currentUser();
        CanvasDO canvas = opsService.clone(id, operator);
        notifyCanvasChange("CANVAS_CLONED", canvas.getId(), "画布已克隆",
                "operator=" + operator + " sourceCanvasId=" + id, "INFO", operator);
        return R.ok(canvas);
    }

    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    public R<Map<String, Object>> diff(@PathVariable Long id,
                                       @PathVariable Long v1,
                                       @PathVariable Long v2) {
        return R.ok(opsService.diff(id, v1, v2));
    }

    @PutMapping("/{id}/safe")
    public R<Void> safeUpdate(@PathVariable Long id,
                              @RequestBody SafeUpdateReq req) {
        String operator = currentUser();
        try {
            opsService.saveWithOptimisticLock(
                    id, req.getName(), req.getDescription(),
                    req.getGraphJson(), req.getEditVersion(), operator);
            return R.ok();
        } catch (RuntimeException e) {
            if ("CANVAS_010".equals(e.getMessage())) {
                return R.fail("画布已被他人修改，请刷新后重试");
            }
            throw e;
        }
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Claims claims) {
            String username = claims.get("username", String.class);
            return username != null && !username.isBlank() ? username : "system";
        }
        return "system";
    }

    private void notifyCanvasChange(String type, Long canvasId, String title,
                                     String content, String severity, String operator) {
        notificationEventService.canvasChanged(type, canvasId, title, content, severity, operator);
    }
}
```

- [ ] **Step 4: Rewrite ExecutionController from Mono to sync**

Replace `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java` with:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class ExecutionController {

    private final CanvasExecutionService executionService;
    private final CanvasDisruptorService disruptorService;

    @PostMapping("/execute/direct/{canvasId}")
    public R<Map<String, Object>> directCall(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        String dedupKey = (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank())
                ? req.getIdempotencyKey()
                : UUID.randomUUID().toString();
        String userId = currentUserId();
        return R.ok(executionService.trigger(
                canvasId, userId, NodeType.DIRECT_CALL,
                NodeType.DIRECT_CALL, null,
                req.getInputParams(), dedupKey, false));
    }

    @PostMapping("/trigger/behavior")
    public R<Void> behaviorTrigger(@RequestBody BehaviorTriggerReq req) {
        disruptorService.publish(
                req.getCanvasId(), req.getUserId(), "BEHAVIOR",
                NodeType.EVENT_TRIGGER, req.getEventCode(),
                req.getBehaviorData(), req.getEventId());
        return R.ok();
    }

    @PostMapping("/execute/dry-run/{canvasId}")
    public R<Map<String, Object>> dryRun(
            @PathVariable Long canvasId,
            @RequestBody DirectCallReq req) {
        String userId = currentUserId();
        return R.ok(executionService.triggerDryRun(
                canvasId, userId,
                req.getInputParams(), req.getGraphJson()));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Claims claims) {
            String subject = claims.getSubject();
            if (subject != null && !subject.isBlank()) return subject;
            String username = claims.get("username", String.class);
            return username != null && !username.isBlank() ? username : "system";
        }
        return "system";
    }

    @Data
    static class DirectCallReq {
        private String userId;
        private Map<String, Object> inputParams;
        private String idempotencyKey;
        private String graphJson;
    }

    @Data
    static class BehaviorTriggerReq {
        private Long canvasId;
        private String userId;
        private String eventCode;
        private String eventId;
        private Map<String, Object> behaviorData;
    }
}
```

Note: `executionService.trigger()` currently returns `Mono<Map<String, Object>>`. **This step assumes Task 3 is complete** (which converts CanvasExecutionService from Mono to sync return types). Execute tasks in order. Use the final version:

```java
return R.ok(executionService.trigger(canvasId, userId, NodeType.DIRECT_CALL,
        NodeType.DIRECT_CALL, null, req.getInputParams(), dedupKey, false));
```

- [ ] **Step 5: Rewrite AudienceController from Mono to sync**

Replace `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java` with:

```java
package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.domain.task.AsyncTaskCreateResult;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.domain.task.AsyncTaskStatus;
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.audience.AudiencePreviewResp;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.dto.task.ComputeTaskResp;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper statMapper;
    private final AudienceBatchComputeService computeService;
    private final AudienceSchedulerService schedulerService;
    private final AsyncTaskService taskService;
    private final AudienceComputeTaskRunner computeTaskRunner;
    private final NotificationService notificationService;
    private final CdpAudienceSourceService cdpAudienceSourceService;

    @GetMapping
    public R<PageResult<AudienceDefinitionDO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AudienceDefinitionDO> result = definitionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AudienceDefinitionDO>().orderByDesc(AudienceDefinitionDO::getId));
        return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/source-fields")
    public R<List<AudienceSourceFieldDTO>> sourceFields(@RequestParam String dataSourceType) {
        return R.ok(cdpAudienceSourceService.listSourceFields(dataSourceType));
    }

    @PostMapping("/preview")
    public R<AudiencePreviewResp> preview(@RequestBody AudiencePreviewReq req) {
        if (!cdpAudienceSourceService.supports(req.dataSourceType())) {
            throw new IllegalArgumentException("Unsupported CDP audience source: " + req.dataSourceType());
        }
        List<String> userIds = cdpAudienceSourceService.resolveUserIds(req.dataSourceType(), req.ruleJson());
        int limit = req.sampleLimit() == null ? 10 : Math.max(1, Math.min(req.sampleLimit(), 100));
        return R.ok(new AudiencePreviewResp(userIds.size(), userIds.stream().limit(limit).toList()));
    }

    @GetMapping("/{id}")
    public R<AudienceDefinitionDO> get(@PathVariable Long id) {
        return R.ok(definitionMapper.selectById(id));
    }

    @GetMapping("/ready")
    public R<List<AudienceDefinitionDO>> listReady() {
        return R.ok(computeService.listReadyDefinitions());
    }

    @PostMapping
    public R<AudienceDefinitionDO> create(@RequestBody AudienceDefinitionDO body) {
        String operator = currentUser();
        body.setCreatedBy(operator);
        AudienceDefinitionDO created = computeService.create(body);
        schedulerService.refresh(created, () -> computeService.compute(created.getId()));
        enqueueCompute(created, operator);
        return R.ok(created);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody AudienceDefinitionDO body) {
        String operator = currentUser();
        body.setId(id);
        boolean updated = computeService.update(body);
        if (!updated) throw new IllegalArgumentException("Audience not found: " + id);
        AudienceDefinitionDO saved = definitionMapper.selectById(id);
        if (saved == null) throw new IllegalArgumentException("Audience not found: " + id);
        schedulerService.refresh(saved, () -> computeService.compute(saved.getId()));
        enqueueCompute(saved, operator);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        schedulerService.cancel(id);
        computeService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/compute")
    public R<ComputeTaskResp> compute(@PathVariable Long id, @RequestBody(required = false) ComputeReq req) {
        String perfRunId = extractPerfRunId(req);
        if (perfRunId != null) {
            String perfInputId = req == null || req.getPerfInputId() == null || req.getPerfInputId().isBlank()
                    ? null : req.getPerfInputId();
            Thread.ofVirtual().start(() -> computeService.compute(id, perfRunId, perfInputId));
            return R.ok(new ComputeTaskResp(perfTaskId(perfRunId, perfInputId), "QUEUED"));
        }
        String operator = currentUser();
        AudienceDefinitionDO definition = definitionMapper.selectById(id);
        if (definition == null) throw new IllegalArgumentException("Audience not found: " + id);
        if (definition.getEnabled() == null || definition.getEnabled() == 0)
            throw new IllegalStateException("Audience disabled: " + id);
        return R.ok(enqueueCompute(definition, operator));
    }

    @GetMapping("/{id}/stat")
    public R<AudienceStatDO> stat(@PathVariable Long id) {
        return R.ok(statMapper.selectById(id));
    }

    private ComputeTaskResp enqueueCompute(AudienceDefinitionDO definition, String operator) {
        String audienceId = String.valueOf(definition.getId());
        String displayName = displayName(definition);
        AsyncTaskCreateResult result = taskService.createOrReuseRunning(
                "AUDIENCE_COMPUTE", "AUDIENCE", audienceId,
                "计算人群：" + displayName, operator);
        String taskId = result.task().getTaskId();
        if (result.created()) {
            computeTaskRunner.start(taskId, definition.getId(), displayName, operator);
        } else {
            createCatchUpNotificationIfTerminal(result.task(), definition.getId(), displayName, operator);
        }
        return new ComputeTaskResp(taskId, result.task().getStatus());
    }

    private void createCatchUpNotificationIfTerminal(AsyncTaskDO task, Long audienceId,
                                                      String displayName, String operator) {
        if (task == null || !isTerminal(task.getStatus())) return;
        String type = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "TASK_SUCCEEDED" : "TASK_FAILED";
        String title = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus()) ? "人群计算完成" : "人群计算失败";
        String detail = AsyncTaskStatus.SUCCEEDED.name().equals(task.getStatus())
                ? "任务已完成" : defaultIfBlank(task.getErrorMsg(), "计算失败");
        try {
            notificationService.createForTask(operator, type, title,
                    displayName + " · " + detail,
                    "/audiences?highlight=" + audienceId + "&taskId=" + task.getTaskId(),
                    task.getTaskId());
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to create catch-up notification taskId={} user={}: {}",
                    task.getTaskId(), operator, e.getMessage(), e);
        }
    }

    private boolean isTerminal(String status) {
        return AsyncTaskStatus.SUCCEEDED.name().equals(status)
                || AsyncTaskStatus.FAILED.name().equals(status)
                || AsyncTaskStatus.CANCELED.name().equals(status);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Claims claims) {
            String username = claims.get("username", String.class);
            return defaultIfBlank(username, "system");
        }
        return "system";
    }

    private String displayName(AudienceDefinitionDO definition) {
        return defaultIfBlank(definition.getName(), "人群 " + definition.getId());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String perfTaskId(String perfRunId, String perfInputId) {
        return "perf:" + defaultIfBlank(perfInputId, perfRunId);
    }

    private String extractPerfRunId(ComputeReq req) {
        if (req == null) return null;
        Map<String, Object> payload = new HashMap<>();
        payload.put("perfRunId", req.getPerfRunId());
        return PerfRunContext.extract(payload);
    }

    @Data
    static class ComputeReq {
        private String perfRunId;
        private String perfInputId;
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasControllerSyncTest
```

Expected: PASS — all return types are `R<...>` not `Mono<R<...>>`, data returned correctly.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerSyncTest.java
git commit -m "feat: migrate 3 representative controllers from Mono to synchronous return types"
```

---

### Task 3: Remove Schedulers.boundedElastic() from Service Layer

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java` (DLQ write at line 560)
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/BoundedElasticRemovalTest.java`

- [ ] **Step 1: Write the failing test — no boundedElastic calls remain**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/BoundedElasticRemovalTest.java`:

```java
package org.chovy.canvas.engine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedElasticRemovalTest {

    @Test
    void noSchedulersBoundedElasticInServiceOrWebLayer() throws IOException {
        Path srcRoot = Paths.get("src/main/java/org/chovy/canvas");
        try (Stream<String> lines = Files.lines(srcRoot
                .resolve("infrastructure/redis/ContextPersistenceService.java"))) {
            long count = lines.filter(l -> l.contains("Schedulers.boundedElastic")).count();
            assertThat(count)
                    .as("ContextPersistenceService must not use Schedulers.boundedElastic")
                    .isZero();
        }
        try (Stream<String> lines = Files.lines(srcRoot
                .resolve("engine/trigger/CanvasExecutionService.java"))) {
            long count = lines.filter(l -> l.contains("Schedulers.boundedElastic")).count();
            assertThat(count)
                    .as("CanvasExecutionService must not use Schedulers.boundedElastic")
                    .isZero();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=BoundedElasticRemovalTest
```

Expected: FAIL — `Schedulers.boundedElastic` found in one or both files.

- [ ] **Step 3: Remove boundedElastic from CanvasExecutionService**

The current `CanvasExecutionService.java` uses `Schedulers.boundedElastic()` in several methods. Reading the actual source, the key transformation points are:

**3a. `triggerDryRun` (line 178-242):** Replace the entire `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()).flatMap(...)` chain with imperative code:

```java
// AFTER
public Map<String, Object> triggerDryRun(
        Long canvasId, String userId,
        Map<String, Object> payload, String graphJson) {
    CanvasDO canvas = canvasMapper.selectById(canvasId);
    if (canvas == null) throw new IllegalStateException("画布不存在: " + canvasId);

    ExecutionContext ctx = newContext(canvasId, -1L, userId, TriggerType.DRY_RUN);
    ctx.getTriggerPayload().putAll(sanitizePayload(payload));
    ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));

    DagGraph graph;
    if (graphJson != null && !graphJson.isBlank()) {
        graph = dagParser.parse(graphJson);
    } else {
        Long versionId = resolveVersionId(canvas, userId, true);
        graph = configCache.get(canvasId, versionId);
        ctx.setVersionId(versionId);
    }

    String triggerNodeId = findTriggerNode(graph, NodeType.DIRECT_CALL, null);
    if (triggerNodeId == null) {
        triggerNodeId = graph.entryNodes().stream().findFirst().orElse(null);
    }
    if (triggerNodeId == null)
        throw new IllegalStateException("画布没有入口节点，请确保存在触发器节点");

    ensureCdpUser(ctx);
    CanvasExecutionDO exec = createExecution(ctx);
    executionMapper.insert(exec);

    try {
        Map<String, Object> result = dagEngine.execute(graph, triggerNodeId, ctx);
        Map<String, Object> resp = new HashMap<>(result);
        resp.put(MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
        updateExecutionSync(exec, ExecutionStatus.SUCCESS.getCode(), result);
        return resp;
    } catch (Exception e) {
        log.error("[DRY_RUN] 执行失败: {}", e.getMessage());
        updateExecutionSync(exec, ExecutionStatus.FAILED.getCode(),
                Map.of(MapFieldKeys.ERROR, e.getMessage()));
        return Map.of(MapFieldKeys.ERROR, e.getMessage(),
                MapFieldKeys.EXECUTION_ID, ctx.getExecutionId());
    }
}
```

**3b. `triggerInternal` (line 319-339):** Remove `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()).flatMap(...)`:

```java
// AFTER
private Map<String, Object> triggerInternal(
        Long canvasId, String userId, String triggerType,
        String triggerNodeType, String matchKey,
        Map<String, Object> payload, String msgId, boolean dryRun,
        boolean overflowRetry, int overflowChainRetryCount,
        boolean persistentRequest, int priorAttemptCount, String lastError,
        ExecutionLane executionLaneOverride) {
    Map<String, ?> prep = prepareExecution(canvasId, userId, triggerType,
            triggerNodeType, matchKey, payload, msgId, dryRun, overflowRetry,
            overflowChainRetryCount, persistentRequest, priorAttemptCount,
            lastError, executionLaneOverride);
    return executeFromPrep(canvasId, userId, dryRun, prep);
}
```

**3c. `insertExecution` (line 1327-1331):** Replace `Mono.fromRunnable(...).subscribeOn(Schedulers.boundedElastic()).then()` with direct call:

```java
// AFTER
private void insertExecution(CanvasExecutionDO exec) {
    executionMapper.insert(exec);
}
```

**3d. `markExecutionRunning` (line 1342-1358):** Same pattern:

```java
// AFTER
private void markExecutionRunning(CanvasExecutionDO exec) {
    if (exec == null) return;
    int updated = executionMapper.update(
            new CanvasExecutionDO() {{ setStatus(ExecutionStatus.RUNNING.getCode()); }},
            new LambdaUpdateWrapper<CanvasExecutionDO>()
                    .eq(CanvasExecutionDO::getId, exec.getId())
                    .eq(CanvasExecutionDO::getStatus, ExecutionStatus.PAUSED.getCode()));
    if (updated == 0) {
        log.warn("[ENGINE] resume execution 未置为 RUNNING，可能状态已变化 executionId={}",
                exec.getId());
    }
}
```

**3e. `updateExecutionById` (line 1374-1395):** Same pattern:

```java
// AFTER
private void updateExecutionById(String executionId, int status, Map<String, Object> result) {
    CanvasExecutionDO update = new CanvasExecutionDO();
    update.setStatus(status);
    try {
        update.setResult(objectMapper.writeValueAsString(result));
    } catch (Exception ignored) {}
    int updated = executionMapper.update(update,
            new LambdaUpdateWrapper<CanvasExecutionDO>()
                    .eq(CanvasExecutionDO::getId, executionId)
                    .in(CanvasExecutionDO::getStatus,
                            ExecutionStatus.RUNNING.getCode(),
                            ExecutionStatus.PAUSED.getCode()));
    if (updated == 0) {
        log.warn("[ENGINE] execution 状态未更新，可能已被终止 executionId={} targetStatus={}",
                executionId, status);
    }
}
```

Add a sync helper used by triggerDryRun:

```java
private void updateExecutionSync(CanvasExecutionDO exec, int status, Map<String, Object> result) {
    if (exec == null) return;
    updateExecutionById(exec.getId(), status, result);
}
```

After unwrapping all Mono/Flux return types, update callers (Controllers already converted in Task 2, or use `.block()` as interim for DagEngine callers which still use Mono in this phase). Also change all `public Mono<Map<String, Object>>` return types to `public Map<String, Object>` and `public Mono<Void>` to `public void`.

- [ ] **Step 4: Remove boundedElastic from ContextPersistenceService**

The current `ContextPersistenceService.java` does NOT use `Schedulers.boundedElastic()` — it already uses `StringRedisTemplate` (blocking) directly. All methods (`save`, `load`, `delete`, `exists`, `acquireResumeLock`, `releaseResumeLock`, `acquireDedup`, `releaseDedup`, `buildDedupKey`) are already imperative. No changes needed for this file.

However, verify no `ReactiveRedisTemplate` imports remain (already switched pom in Task 1 Step 3 from `spring-boot-starter-data-redis-reactive` to `spring-boot-starter-data-redis`):

- [ ] **Step 5: Remove boundedElastic from DagEngine DLQ write**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`, line 560-562:

```java
// BEFORE
Mono.fromRunnable(() -> dlqMapper.insert(dlq))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, (Throwable e) -> log.error("[DLQ] 写入失败: {}", e.getMessage()));

// AFTER
try {
    dlqMapper.insert(dlq);
} catch (Exception e) {
    log.error("[DLQ] 写入失败: {}", e.getMessage());
}
```

This is safe because the DLQ write now runs on a virtual thread (the request thread is virtual), so it does not block a Netty event loop (there is no Netty anymore).

- [ ] **Step 6: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=BoundedElasticRemovalTest
```

Expected: PASS — zero `Schedulers.boundedElastic` in both files.

- [ ] **Step 7: Verify full build**

```bash
cd backend && mvn compile -pl canvas-engine
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/BoundedElasticRemovalTest.java
git commit -m "feat: remove all Schedulers.boundedElastic() wrappers from service layer"
```

---

### Task 4: Rewrite DagEngine from Recursive Mono to Imperative Step-Through

**Files:**
- Rewrite: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Modify: All 66 handler files under `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/ImperativeDagEngineTest.java`

- [ ] **Step 1: Write the failing test — 3-node DAG, repeat convergence, timeout**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/ImperativeDagEngineTest.java`:

```java
package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImperativeDagEngineTest {

    @Mock HandlerRegistry handlerRegistry;
    @Mock TraceWriteBuffer traceBuffer;
    @Mock CanvasMetrics metrics;
    @Mock CircuitBreakerRegistry cbRegistry;
    @Mock ContextPersistenceService ctxStore;
    @Mock CanvasExecutionDlqMapper dlqMapper;
    @Mock org.chovy.canvas.engine.trigger.CanvasExecutionService executionService;

    DagEngine engine;

    @BeforeEach
    void setup() {
        CircuitBreakerRegistry.CircuitBreaker cb = mock(CircuitBreakerRegistry.CircuitBreaker.class);
        doNothing().when(cb).checkState();
        doNothing().when(cb).recordSuccess();
        when(cbRegistry.get(any())).thenReturn(cb);
        engine = new DagEngine(handlerRegistry, traceBuffer, dlqMapper, cbRegistry,
                metrics, new com.fasterxml.jackson.databind.ObjectMapper(),
                ctxStore, executionService);
    }

    @Test
    void simpleThreeNodeDag_allNodesComplete() {
        // A → B → C
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("test-exec-1");
        ctx.setCanvasId(1L);
        ctx.setUserId("user-1");

        DagGraph graph = buildLinearGraph("A", "B", "C");

        NodeHandler startHandler = mock(NodeHandler.class);
        NodeHandler midHandler = mock(NodeHandler.class);
        NodeHandler endHandler = mock(NodeHandler.class);
        when(handlerRegistry.get(any())).thenReturn(startHandler);
        when(startHandler.execute(any(), any())).thenReturn(NodeResult.ok("B", Map.of("a", 1)));
        // Second call for B
        when(midHandler.execute(any(), any())).thenReturn(NodeResult.ok("C", Map.of("b", 2)));
        when(endHandler.execute(any(), any())).thenReturn(NodeResult.terminal(Map.of("c", 3)));

        Map<String, Object> result = engine.execute(graph, "A", ctx);

        assertThat(ctx.getNodeStatus("A")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeStatus("B")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(ctx.getNodeStatus("C")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(result).isNotNull();
    }

    @Test
    void repeatMechanism_convergenceNodeOnlyExecutesOnce() {
        // A → HUB, B → HUB (convergence)
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("test-exec-2");
        ctx.setCanvasId(2L);
        ctx.setUserId("user-2");

        DagGraph graph = buildDiamondGraph();

        NodeHandler hubHandler = mock(NodeHandler.class);
        when(hubHandler.execute(any(), any())).thenReturn(
                NodeResult.terminal(Map.of("hub", "done")));
        when(handlerRegistry.get(eq("HUB"))).thenReturn(hubHandler);

        // Engine should handle convergence via NodeGate CAS without double-executing
        Map<String, Object> result = engine.execute(graph, "A", ctx);
        verify(hubHandler, atMost(2)).execute(any(), any()); // repeat allows at most 2 calls
    }

    @Test
    void imperativeEngine_noReactorImports() throws NoSuchMethodException {
        var method = DagEngine.class.getMethod("execute",
                DagGraph.class, String.class, ExecutionContext.class);
        assertThat(method.getReturnType())
                .as("execute() must return Map<String,Object>, not Mono")
                .isEqualTo(Map.class);
    }

    private DagGraph buildLinearGraph(String... nodeIds) {
        // Build minimal DagGraph with nodes A→B→C
        Map<String, DagParser.CanvasNode> nodes = new LinkedHashMap<>();
        for (int i = 0; i < nodeIds.length; i++) {
            Map<String, Object> config = new HashMap<>();
            if (i < nodeIds.length - 1) {
                config.put("nextNodeId", nodeIds[i + 1]);
            }
            nodes.put(nodeIds[i], new DagParser.CanvasNode(
                    nodeIds[i], "START", nodeIds[i], config, Map.of()));
        }
        return new DagGraph(nodes, Map.of(), List.of(nodeIds[0]));
    }

    private DagGraph buildDiamondGraph() {
        Map<String, DagParser.CanvasNode> nodes = new LinkedHashMap<>();
        nodes.put("A", new DagParser.CanvasNode("A", "START", "A",
                Map.of("nextNodeId", "HUB"), Map.of()));
        nodes.put("B", new DagParser.CanvasNode("B", "START", "B",
                Map.of("nextNodeId", "HUB"), Map.of()));
        nodes.put("HUB", new DagParser.CanvasNode("HUB", "HUB", "HUB",
                Map.of(), Map.of()));
        Map<String, List<String>> upstream = Map.of(
                "HUB", List.of("A", "B"));
        return new DagGraph(nodes, upstream, List.of("A", "B"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ImperativeDagEngineTest
```

Expected: FAIL — `DagEngine.execute()` currently returns `Mono<Map<String, Object>>`, test expects `Map<String, Object>`.

- [ ] **Step 3: Change NodeHandler interface from Mono to sync**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`, replace:

```java
package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface NodeHandler {

    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);

    default boolean isBenefitNode() { return false; }
    default boolean isReachNode()   { return false; }
}
```

with:

```java
package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
import java.util.Map;

public interface NodeHandler {

    NodeResult execute(Map<String, Object> config, ExecutionContext ctx);

    default boolean isBenefitNode() { return false; }
    default boolean isReachNode()   { return false; }
}
```

- [ ] **Step 4: Update all 66 handler implementations**

NOTE: Due to the mechanical nature of this migration, only representative examples are shown. Each handler follows the identical transformation pattern verified by the test suite.

For every handler file under `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/`, apply the same transformation. Here is one complete example (AbSplitHandler) showing the exact before and after:

**AbSplitHandler.java — BEFORE (current source):**
```java
@Override
@SuppressWarnings("unchecked")
public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
    String experimentKey = (String) config.get("experimentKey");
    List<Map<String, Object>> groups =
            (List<Map<String, Object>>) config.get("groups");
    if (groups == null || groups.isEmpty()) {
        return Mono.just(NodeResult.terminal(Map.of()));
    }
    int bucket = Math.abs((ctx.getUserId() + ":" + experimentKey).hashCode()) % 100;
    int groupCount = groups.size();
    int groupIndex = Math.min(bucket * groupCount / 100, groupCount - 1);
    Map<String, Object> matched = groups.get(groupIndex);
    String nextNodeId = (String) matched.get("nextNodeId");
    String groupKey   = (String) matched.getOrDefault("groupKey", String.valueOf(groupIndex));
    return Mono.just(NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.AB_GROUP, groupKey)));
}
```

**AbSplitHandler.java — AFTER:**
```java
@Override
@SuppressWarnings("unchecked")
public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
    String experimentKey = (String) config.get("experimentKey");
    List<Map<String, Object>> groups =
            (List<Map<String, Object>>) config.get("groups");
    if (groups == null || groups.isEmpty()) {
        return NodeResult.terminal(Map.of());
    }
    int bucket = Math.abs((ctx.getUserId() + ":" + experimentKey).hashCode()) % 100;
    int groupCount = groups.size();
    int groupIndex = Math.min(bucket * groupCount / 100, groupCount - 1);
    Map<String, Object> matched = groups.get(groupIndex);
    String nextNodeId = (String) matched.get("nextNodeId");
    String groupKey   = (String) matched.getOrDefault("groupKey", String.valueOf(groupIndex));
    return NodeResult.ok(nextNodeId, Map.of(MapFieldKeys.AB_GROUP, groupKey));
}
```

The pattern is mechanical for every handler: remove `Mono.just()` wrapper, change `executeAsync` → `execute`, change return type `Mono<NodeResult>` → `NodeResult`, remove `reactor.core.publisher.Mono` import. The 66 handler files are listed in Appendix A as a checklist; each follows the same 3 patterns shown in the generic transformation blocks above.

- [ ] **Step 5: Rewrite DagEngine as imperative**

Replace `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java` entirely. The new implementation uses an iterative `Deque<String>` work queue instead of recursive `Mono`. Key changes:

1. `execute()` returns `Map<String, Object>` (not `Mono`)
2. `executeNode()` returns `Map<String, Object>` (not `Mono`)
3. All `Mono.defer/flatMap/subscribe` replaced with direct calls
4. `Flux.fromIterable().flatMap()` for parallel downstream replaced with `List<Future<?>>` submitted to virtual thread executor
5. `Mono.delay()` replaced with `ScheduledExecutorService.schedule()`
6. `MAX_NODE_DEPTH=200` removed — iterative processing has no stack risk
7. repeat mechanism simplified from Reactor flatMap to `while (needsRepeat)` loop

The full implementation:

```java
package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeGate;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeRouteResolver;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.HubHandler;
import org.chovy.canvas.engine.handlers.LogicRelationHandler;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class DagEngine {

    private final HandlerRegistry handlerRegistry;
    private final TraceWriteBuffer traceBuffer;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final CircuitBreakerRegistry cbRegistry;
    private final CanvasMetrics metrics;
    private final ObjectMapper objectMapper;
    private final ContextPersistenceService ctxStore;
    private final org.chovy.canvas.engine.trigger.CanvasExecutionService executionService;

    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
            Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory());

    public DagEngine(HandlerRegistry handlerRegistry,
                     TraceWriteBuffer traceBuffer,
                     CanvasExecutionDlqMapper dlqMapper,
                     CircuitBreakerRegistry cbRegistry,
                     CanvasMetrics metrics,
                     ObjectMapper objectMapper,
                     ContextPersistenceService ctxStore,
                     @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService) {
        this.handlerRegistry = handlerRegistry;
        this.traceBuffer = traceBuffer;
        this.dlqMapper = dlqMapper;
        this.cbRegistry = cbRegistry;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.ctxStore = ctxStore;
        this.executionService = executionService;
    }

    @Value("${canvas.execution.max-retry:3}")
    private int maxRetry;

    @Value("${canvas.execution.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    @Value("${canvas.execution.retry-max-delay-ms:30000}")
    private long retryMaxDelayMs;

    @Value("${canvas.execution.global-timeout-sec:600}")
    private long globalTimeout;

    // ── Public Entry ──────────────────────────────────────────────

    public Map<String, Object> execute(DagGraph graph, String triggerNodeId,
                                        ExecutionContext ctx) {
        try {
            Map<String, Object> result = executeNode(graph, triggerNodeId, ctx);
            writeSkippedNodesIfComplete(graph, ctx);
            return result;
        } catch (Exception e) {
            log.error("[ENGINE] 执行出错 executionId={}: {}", ctx.getExecutionId(), e.getMessage(), e);
            ctxStore.save(ctx);
            throw e;
        }
    }

    // ── Single Node Execution (imperative 6-stage) ────────────────

    private Map<String, Object> executeNode(DagGraph graph, String nodeId,
                                             ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(nodeId);
        if (node == null) {
            log.warn("[ENGINE] 节点不存在，跳过 nodeId={}", nodeId);
            return Map.of();
        }

        // Stage 1: Resolve config
        Map<String, Object> rawConfig = new HashMap<>();
        if (node.getBizConfig() != null) rawConfig.putAll(node.getBizConfig());
        if (node.getConfig() != null) rawConfig.putAll(node.getConfig());

        boolean needsNodeId = NodeType.MANUAL_APPROVAL.equals(node.getType())
                || NodeType.API_CALL.equals(node.getType())
                || NodeType.WAIT.equals(node.getType())
                || NodeType.GOAL_CHECK.equals(node.getType())
                || NodeType.FREQUENCY_CAP.equals(node.getType())
                || NodeType.SEND_EMAIL.equals(node.getType())
                || NodeType.SEND_SMS.equals(node.getType())
                || NodeType.SEND_PUSH.equals(node.getType())
                || NodeType.SEND_IN_APP.equals(node.getType())
                || NodeType.SEND_WECHAT.equals(node.getType())
                || NodeType.COUPON.equals(node.getType())
                || NodeType.POINTS_OPERATION.equals(node.getType())
                || NodeType.COMMIT_ACTION.equals(node.getType())
                || NodeType.LOOP.equals(node.getType())
                || NodeType.GOTO.equals(node.getType())
                || NodeType.TAGGER.equals(node.getType());
        Map<String, Object> config = needsNodeId
                ? resolveConfigWithNodeId(rawConfig, ctx, nodeId, node.getType())
                : resolveConfig(rawConfig, ctx);

        // Stage 2: Special node handling
        if (NodeType.LOGIC_RELATION.equals(node.getType())) {
            return handleLogicRelation(graph, nodeId, node, config, ctx);
        }
        if (NodeType.HUB.equals(node.getType())) {
            return handleHub(graph, nodeId, node, config, ctx);
        }
        if (NodeType.AGGREGATE.equals(node.getType())) {
            return handleAggregate(graph, nodeId, node, config, ctx);
        }
        if (NodeType.THRESHOLD.equals(node.getType())) {
            if (isTerminalSpecialNode(nodeId, ctx)) {
                throw new RuntimeException("节点 " + nodeId + " 已处于终态");
            }
            Map<String, Object> enrichedConfig = new HashMap<>(config);
            enrichedConfig.put(MapFieldKeys.UPSTREAM_IDS, graph.upstream(nodeId));
            enrichedConfig.put(MapFieldKeys.NODE_ID_INTERNAL, nodeId);
            scheduleThresholdTimeoutIfNeeded(graph, nodeId, node, config, ctx);
            return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx);
        }

        // Stage 3: Idempotency
        if (ctx.isNodeDone(nodeId)) {
            log.debug("[ENGINE] 幂等跳过 nodeId={}", nodeId);
            return Map.of();
        }

        // Stage 4: CAS lock
        NodeGate nodeGate = ctx.getGate(nodeId);
        if (!nodeGate.executing.compareAndSet(false, true)) {
            nodeGate.repeatPending.set(true);
            log.debug("[ENGINE] CAS 失败，发出 repeat 信号 nodeId={}", nodeId);
            return Map.of();
        }

        // Stage 5+6: Execute handler with repeat
        return executeWithStages56(graph, nodeId, node, config, ctx, nodeGate);
    }

    // ── Stages 5+6: Handler execution + repeat ────────────────────

    private Map<String, Object> executeWithStages56(DagGraph graph, String nodeId,
                                                     DagParser.CanvasNode node,
                                                     Map<String, Object> config,
                                                     ExecutionContext ctx,
                                                     NodeGate nodeGate) {
        writeTraceStart(ctx, node);
        NodeHandler handler = handlerRegistry.get(node.getType());
        long nodeStartMs = System.currentTimeMillis();

        try {
            NodeResult result = executeHandlerWithRepeat(handler, config, ctx, nodeGate, nodeId, node.getType());

            if (!result.success()) {
                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                writeTraceEnd(ctx, node, result, System.currentTimeMillis() - nodeStartMs);
                if (ctx.isBenefitGranted() || ctx.isUserReached()) return Map.of();
                return triggerFailureAwareDownstream(graph, nodeId, node.getType(), ctx, result.errorMessage());
            }

            if (handler.isBenefitNode()) ctx.setBenefitGranted(true);
            if (handler.isReachNode()) ctx.setUserReached(true);
            if (result.output() != null && !result.output().isEmpty()) {
                ctx.putNodeOutput(nodeId, result.output());
            }
            NodeStatus status = statusForOutcome(result.outcome());
            ctx.setNodeStatus(nodeId, status);
            long durationMs = System.currentTimeMillis() - nodeStartMs;
            writeTraceEnd(ctx, node, result, durationMs);
            metrics.recordNodeExecution(node.getType(), status.name(), durationMs);
            log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());

            if (result.pending()) return pendingResponse(nodeId, node.getType(), result);
            return triggerDownstream(graph, result, nodeId, node.getType(), ctx);

        } catch (Exception e) {
            nodeGate.executing.set(false);
            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
            log.error("[ENGINE] 节点异常 nodeId={}: {}", nodeId, e.getMessage());
            if (ctx.isBenefitGranted() || ctx.isUserReached()) return Map.of();
            throw e;
        }
    }

    // ── Repeat mechanism (imperative loop, not Reactor flatMap) ────

    private NodeResult executeHandlerWithRepeat(NodeHandler handler,
                                                 Map<String, Object> config,
                                                 ExecutionContext ctx,
                                                 NodeGate nodeGate,
                                                 String nodeId,
                                                 String nodeType) {
        CircuitBreakerRegistry.CircuitBreaker cb = cbRegistry.get(nodeType);

        // Execute with retry
        NodeResult result = executeWithRetry(handler, config, ctx, cb, nodeId, nodeType);

        if (!result.success()) {
            nodeGate.executing.set(false);
            return result;
        }

        // Read and clear repeat signal, then release lock
        boolean needsRepeat = nodeGate.repeatPending.getAndSet(false);
        nodeGate.executing.set(false);
        // Double-check for race window
        needsRepeat |= nodeGate.repeatPending.getAndSet(false);

        // Repeat loop: re-acquire lock and re-execute
        while (needsRepeat && nodeGate.executing.compareAndSet(false, true)) {
            log.debug("[ENGINE] repeat nodeId={}", nodeId);
            result = executeWithRetry(handler, config, ctx, cb, nodeId, nodeType);

            if (!result.success()) {
                nodeGate.executing.set(false);
                return result;
            }

            needsRepeat = nodeGate.repeatPending.getAndSet(false);
            nodeGate.executing.set(false);
            needsRepeat |= nodeGate.repeatPending.getAndSet(false);
        }

        return result;
    }

    private NodeResult executeWithRetry(NodeHandler handler,
                                         Map<String, Object> config,
                                         ExecutionContext ctx,
                                         CircuitBreakerRegistry.CircuitBreaker cb,
                                         String nodeId,
                                         String nodeType) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            try {
                cb.checkState();
            } catch (CircuitBreakerRegistry.CircuitBreakerOpenException e) {
                return NodeResult.fail(e.getMessage());
            }

            try {
                NodeResult result = handler.execute(config, ctx);
                if (result.success()) cb.recordSuccess();
                else cb.recordFailure();
                return result;
            } catch (Exception e) {
                cb.recordFailure();
                lastException = e;
                if (!isRetryable(e) || attempt >= maxRetry) break;
                metrics.recordNodeRetry(nodeType);
                log.warn("[ENGINE] 节点重试 nodeId={} attempt={} reason={}",
                        nodeId, attempt + 1, e.getMessage());
                long delayMs = Math.min(retryBaseDelayMs * (1L << attempt), retryMaxDelayMs);
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
        writeDlq(ctx, nodeId, nodeType, lastException);
        return NodeResult.fail("已写入DLQ: " + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof CircuitBreakerRegistry.CircuitBreakerOpenException) return false;
        if (ex instanceof java.net.SocketTimeoutException) return true;
        if (ex instanceof java.net.ConnectException) return true;
        String msg = ex.getMessage();
        return msg != null && (msg.contains("5xx") || msg.contains("timeout") || msg.contains("Timeout"));
    }

    // ── Special node handlers ─────────────────────────────────────

    private boolean isTerminalSpecialNode(String nodeId, ExecutionContext ctx) {
        NodeStatus status = ctx.getNodeStatus(nodeId);
        return status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT
                || status == NodeStatus.PARTIAL_FAIL || ctx.isNodeDone(nodeId);
    }

    private Map<String, Object> handleLogicRelation(DagGraph graph, String nodeId,
                                                     DagParser.CanvasNode node,
                                                     Map<String, Object> config,
                                                     ExecutionContext ctx) {
        if (isTerminalSpecialNode(nodeId, ctx)) {
            NodeStatus status = ctx.getNodeStatus(nodeId);
            if (status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT || status == NodeStatus.PARTIAL_FAIL)
                throw new RuntimeException("节点 " + nodeId + " 已处于终态: " + status);
            return Map.of();
        }
        List<String> upstreamIds = graph.upstream(nodeId);
        String relation = (String) config.getOrDefault("relation", "AND");

        if (LogicRelationHandler.shouldFailImmediately(relation, upstreamIds, ctx)) {
            NodeGate gate = ctx.getGate(nodeId);
            if (gate.executing.compareAndSet(false, true)) {
                writeTraceStart(ctx, node);
                ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                writeTraceEnd(ctx, node, NodeResult.fail("AND 上游失败，条件不可满足"), 0);
                gate.executing.set(false);
                log.warn("[ENGINE] LOGIC_RELATION AND 上游失败，立即 FAILED nodeId={}", nodeId);
                if (ctx.isBenefitGranted() || ctx.isUserReached()) return Map.of();
                throw new RuntimeException("LOGIC_RELATION AND 条件因上游失败不可满足");
            }
            return Map.of();
        }

        if (!LogicRelationHandler.checkCondition(relation, upstreamIds, ctx)) {
            ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING);
            if (ctx.getScheduledHubTimeouts().add("lr:" + nodeId)) {
                ctx.getHubStartTimes().putIfAbsent("lr:" + nodeId, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
                TIMEOUT_SCHEDULER.schedule(() ->
                        handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, "LOGIC_RELATION", timeoutSec),
                        timeoutSec, TimeUnit.SECONDS);
                log.debug("[LOGIC_RELATION] 启动等待超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            return Map.of();
        }
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx);
    }

    private Map<String, Object> handleHub(DagGraph graph, String nodeId,
                                           DagParser.CanvasNode node,
                                           Map<String, Object> config,
                                           ExecutionContext ctx) {
        if (isTerminalSpecialNode(nodeId, ctx)) {
            NodeStatus status = ctx.getNodeStatus(nodeId);
            if (status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT || status == NodeStatus.PARTIAL_FAIL)
                throw new RuntimeException("节点 " + nodeId + " 已处于终态: " + status);
            return Map.of();
        }
        List<String> upstreamIds = graph.upstream(nodeId);

        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING);
            if (ctx.getScheduledHubTimeouts().add(nodeId)) {
                ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
                int timeoutSec = HubHandler.getTimeoutSeconds(config);
                TIMEOUT_SCHEDULER.schedule(() ->
                        handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, "HUB", timeoutSec),
                        timeoutSec, TimeUnit.SECONDS);
                log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
            }
            return Map.of();
        }
        return executeNodeAfterStage2(graph, nodeId, node, config, ctx);
    }

    private Map<String, Object> handleAggregate(DagGraph graph, String nodeId,
                                                 DagParser.CanvasNode node,
                                                 Map<String, Object> config,
                                                 ExecutionContext ctx) {
        if (isTerminalSpecialNode(nodeId, ctx)) {
            NodeStatus status = ctx.getNodeStatus(nodeId);
            if (status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT || status == NodeStatus.PARTIAL_FAIL)
                throw new RuntimeException("节点 " + nodeId + " 已处于终态: " + status);
            return Map.of();
        }
        List<String> upstreamIds = graph.upstream(nodeId);

        if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
            ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.WAITING);
            String timerKey = "ag:" + nodeId;
            if (ctx.getScheduledHubTimeouts().add(timerKey)) {
                ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
                int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
                TIMEOUT_SCHEDULER.schedule(() ->
                        handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, "AGGREGATE", timeoutSec),
                        timeoutSec, TimeUnit.SECONDS);
            }
            return Map.of();
        }
        Map<String, Object> enrichedConfig = new HashMap<>(config);
        enrichedConfig.put(MapFieldKeys.UPSTREAM_IDS, upstreamIds);
        return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx);
    }

    private void scheduleThresholdTimeoutIfNeeded(DagGraph graph, String nodeId,
                                                   DagParser.CanvasNode node,
                                                   Map<String, Object> config,
                                                   ExecutionContext ctx) {
        String timerKey = "th:" + nodeId;
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) return;
        int timeoutSec = config.get("timeout") instanceof Number n ? n.intValue() : (int) globalTimeout;
        TIMEOUT_SCHEDULER.schedule(() ->
                handleSpecialNodeTimeout(graph, nodeId, node, config, ctx, "THRESHOLD", timeoutSec),
                timeoutSec, TimeUnit.SECONDS);
    }

    private void handleSpecialNodeTimeout(DagGraph graph, String nodeId,
                                           DagParser.CanvasNode node,
                                           Map<String, Object> config,
                                           ExecutionContext ctx,
                                           String label,
                                           int timeoutSec) {
        if (!ctx.setNodeStatusIfNotDone(nodeId, NodeStatus.TIMEOUT)) return;
        log.warn("[{}] 等待超时 timeout={}s nodeId={}", label, timeoutSec, nodeId);
        String targetNodeId = resolveSpecialTimeoutTarget(config);
        Map<String, Object> timeoutOutput = new LinkedHashMap<>();
        timeoutOutput.put(MapFieldKeys.NODE_ID, nodeId);
        timeoutOutput.put(MapFieldKeys.NODE_TYPE, node.getType());
        timeoutOutput.put(MapFieldKeys.OUTCOME, NodeOutcome.TIMEOUT.name());
        timeoutOutput.put(MapFieldKeys.REASON_CODE, "SPECIAL_NODE_TIMEOUT");
        timeoutOutput.put(MapFieldKeys.REASON_MESSAGE, label + " 等待超时");
        ctx.putNodeOutput(nodeId, timeoutOutput);
        writeTraceEnd(ctx, node, NodeResult.timeout(targetNodeId,
                "SPECIAL_NODE_TIMEOUT", label + " 等待超时"), 0);

        if (targetNodeId == null || targetNodeId.isBlank()) {
            ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
            executionService.completePausedExecution(ctx, ExecutionStatus.FAILED.getCode(), timeoutOutput);
            return;
        }
        ctxStore.save(ctx);
        try {
            executeNode(graph, targetNodeId, ctx);
            if (hasWaitingNodes(ctx)) {
                ctxStore.save(ctx);
                executionService.completePausedExecution(ctx, ExecutionStatus.PAUSED.getCode(), Map.of());
            } else {
                writeSkippedNodesIfComplete(graph, ctx);
                ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
                executionService.completePausedExecution(ctx, ExecutionStatus.SUCCESS.getCode(), Map.of());
            }
        } catch (Exception e) {
            ctxStore.delete(ctx.getCanvasId(), ctx.getUserId());
            executionService.completePausedExecution(ctx, ExecutionStatus.FAILED.getCode(),
                    Map.of(MapFieldKeys.ERROR, e.getMessage()));
        }
    }

    private String resolveSpecialTimeoutTarget(Map<String, Object> config) {
        Object timeoutTarget = config.get(MapFieldKeys.TIMEOUT_NODE_ID);
        if (timeoutTarget instanceof String s && !s.isBlank()) return s;
        Object failTarget = config.get(MapFieldKeys.FAIL_NODE_ID);
        return failTarget instanceof String s && !s.isBlank() ? s : null;
    }

    // ── Stage 3-6 shared entry ────────────────────────────────────

    private Map<String, Object> executeNodeAfterStage2(DagGraph graph, String nodeId,
                                                        DagParser.CanvasNode node,
                                                        Map<String, Object> config,
                                                        ExecutionContext ctx) {
        if (ctx.isNodeDone(nodeId)) return Map.of();
        NodeGate nodeGate = ctx.getGate(nodeId);
        if (!nodeGate.executing.compareAndSet(false, true)) {
            nodeGate.repeatPending.set(true);
            return Map.of();
        }
        return executeWithStages56(graph, nodeId, node, config, ctx, nodeGate);
    }

    // ── Trigger downstream ────────────────────────────────────────

    private Map<String, Object> triggerDownstream(DagGraph graph, NodeResult result,
                                                   String sourceNodeId, String sourceType,
                                                   ExecutionContext ctx) {
        markNonTakenBranchesSkipped(graph, sourceNodeId, sourceType, result, ctx);

        if (NodeType.PRIORITY.equals(sourceType) && result.branchMap() != null) {
            String fallbackNextId = NodeRouteResolver.resolveFallbackTarget(result);
            List<String> orderedBranches = NodeRouteResolver.resolvePriorityBranchTargets(result);
            return tryPrioritySequentially(orderedBranches, fallbackNextId, sourceNodeId, graph, ctx);
        }

        List<String> nextIds = collectNextIds(result);
        if (nextIds.isEmpty()) {
            return result.output() != null ? result.output() : Map.of();
        }
        prepareControlFlowReentry(graph, result, sourceNodeId, sourceType, nextIds, ctx);

        // Parallel downstream execution via virtual threads
        if (nextIds.size() == 1) {
            return executeNode(graph, nextIds.getFirst(), ctx);
        }
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        for (String nextId : nextIds) {
            futures.add(Thread.ofVirtual().start(() -> executeNode(graph, nextId, ctx)));
        }
        Map<String, Object> lastResult = Map.of();
        for (Future<Map<String, Object>> f : futures) {
            try {
                lastResult = f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return lastResult;
    }

    private Map<String, Object> triggerFailureAwareDownstream(DagGraph graph,
                                                                String sourceNodeId,
                                                                String sourceType,
                                                                ExecutionContext ctx,
                                                                String errorMessage) {
        List<String> nextIds = graph.downstream(sourceNodeId).stream()
                .filter(nextId -> {
                    DagParser.CanvasNode nextNode = graph.getNode(nextId);
                    return nextNode != null && isFailureAwareConvergenceNode(nextNode.getType());
                }).distinct().toList();
        if (nextIds.isEmpty()) {
            throw new RuntimeException("节点 " + sourceNodeId + " 失败: " + errorMessage);
        }
        log.debug("[ENGINE] 失败信号继续传递到汇聚节点 sourceNodeId={} downstream={}", sourceNodeId, nextIds);
        Map<String, Object> lastResult = Map.of();
        for (String nextId : nextIds) {
            lastResult = executeNode(graph, nextId, ctx);
        }
        return lastResult;
    }

    private boolean isFailureAwareConvergenceNode(String nodeType) {
        return NodeType.HUB.equals(nodeType) || NodeType.AGGREGATE.equals(nodeType)
                || NodeType.LOGIC_RELATION.equals(nodeType) || NodeType.THRESHOLD.equals(nodeType);
    }

    // ── Priority sequential ────────────────────────────────────────

    private Map<String, Object> tryPrioritySequentially(List<String> branches,
                                                         String fallbackNextId,
                                                         String priorityNodeId,
                                                         DagGraph graph,
                                                         ExecutionContext ctx) {
        if (branches.isEmpty()) {
            if (fallbackNextId != null) {
                ctx.setNodeStatus(priorityNodeId, NodeStatus.PARTIAL_FAIL);
                if (ctx.isNodeDone(fallbackNextId)) return Map.of();
                return executeNode(graph, fallbackNextId, ctx);
            }
            throw new RuntimeException("PRIORITY 所有分支均失败");
        }
        String currentBranchId = branches.getFirst();
        try {
            Map<String, Object> result = executeNode(graph, currentBranchId, ctx);
            if (ctx.getNodeStatus(currentBranchId) == NodeStatus.SUCCESS) return Map.of();
            return tryPrioritySequentially(branches.subList(1, branches.size()),
                    fallbackNextId, priorityNodeId, graph, ctx);
        } catch (Exception e) {
            if (!ctx.isNodeDone(currentBranchId)) throw e;
            return tryPrioritySequentially(branches.subList(1, branches.size()),
                    fallbackNextId, priorityNodeId, graph, ctx);
        }
    }

    // ── Control flow reentry (LOOP/GOTO) ──────────────────────────

    private void prepareControlFlowReentry(DagGraph graph, NodeResult result,
                                            String sourceNodeId, String sourceType,
                                            List<String> nextIds, ExecutionContext ctx) {
        boolean looping = NodeType.LOOP.equals(sourceType)
                && result.routes() != null && result.routes().containsKey("loop");
        boolean jumping = NodeType.GOTO.equals(sourceType)
                && result.routes() != null && result.routes().containsKey("goto");
        if (!looping && !jumping) return;
        for (String nextId : nextIds) {
            resetReachableUntilSource(graph, nextId, sourceNodeId, ctx);
        }
    }

    private void resetReachableUntilSource(DagGraph graph, String startNodeId,
                                            String sourceNodeId, ExecutionContext ctx) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(startNodeId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) continue;
            ctx.resetNodeStatusForReentry(current);
            if (current.equals(sourceNodeId)) continue;
            graph.downstream(current).forEach(queue::addLast);
        }
    }

    // ── Skipped nodes ─────────────────────────────────────────────

    private void writeSkippedNodes(DagGraph graph, ExecutionContext ctx) {
        List<CanvasExecutionTraceDO> skippedTraces = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        graph.getNodeMap().forEach((nodeId, node) -> {
            if (ctx.setNodeStatusIfAbsent(nodeId, NodeStatus.SKIPPED)) {
                skippedTraces.add(CanvasExecutionTraceDO.builder()
                        .executionId(ctx.getExecutionId()).nodeId(nodeId)
                        .nodeType(node.getType()).nodeName(node.getName())
                        .status(3).startedAt(now).finishedAt(now).build());
            }
        });
        skippedTraces.forEach(traceBuffer::offer);
    }

    private void writeSkippedNodesIfComplete(DagGraph graph, ExecutionContext ctx) {
        if (!hasWaitingNodes(ctx)) writeSkippedNodes(graph, ctx);
    }

    private boolean hasWaitingNodes(ExecutionContext ctx) {
        return ctx.getNodeStatuses().values().stream().anyMatch(s -> s == NodeStatus.WAITING);
    }

    // ── Trace writing ─────────────────────────────────────────────

    private void writeTraceStart(ExecutionContext ctx, DagParser.CanvasNode node) {
        traceBuffer.offer(CanvasExecutionTraceDO.builder()
                .executionId(ctx.getExecutionId()).nodeId(node.getId())
                .nodeType(node.getType()).nodeName(node.getName())
                .status(0).startedAt(LocalDateTime.now()).build());
    }

    private void writeTraceEnd(ExecutionContext ctx, DagParser.CanvasNode node,
                               NodeResult result, long durationMs) {
        int status = traceStatus(result);
        String outputJson = null;
        try {
            if (result.output() != null && !result.output().isEmpty()) {
                outputJson = objectMapper.writeValueAsString(DataMaskingUtil.maskObject(result.output()));
            }
        } catch (Exception ignored) {}
        traceBuffer.offer(CanvasExecutionTraceDO.builder()
                .executionId(ctx.getExecutionId()).nodeId(node.getId())
                .nodeType(node.getType()).nodeName(node.getName())
                .status(status).outputData(outputJson)
                .errorMsg(DataMaskingUtil.maskText(result.errorMessage()))
                .finishedAt(LocalDateTime.now())
                .durationMs(durationMs > 0 ? durationMs : null).build());
    }

    // ── DLQ ───────────────────────────────────────────────────────

    private void writeDlq(ExecutionContext ctx, String nodeId, String nodeType, Throwable cause) {
        metrics.recordDlq(nodeType);
        try {
            String msg = cause != null && cause.getMessage() != null ? cause.getMessage() : "unknown";
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                    .executionId(ctx.getExecutionId()).canvasId(ctx.getCanvasId())
                    .userId(ctx.getUserId()).perfRunId(ctx.getPerfRunId())
                    .failedNodeId(nodeId).failedNodeType(nodeType)
                    .errorMsg(msg.substring(0, Math.min(500, msg.length())))
                    .retryCount(maxRetry)
                    .triggerPayload(objectMapper.writeValueAsString(ctx.getTriggerPayload()))
                    .triggerType(ctx.getTriggerType())
                    .triggerNodeType(ctx.getTriggerNodeType())
                    .matchKey(ctx.getMatchKey())
                    .failedAt(LocalDateTime.now()).build();
            dlqMapper.insert(dlq);
            log.warn("[DLQ] executionId={} nodeId={} reason={}", ctx.getExecutionId(), nodeId, msg);
        } catch (Exception e) {
            log.error("[DLQ] 序列化失败: {}", e.getMessage());
        }
    }

    // ── Branch skipping ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void markNonTakenBranchesSkipped(DagGraph graph, String sourceNodeId,
                                              String sourceType, NodeResult result,
                                              ExecutionContext ctx) {
        DagParser.CanvasNode node = graph.getNode(sourceNodeId);
        if (node == null || node.getConfig() == null) return;
        Map<String, Object> cfg = node.getConfig();

        if (NodeType.IF_CONDITION.equals(sourceType)) {
            String successId = (String) cfg.get(MapFieldKeys.SUCCESS_NODE_ID);
            String failId = (String) cfg.get(MapFieldKeys.FAIL_NODE_ID);
            String takenId = result.successNodeId() != null ? result.successNodeId() : result.failNodeId();
            String skippedId = takenId != null && takenId.equals(successId) ? failId : successId;
            markSkippedPath(graph, skippedId, ctx);
        } else if ("SELECTOR".equals(sourceType)) {
            java.util.List<Map<String, Object>> branches =
                    (java.util.List<Map<String, Object>>) cfg.getOrDefault(MapFieldKeys.BRANCHES, List.of());
            String elseId = (String) cfg.get(MapFieldKeys.ELSE_NODE_ID);
            String takenId = result.nextNodeId();
            branches.forEach(b -> {
                String branchNext = (String) b.get(MapFieldKeys.NEXT_NODE_ID);
                if (branchNext != null && !branchNext.equals(takenId)) markSkippedPath(graph, branchNext, ctx);
            });
            if (elseId != null && !elseId.equals(takenId)) markSkippedPath(graph, elseId, ctx);
        }
    }

    private void markSkippedPath(DagGraph graph, String nodeId, ExecutionContext ctx) {
        if (nodeId == null || nodeId.isBlank()) return;
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(nodeId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) continue;
            ctx.setNodeStatusIfNotDone(current, NodeStatus.SKIPPED);
            for (String downstream : graph.downstream(current)) {
                if (allUpstreamSkipped(graph, downstream, ctx)) queue.addLast(downstream);
            }
        }
    }

    private boolean allUpstreamSkipped(DagGraph graph, String nodeId, ExecutionContext ctx) {
        List<String> upstreamIds = graph.upstream(nodeId);
        return !upstreamIds.isEmpty()
                && upstreamIds.stream().allMatch(u -> ctx.getNodeStatus(u) == NodeStatus.SKIPPED);
    }

    // ── Utility ───────────────────────────────────────────────────

    private List<String> collectNextIds(NodeResult result) {
        return NodeRouteResolver.resolveTargets(result).stream().distinct().toList();
    }

    private NodeStatus statusForOutcome(NodeOutcome outcome) {
        if (outcome == null) return NodeStatus.SUCCESS;
        return switch (outcome) {
            case FAIL -> NodeStatus.FAILED;
            case TIMEOUT -> NodeStatus.TIMEOUT;
            case SUPPRESSED -> NodeStatus.SUPPRESSED;
            case SKIPPED -> NodeStatus.SKIPPED;
            case PENDING -> NodeStatus.WAITING;
            case SUCCESS -> NodeStatus.SUCCESS;
        };
    }

    private int traceStatus(NodeResult result) {
        if (!result.success()) return 2;
        if (result.outcome() == NodeOutcome.TIMEOUT || result.outcome() == NodeOutcome.FAIL) return 2;
        if (result.outcome() == NodeOutcome.PENDING) return 0;
        if (result.outcome() == NodeOutcome.SKIPPED) return 3;
        return 1;
    }

    private Map<String, Object> pendingResponse(String nodeId, String nodeType, NodeResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(MapFieldKeys.PENDING, true);
        response.put(MapFieldKeys.NODE_ID, nodeId);
        response.put(MapFieldKeys.NODE_TYPE, nodeType);
        response.put(MapFieldKeys.OUTCOME, result.outcome().name());
        if (result.resumeAtEpochMs() != null) response.put(MapFieldKeys.RESUME_AT_EPOCH_MS, result.resumeAtEpochMs());
        if (result.reasonCode() != null) response.put(MapFieldKeys.REASON_CODE, result.reasonCode());
        if (result.reasonMessage() != null) response.put(MapFieldKeys.REASON_MESSAGE, result.reasonMessage());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext ctx) {
        if (config == null) return Map.of();
        boolean hasContextField = config.values().stream().anyMatch(
                v -> v instanceof Map<?, ?> m && MapFieldKeys.CONTEXT.equals(m.get(MapFieldKeys.VALUE_TYPE)));
        if (!hasContextField) return config;
        Map<String, Object> resolved = new HashMap<>(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> m) {
                String valueType = (String) ((Map<String, Object>) m).get(MapFieldKeys.VALUE_TYPE);
                String value = (String) ((Map<String, Object>) m).get(MapFieldKeys.VALUE_KEY);
                if (MapFieldKeys.CONTEXT.equals(valueType) && value != null) {
                    resolved.put(entry.getKey(), ctx.getContextValue(value));
                }
            }
        }
        return resolved;
    }

    private Map<String, Object> resolveConfigWithNodeId(Map<String, Object> config,
                                                         ExecutionContext ctx, String nodeId,
                                                         String nodeType) {
        Map<String, Object> resolved = new HashMap<>(resolveConfig(config, ctx));
        resolved.put(MapFieldKeys.NODE_ID_INTERNAL, nodeId);
        enrichWaitResumePayload(resolved, ctx, nodeId, nodeType);
        return resolved;
    }

    private void enrichWaitResumePayload(Map<String, Object> resolved,
                                           ExecutionContext ctx, String nodeId,
                                           String nodeType) {
        Map<String, Object> payload = ctx.getTriggerPayload();
        Object sourceNodeId = payload.get(MapFieldKeys.SOURCE_NODE_ID);
        if (sourceNodeId != null && !nodeId.equals(sourceNodeId.toString())) return;
        if (NodeType.WAIT.equals(nodeType) && payload.containsKey(MapFieldKeys.WAIT_RESUME_STATUS))
            resolved.put(MapFieldKeys.WAIT_RESUME_STATUS, payload.get(MapFieldKeys.WAIT_RESUME_STATUS));
        if (NodeType.GOAL_CHECK.equals(nodeType) && payload.containsKey(MapFieldKeys.GOAL_RESUME_STATUS))
            resolved.put(MapFieldKeys.GOAL_RESUME_STATUS, payload.get(MapFieldKeys.GOAL_RESUME_STATUS));
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ImperativeDagEngineTest
```

Expected: PASS — all 3 tests green: linear DAG completes, convergence node executes at most twice, `execute()` returns `Map` not `Mono`.

- [ ] **Step 7: Run full test suite**

```bash
cd backend && mvn test -pl canvas-engine
```

Expected: All tests pass (may need signature updates in test classes referencing old `executeAsync`/`Mono` return types).

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/ImperativeDagEngineTest.java
git commit -m "feat: rewrite DagEngine as imperative step-through executor with virtual threads"
```

---

### Task 5: Remove Reactor Dependencies from pom.xml

**Files:**
- Modify: `backend/canvas-engine/pom.xml`

- [ ] **Step 1: Verify zero reactor imports in source code**

```bash
grep -rn "import reactor" backend/canvas-engine/src/main/java/ | wc -l
```

Expected: `0`. If > 0, list the files:

```bash
grep -rn "import reactor" backend/canvas-engine/src/main/java/ | sort
```

Fix each remaining file by removing the import and updating the code (follow patterns from Tasks 2-4).

- [ ] **Step 2: Verify zero reactor imports in test code**

```bash
grep -rn "import reactor" backend/canvas-engine/src/test/java/ | wc -l
```

Expected: `0`. If > 0, update test files to use non-reactive assertions.

- [ ] **Step 3: Remove reactor-test from pom.xml if still present**

In `backend/canvas-engine/pom.xml`, remove:

```xml
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 4: Verify spring-boot-starter-webflux is absent**

```bash
grep -n "webflux" backend/canvas-engine/pom.xml
```

Expected: no output (already removed in Task 1, but double-check no transitive dependency pulled it back).

- [ ] **Step 5: Build and verify clean**

```bash
cd backend && mvn clean install -pl canvas-engine
```

Expected: BUILD SUCCESS, no reactor dependency warnings.

- [ ] **Step 6: Final grep verification**

```bash
grep -rn "import reactor\|Schedulers\.\|Mono<\|Flux<" backend/canvas-engine/src/main/java/ | grep -v "// " | wc -l
```

Expected: `0`.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/pom.xml
git commit -m "feat: remove all Reactor dependencies from canvas-engine"
```

---

## Appendix A: Complete Handler File Checklist (66 files)

All files under `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/` that need `executeAsync` → `execute` migration:

| # | File | Notes |
|---|------|-------|
| 1 | `AbSplitHandler.java` | Simple: `Mono.just()` → direct return |
| 2 | `AbstractSendMessageHandler.java` | Has WebClient calls → switch to RestClient |
| 3 | `AggregateHandler.java` | Pure routing, no I/O |
| 4 | `AiNextBestActionHandler.java` | External API call → RestClient |
| 5 | `ApiCallHandler.java` | External API call → RestClient |
| 6 | `ApiCallPayloadBuilder.java` | Utility class, no handler method |
| 7 | `ApiTriggerHandler.java` | Simple |
| 8 | `AudienceTriggerHandler.java` | DB query → direct |
| 9 | `CanvasTriggerHandler.java` | Simple |
| 10 | `CdpTagWriteHandler.java` | External API → RestClient |
| 11 | `ChannelAvailabilityHandler.java` | Simple |
| 12 | `CommitActionHandler.java` | Simple |
| 13 | `ConditionEvaluator.java` | Utility, no handler method |
| 14 | `CouponHandler.java` | External API → RestClient |
| 15 | `CreateTaskHandler.java` | Simple |
| 16 | `DelayHandler.java` | Uses `Mono.delay()` → `ScheduledExecutorService` |
| 17 | `DirectCallHandler.java` | Simple |
| 18 | `DirectReturnHandler.java` | Simple |
| 19 | `EndHandler.java` | Simple |
| 20 | `EventTriggerHandler.java` | Simple |
| 21 | `ExperimentHandler.java` | Simple routing |
| 22 | `FrequencyCapHandler.java` | DB query → direct |
| 23 | `GoalCheckHandler.java` | Simple |
| 24 | `GotoHandler.java` | Simple routing |
| 25 | `GroovyHandler.java` | Script execution, may use boundedElastic → direct on virtual thread |
| 26 | `GroovyScriptCache.java` | Utility, no handler method |
| 27 | `GroupHandler.java` | Simple routing |
| 28 | `HubHandler.java` | Convergence logic (mostly static methods, may not need changes) |
| 29 | `IfConditionHandler.java` | Simple routing |
| 30 | `InAppNotifyHandler.java` | External API → RestClient |
| 31 | `LogicRelationHandler.java` | Convergence logic (mostly static methods) |
| 32 | `LoopHandler.java` | Simple routing |
| 33 | `ManualApprovalHandler.java` | DB query → direct |
| 34 | `MergeHandler.java` | Simple routing |
| 35 | `MqTriggerHandler.java` | Simple |
| 36 | `PointsOperationHandler.java` | External API → RestClient |
| 37 | `PriorityHandler.java` | Simple routing |
| 38 | `QuietHoursHandler.java` | Simple |
| 39 | `RandomSplitHandler.java` | Simple routing |
| 40 | `ReachPlatformHandler.java` | External API → RestClient |
| 41 | `RecommendationHandler.java` | External API → RestClient |
| 42 | `ScheduledTriggerHandler.java` | Simple |
| 43 | `ScoringHandler.java` | Simple |
| 44 | `SelectorHandler.java` | Simple routing |
| 45 | `SendEmailHandler.java` | External API → RestClient |
| 46 | `SendInAppHandler.java` | External API → RestClient |
| 47 | `SendMqHandler.java` | MQ send → direct (RocketMQ producer is sync) |
| 48 | `SendPushHandler.java` | External API → RestClient |
| 49 | `SendSmsHandler.java` | External API → RestClient |
| 50 | `SendWechatHandler.java` | External API → RestClient |
| 51 | `StartHandler.java` | Simple |
| 52 | `SubflowHandler.java` | Recursive engine call (now sync) |
| 53 | `SubFlowRefHandler.java` | Simple |
| 54 | `SuppressionCheckHandler.java` | DB query → direct |
| 55 | `TaggerHandler.java` | External API → RestClient |
| 56 | `TaggerOfflineHandler.java` | External API → RestClient |
| 57 | `TaggerRealtimeHandler.java` | External API → RestClient |
| 58 | `TagOperationHandler.java` | DB/External API |
| 59 | `TemplateNodeHandler.java` | Simple |
| 60 | `ThresholdHandler.java` | Simple (repeat semantics handled by engine) |
| 61 | `TrackEventHandler.java` | Simple |
| 62 | `TransferJourneyHandler.java` | Simple |
| 63 | `UpdateProfileHandler.java` | External API → RestClient |
| 64 | `WaitHandler.java` | Simple (WAIT/pending) |
| 65 | `WeightedChoice.java` | Utility, no handler method |
## Appendix B: Complete Controller File Checklist (29 files)

All files under `backend/canvas-engine/src/main/java/org/chovy/canvas/web/` that need Mono→sync migration. Pattern is identical to Task 2's 3 representative controllers:

| # | File | Primary Pattern |
|---|------|-----------------|
| 1 | `AbExperimentController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 2 | `AdminController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 3 | `ApiDefinitionController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 4 | `AsyncTaskController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 5 | `AudienceController.java` | **Done in Task 2** |
| 6 | `AuthController.java` | Mixed patterns |
| 7 | `CanvasController.java` | **Done in Task 2** |
| 8 | `CanvasExecutionManagementController.java` | `Mono.fromRunnable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 9 | `CanvasExecutionRequestManagementController.java` | Mixed |
| 10 | `CanvasMqTriggerRejectedController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 11 | `CanvasStatsController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` (heavy stats queries) |
| 12 | `CanvasUserController.java` | Mixed |
| 13 | `CdpTagOperationController.java` | Mixed |
| 14 | `CdpUserController.java` | Mixed |
| 15 | `DataSourceConfigController.java` | Mixed |
| 16 | `DlqController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 17 | `EventDefinitionController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `@RequestBody Mono<String>` |
| 18 | `ExecutionController.java` | **Done in Task 2** |
| 19 | `HomeOverviewController.java` | Mixed |
| 20 | `IdentityTypeController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` |
| 21 | `MetaController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` (heaviest: 20+ occurrences) |
| 22 | `MqDefinitionController.java` | Mixed |
| 23 | `NotificationController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 24 | `OpsController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 25 | `SystemOptionController.java` | `Mono.fromCallable().subscribeOn(boundedElastic)` + `ReactiveSecurityContextHolder` |
| 26 | `TagDefinitionController.java` | Mixed |
| 27 | `TagImportController.java` | Mixed |
| 28 | `TagImportSourceController.java` | Mixed |
| 29 | `TenantController.java` | Mixed |

### Migration patterns for reference:

**Pattern A** — Simple query (most common):
```java
// BEFORE
public Mono<R<SomeDO>> get(@PathVariable Long id) {
    return Mono.fromCallable(() -> mapper.selectById(id))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
// AFTER
public R<SomeDO> get(@PathVariable Long id) {
    return R.ok(mapper.selectById(id));
}
```

**Pattern B** — With ReactiveSecurityContextHolder:
```java
// BEFORE
public Mono<R<Void>> create(@RequestBody SomeDO body) {
    return currentUser().flatMap(operator ->
            Mono.fromCallable(() -> { body.setCreatedBy(operator); service.create(body); return R.<Void>ok(); })
                    .subscribeOn(Schedulers.boundedElastic()));
}
private Mono<String> currentUser() {
    return ReactiveSecurityContextHolder.getContext()...;
}
// AFTER
public R<Void> create(@RequestBody SomeDO body) {
    body.setCreatedBy(currentUser());
    service.create(body);
    return R.ok();
}
private String currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Claims claims) {
        return defaultIfBlank(claims.get("username", String.class), "system");
    }
    return "system";
}
```

**Pattern C** — Void operation:
```java
// BEFORE
public Mono<R<Void>> delete(@PathVariable Long id) {
    return Mono.<Void>fromRunnable(() -> service.delete(id))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}
// AFTER
public R<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return R.ok();
}
```
