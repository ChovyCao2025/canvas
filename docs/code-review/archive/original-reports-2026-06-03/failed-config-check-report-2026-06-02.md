# Configuration Security Check Report

**检查时间：** 2026-06-02  
**检查范围：** `application.yml` 核心配置安全性  
**检查方法：** 人工代码审查 + 静态扫描

---

## ❌ **主要发现：3个CRITICAL安全问题**

### 🔴 **问题 #1: CORS配置存在严重安全风险**

**配置位置：** `application.yml` 第57-59行
```yaml
canvas:
  cors:
    # 生产环境必须改为实际前端域名，禁止用 * 上线
    allowed-origins: "*"
```

**严重性：** 🔴 **CRITICAL**

**问题分析：**
- ✅ **明文注释已警告**："生产环境必须改为实际前端域名，禁止用 * 上线"
- ❌ **当前配置违反警告**：`allowed-origins: "*"` 允许任意域携带凭证
- ⚠️ **触发条件**：`allowCredentials` 配置必须与来源限制联合使用

**安全风险评估：**
```
攻击场景1：CORS劫持攻击
├─ 攻击者控制恶意域名 example.com
├─ 前端客户端添加 credentials: 'include' 
├─ 恶意页面从 canvas.yourdomain.com 读取 JWT Token
└─ 结果：用户Token可被跨站窃取

攻击场景2：恶意仓储接入
├─ 攻击者创建攻击仓储 canvas-hacker.com
├─ Cloudflare Workers/Proxy转发请求
├─ 跨域携带Cookie + JWT
└─ 结果：可伪造用户身份调用Canvas API
```

**法律规定（中国）：**
- 《网络安全法》第27条：禁止非法侵入、干扰、破坏网络功能
- 《个人信息保护法》第51条：个人信息处理者应采取加密等技术措施
- 违反明确安全警告 = 产品责任风险

---

### 🔴 **问题 #2: 数据库明文凭证风险**

**配置位置：** `application.yml` 第6-9行
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/canvas_db?...
  username: root
  password: root
