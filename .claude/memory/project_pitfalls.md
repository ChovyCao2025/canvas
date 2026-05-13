---
name: canvas-project-pitfalls
description: canvas 项目开发过程中遇到的坑和解决方案，防止重复踩
type: project
originSessionId: 08bf9a78-17aa-4baa-a698-1c4e990c1fce
---
## 1. Vite 代理与 SPA 路由冲突

**问题**：`/canvas` 同时是 React 前端路由和后端 API 前缀。刷新 `localhost:3000/canvas` 时，Vite 把浏览器的页面导航请求代理到后端，后端返回 JSON，浏览器直接显示 `{"code":-1,...}`，看起来像"token 失效"。

**Why**：Vite proxy 对所有匹配路径的请求一律转发，不区分页面导航和 API 请求。

**How to apply**：凡是前端路由与后端 API 路径前缀相同的，必须加 `bypass`：
```typescript
'/canvas': {
  target: 'http://localhost:8080',
  changeOrigin: true,
  bypass(req) {
    if (req.headers.accept?.includes('text/html')) return '/index.html'
  },
}
```

---

## 2. BCrypt 哈希写死在 Migration 文件里需验证

**问题**：`V3__auth_and_supplements.sql` 里 admin 的 BCrypt 哈希不匹配 `Admin@123`，导致登录一直报"用户名或密码错误"。

**Why**：哈希是手写进 SQL 的，没有经过实际加密验证。

**How to apply**：Migration 文件中的密码哈希，必须用 Spring 的 `BCryptPasswordEncoder` 实际生成后再填入，不能手写猜测。验证命令：
```bash
JAVA=/path/to/java21/bin/java
SPRING_JAR=~/.m2/repository/org/springframework/security/spring-security-crypto/*.jar
COMMONS_JAR=~/.m2/repository/commons-logging/commons-logging/*.jar
$JAVA -cp "$SPRING_JAR:$COMMONS_JAR" -e "new BCryptPasswordEncoder().matches('Admin@123', hash)"
```

---

## 3. @TableField(select = false) 导致登录密码验证失败

**问题**：`SysUser.password` 加了 `@TableField(select = false)`，MyBatis-Plus 普通查询不 SELECT 该字段，导致 `checkPassword()` 拿到 `null`，BCrypt 比对必然失败。

**Why**：`select = false` 是为了防止接口返回密码，但也阻止了登录验证。

**How to apply**：需要密码的场景（登录验证）必须用单独的方法显式 SELECT password：
```java
sysUserMapper.selectOne(
  new QueryWrapper<SysUser>()
    .select("id","username","password","role","enabled")
    .eq("username", username));
```

---

## 4. 修改已执行的 Flyway Migration 文件导致启动失败

**问题**：修改了已 apply 的 V3 migration 文件（改了 BCrypt 哈希），Flyway 检测到 checksum 不一致，应用启动失败。

**Why**：Flyway 对已执行的 migration 做 checksum 校验，任何修改都会触发 `FlywayValidateException`。

**How to apply**：
- **开发环境**：直接更新 DB 中的 checksum：`UPDATE flyway_schema_history SET checksum=新值 WHERE version='3'`
- **生产环境/正确做法**：新建 V8/V9 migration 来修正数据，永远不修改旧文件

---

## 5. Maven 编译报 --release 无效（Java 版本不匹配）

**问题**：`mvn` 命令默认使用系统 Java 8，但项目需要 Java 21，报 `无效的标记: --release`。

**How to apply**：始终用完整命令指定 Java 21：
```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  /opt/homebrew/bin/mvn spring-boot:run -f /Users/photonpay/project/canvas/backend/canvas-engine/pom.xml
```

---

## 6. 端口占用导致应用启动失败

**问题**：上一次 `spring-boot:run` 没有正常退出，8080 端口仍被占用，重启时报 `Port 8080 was already in use`。

**How to apply**：重启前先清理端口：
```bash
lsof -ti:8080 | xargs kill -9
```

---

## 7. Spring Security 触发浏览器原生 Basic Auth 弹窗

**问题**：未登录时 Spring Security 默认返回带 `WWW-Authenticate: Basic` 的 401，浏览器弹出原生登录框。

**How to apply**：必须配置自定义 `authenticationEntryPoint`，返回 JSON 401 而非触发 Basic Auth：
```java
.exceptionHandling(ex -> ex.authenticationEntryPoint((exchange, e) -> {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    var body = "{\"code\":-1,\"message\":\"未登录或 Token 已过期\",\"data\":null}";
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
}))
```