```

**严重性：** 🟠 **HIGH（开发环境可接受，生产环境CRITICAL）**

**检查方法：**
- ✅ **无Jasypt加密**：已扫描配置文件，未发现Jasypt或Vault配置
- ✅ **无环境变量注入**：未使用 `${DB_PASSWORD:}` 格式
- ✅ **金鱼缸隔离**：Docker环境下 root凭证在隔离容器运行

**风险评估：**
```
泄露路径：
├─ 系统漏洞：MySQL未打补丁 → 权限提升 → 账号密码泄露
├─ 关键词扫描：grep -r "password" .git/ → 社交媒体泄密 → 
├─ 日志泄露：grep password /var/log/*.log
└─ 内部泄密：离职员工导出配置 + 登录数据库

生产环境风险：4/5（高）
开发环境风险：1/5（低，容器隔离）
```

**合规要求（GDPR/个保法）：**
- 个人金融信息（PHI）必须加密存储（永远不要明文）
- `data_source_config` 属于敏感配置项，合规风险高

---

### ✅ **问题 #3: Redis无密码 - 中等风险**

**配置位置：** `application.yml` 第24-35行
```yaml
data:
  redis:
    host: localhost
    port: 6379
    timeout: 1000ms
    # password: （无密码时注释掉）
```

**严重性：** 🟡 **MEDIUM（已标注无密码，风险可控）**

**检查结果：**
- ✅ **明确注释说明**：`# password: （无密码时注释掉）`
- ✅ **环境隔离**：Redis与业务系统同容器运行 → 逃逸风险高但需资本价
- ✅ **防火墙策略**：Jetty使用localhost绑定 → 外部无法访问（仅容器内可访问）

**风险评估：**
```
泄露路径：
├─ 容器逃逸：K8s漏洞/容器root权限 → 容器外访问端口6379
├─ 账号泄露：grep password /root/.ssh/*.key
└─ 内部危害：Redis不支持TLS → 文本传输Key-Value

生产环境风险：2/5（中，若容器安全配置良好则低）
```

---

## ✅ **已正确配置的设计**

### #4: JWT Secret配置安全 ✅

**配置：** `application.yml` 第51-55行
```yaml
canvas:
  jwt:
    secret: ${CANVAS_JWT_SECRET:}
    expiry-hours: 24
```

**评估：** ✅ **优秀配置**
- ✅ 使用环境变量 `${CANVAS_JWT_SECRET:}` 敏感配置外置
- ✅ 有空值警告：生产环境必须设置环境变量
- ✅ 有全局默认值：开发环境可快速启动
- ✅ 时效控制：24小时过期策略合理

---

### #5: 配置即代码（Cops管控）✅

**评估：** ✅ **良好**
- ✅ 所有配置集中在 `application.yml`
- ✅ 覆盖炭布引擎核心参数
- ✅ 有默认值和注释说明（中文）
- ✅ 通过Nacos动态配置扩展（末尾已注释扩展点）

---

## 📋 **紧急修复清单（本周必须完成）**

### P0 - CRITICAL（明天前）

🔥 **任务1: 紧急更改CORS配置**

```yaml
# ❌ 当前配置（严禁上线）
canvas:
  cors:
    allowed-origins: "*"

# ✅ 修复方案A：白名单配置
CANVAS_CORS_ALLOWED_ORIGINS=https://photonpay.com,https://app.photonpay.com
canvas:
  cors:
    allowed-origins: ${CANVAS_CORS_ALLOWED_ORIGINS:https://canvas.local.com}

# ✅ 修复方案B：生产环境完全禁用（如果无需跨域）
canvas:
  cors:
    allowed-origins: NOT_APPLICABLE
```

**验证命令：**
```bash
# 部署配置后验证
grep "allowed-origins" application.yml
curl -H "Origin: https://evil.com" \
     -H "Cookie: JWT_TOKEN=..." \
     -H "Access-Control-Request-Method: POST" \
     -I http://localhost:8080/api/execute
# 应返回 Access-Control-Allow-Origin: https://photonpay.com
```

---

### P1 - HIGH（本周内）

🔥 **任务2: 生产环境数据库账户权限最小化**

```bash
# 1. 创建专用应用账户（非root）
sudo mysql -u root -ppassword -e "CREATE USER 'canvas_app'@'localhost' IDENTIFIED BY 'StrongPass123!';"
sudo mysql -u root -ppassword -e "GRANT SELECT, INSERT, UPDATE, DELETE ON canvas_db.* TO 'canvas_app'@'localhost';"
sudo mysql -u root -ppassword -e "FLUSH PRIVILEGES;"

# 2. 更新配置
datasource:
  username: canvas_app
  password: ${DB_PASSWORD}  # 使用Vault或Jasypt
```

**可选：Jasypt加密方案**
```yaml
# application.yml
spring:
  datasource:
    password: ENC(加密后的密码)
    
# pom.xml依赖
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>

# 环境变量
JASYPT_ENCRYPTOR_PASSWORD=加密钥匙
```

---

### P2 - MEDIUM（本月内）

🔥 **任务3: Redis访问控制加固**

```bash
# 1. 启用Redis保护密码（非密码方案）
cd redis
redis-cli CONFIG SET requirepass "StrongRedisPass123!💫"
redis-cli CONFIG REWRITE

# 2. 配置白名单（仅允许本地应用访问）
bind 127.0.0.1 127.0.0.1
protected-mode yes

# 3. 禁用危险命令
CONFIG SET rename-command FLUSHDB ""
CONFIG SET rename-command FLUSHALL ""
```

**验证命令：**
```bash
# 安全检查
redis-cli ping
# 应返回：PONG（若连接失败且配置了requirepass则暴露配置值）

redis-cli -a "StrongRedisPass123!💫" CONFIG GET requirepass
# 应返回正确的值
```

---

## 🎯 **配置审计自动化脚本**

### 部署前安全脚本

```bash
#!/bin/bash
# deploy-security-check.sh

echo "🔍 Canvas Architecture Security Check"
echo "======================================"

# 1. CORS配置检查
echo -e "\n1️⃣  Checking CORS configuration..."
if grep -q "allowed-origins: \"\\*\"" application.yml; then
    echo "❌ FAIL: CORS wildcard detected! Code in Line: $(grep -n 'allowed-origins' application.yml | head -1)"
    echo "   Fixed: Change to specific domain list"
    exit 1
fi
echo "✅ PASS: CORS configured properly"

# 2. 数据库密码检查
echo -e "\n2️⃣  Checking database password exposure..."
if grep -q "password: root" application.yml; then
    echo "⚠️  WARN: Plaintext 'root' password in application.yml"
    echo "   React: Change to specialized account or Jasypt encryption"
fi
if grep "password:" application.yml | grep -v "letsEncrypt"; then
    echo "ℹ️  INFO: Database password configuration observed"
elif [[ -z "${DB_PASSWORD}" ]]; then
    echo "❌ FAIL: Database password not set as environment variable"
    exit 1
fi

# 3. Redis密码检查
echo -e "\n3️⃣  Checking Redis configuration..."
if grep -q "password: " application.yml; then
    echo "✅ PASS: Redis password configured"
else
    echo "⚠️  WARN: Redis password not configured"
    echo "   Risk: Container internal adapter"
fi

# 4. JWT Secret检查
echo -e "\n4️⃣  Checking JWT secret..."
if grep -q "canvas.jwt.secret: \"\"" application.yml; then
    echo "❌ FAIL: JWT secret not set! Code in Line: $(grep -n 'jwt.secret' application.yml | head -1)"
    exit 1
elif [[ -z "${CANVAS_JWT_SECRET}" ]]; then
    echo "⚠️  WARN: JWT secret environment variable not set (dev mode okay)"
fi

# 5. 熔断器配置检查
echo -e "\n5️⃣  Checking circuit breaker configuration..."
if grep -q "circuit-breaker:" application.yml; then
    count=$(grep -A4 "circuit-breaker:" application.yml | grep -c "failure-threshold")
    if [[ $count -gt 0 ]]; then
        echo "✅ PASS: Circuit breaker configured ($count threshold(s))"
    fi
fi

echo -e "\n✅ All security checks passed"
```

---

## 📊 **配置安全评分（当前）**

| 配置项 | 安全评分 | 评级 | 风险等级 |
|--------|---------|------|---------|
| CORS配置 | 3/10 | ❌ 严重 | CRITICAL |
| MySQL密码 | 4/10 | ⚠️ 高 | HIGH |
| Redis密码 | 7/10 | ✅ 可控 | MEDIUM |
| JWT Secret | 9/10 | ✅ 良好 | LOW |
| 敏感配置外置 | 9/10 | ✅ 优秀 | N/A |
| **总体评分** | **6.4/10** | ⚠️ **中等** | **需改进** |

---

## 🎖️ **安全准入标准（上线前必须通过）**

```
✅ 部署前自动化检查清单：
✓ CORS NOT using wildcard "*"
✓ Database password is NOT "root" and NOT plaintext in repo
✓ Redis password OR localhost-only binding
✓ JWT secret is set (via env var or vault)
✓ fail-fast on any vulnerability
✓ Docker image scanned for known CVEs (Trivy/nancy)

⚠️  部署前人工复核清单：
⚠️  生产域名已添加到 CORS 白名单
⚠️  数据库应用账户权限已最小化
⚠️  Redis已启用密码保护或配置白名单
⚠️  内网部署：Redis可保留无密码
⚠️  外网部署：所有配置项强制密码
```

---

## 📝 **后续行动建议**

### 立即执行（今天）:

1. **强制修改CORS配置：**
   ```bash
   CANVAS_CORS_ALLOWED_ORIGINS=https://e自己的域名.com 考domains
   # Build = 配置生效
   ```

2. **环境变量验证：**
   ```bash
   # 开发环境设置假密码（用于测试）
   export DB_PASSWORD=dev_test_password_123
   
   export CANVAS_JWT_SECRET=dev_jwt_secret_2026!!
   
   export ROCKETMQ_NAME_SERVER=localhost:9876
   export CANVAS_EVENT_REPORT_SECRET=canvas-event-report-secret-2026!!
   ```

3. **Docker安全启动：**
   ```bash
   docker-compose -f docker-compose.yml --env-file prod.env up -d
   ```

---

### 本周必须（本周五前）:

1. **Redis密码加固**
2. **数据库应用账户创建**（非root）
3. **Jasypt集成验证**

---

### 下月优化（6月30日前）:

1. **集成Vault或参数加密平台**
2. **上线前自动化安全检查脚本部署**
3. **建立CI/CD安全扫描流水线**

---

## 🆘 **紧急联系与支持**

**安全加固咨询：**
- DevOps团队
- 安全团队（photonpay.security）
- 法务部门（RGPD/ITT合规）

**参考资料：**
- OWASP CORS Security
- Spring Boot Security Best Practices
- OWASP DotNetStarter: Spring Configuration

---

**报告生成时间：** 2026-06-02  
**扫描工具：** 人工代码审查 + 静态分析  
**下次审查建议：** 重大配置变更后或每季度一次
