# PRD-P2-05-DataSource密码加密存储

> 本文档为营销画布平台数据库凭证明文存储加密需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-05 |
| **需求名称** | DataSource密码加密存储 |
| **优先级** | P2 |
| **所属类别** | 安全合规 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

`application.yml` 中数据库密码明文存储：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/canvas_db
    username: root
    password: 12345678  # 明文密码
```

### 1.2 痛点

1. **文件泄露风险**：应用部署时 `.gitignore` 未忽略 `application.yml`，密码暴露在 Git 仓库
2. **容器泄露风险**：Docker 容器日志或 ConfigMap 挂载时密码泄露
3. **默认凭证风险**：使用 `root/12345678` 等弱密码，易被暴力破解
4. **第三方系统依赖**：RocketMQ、Redis、外部 API 密钥明文存储在配置文件中

### 1.3 竞品对标

| 竞品 | 密码存储方式 |
|------|-------------|
| Braze | 使用 Vault/KMS 加密，配置文件只存储加密后的密文 |
| Iterable | 使用 AWS Secrets Manager 或 Kubernetes Secrets |

---

## 2. 目标与价值

### 2.1 用户故事

- **运维人员**：作为系统管理员，我希望数据库密码在配置文件中是加密密文，只有通过密钥管理工具才能解密使用。

### 2.2 成功指标

- `application.yml` 中所有敏感配置项均加密（密码、证书、Key）
- Service 启动时通过 `spring.config.import` 引入加密后的配置
- 禁止 Development 环境明文密码，CI/CD 流水线检测到明文直接失败

### 2.3 不做会怎样

- Source Code 泄露导致数据库完全被攻击
- 内部人员访问配置文件时可直接看到数据库密码

---

## 3. 功能需求

### 3.1 核心功能

1. **Jasypt 加密集成**：使用 Jasypt 在启动时解密敏感配置
2. **配置文件加密**：`.env` 或 `application-prod.yml` 存储加密后的密文
3. **加密密钥管理**：通过环境变量 `JASYPT_ENCRYPTOR_PASSWORD_KEY` 传入主密钥
4. **CI/CD 密钥注入**：Jenkins/GitHub Actions 中注入 `JASYPT_ENCRYPTOR_PASSWORD_KEY`

#### 3.1.1 加密配置示例

**原始配置**：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/canvas_db
    username: root
    password: 12345678  # 明文
```

**加密后的配置**：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/canvas_db
    username: root
    password: ENC(1kF9z3mN2pL8qQ7vR5tU4sW3xY2zA1bC0dE9fG7h)

rocketmq:
  name-server: localhost:9876
  access-key: ENC(3jH4kL6mN8qO1pR2sT5uV7wX9yZ1aB2cC3dE4fG5hI)
  secret-key: ENC(4kL9mN2pR4sT6vW8xY0zA2bC4dE6fG8hJ0kL2mN4pR6sT8wX)

logging:
  file:
    password: ENC(5mP1qR3sT5vW7xY9zA1bC3dE5fG7hI9jK1lM3nO5qR7sT9wX1yZ3)
```

#### 3.1.2 加解密命令

```bash
# 安装 Jasypt CLI
mvn com.github.ulisesbocchio:jasypt-maven-plugin:3.0.5:encrypt-value \
  -Djasypt.encryptor.password="master-key"

# 加密密码
echo -n "12345678" | java -jar jasypt-cli.jar dempot secret-key to ENC(1kF9z3mN2pL8qQ7vR5tU4sW3xY2zA1bC0dE9fG7h)

# CI/CD 注入密钥
export JASYPT_ENCRYPTOR_PASSWORD_KEY="prod-secret-key-2026"
```

### 3.2 详细描述

**加密封装流程**：

1. 开发者提交代码时 `git diff` 检查是否包含明文密码
2. 使用 `jasypt-maven-plugin` 加密配置项
3. `.gitignore` 添加 `**/*-prod.yml`, `**/application-prod.yml.backup`
4. CI/CD 流水线通过 Secrets 注入 `JASYPT_ENCRYPTOR_PASSWORD_KEY`
5. `JasyptPropertiesConfig` 读取密钥，解密后注入 Spring Environment

**密钥安全策略**：

| 密钥类型 | 生成方式 | 存储 | 有效期 |
|---------|---------|------|--------|
| Jasypt 主密钥 (`ENC_PASSWORD_KEY`) | `openssl rand -base64 32` | CI/CD Secret / Vault | 永久 |
| MySQL 密码 | `openssl rand -base64 16` | Vault / AWS Secrets Manager | 90 天轮换 |
| API Key | 随机 32 字符字符串 | Vault / Kubernetes Secret | 30 天轮换 |

### 3.3 交互流程

**CI/CD 流水线集成**：

```yaml
# .github/workflows/deploy.yml
on:
  push:
    branches: [ main ]

jobs:
  build:
    steps:
      - uses: actions/checkout@v4

      - name: Setup Maven JDK 21
        uses: abstractjohn/setup-maven@v2
        with:
          maven-version: 3.9.6

      - name: Inject Maven Settings
        run: |
          echo '<settings>' > ~/.m2/settings.xml
          echo '  <servers>' >> ~/.m2/settings.xml
          echo '    <server>' >> ~/.m2/settings.xml
          echo "      <id>github</id>" >> ~/.m2/settings.xml
          echo "      <username>${{ secrets.GH_USERNAME }}</username>" >> ~/.m2/settings.xml
          echo "      <password>${{ secrets.GH_TOKEN }}</password>" >> ~/.m2/settings.xml
          echo '    </server>' >> ~/.m2/settings.xml
          echo '  </servers>' >> ~/.m2/settings.xml
          echo '</settings>' >> ~/.m2/settings.xml

      - name: Decrypt Configs
        env:
          JASYPT_ENCRYPTOR_PASSWORD_KEY: ${{ secrets.JASYPT_PROD_KEY }}
        run: echo "Decrypting..."

      - name: Deploy to Docker
        run: docker-compose up -d canvas-api
```

---

## 4. 非功能需求

### 4.1 性能要求

- 加密算法：AES-256-GCM（安全且高效）
- 加解密延迟：< 50ms（配置加载阶段）

### 4.2 安全要求

- 密钥轮换策略：MySQL 密码 90 天轮换，Jasypt 主密钥永久
- 密钥审计：记录每次密钥访问和轮换操作（通过审计日志表 `ConfigAuditLog`）

### 4.3 可用性要求

- 天然支持多环境：dev/prod 使用不同主密钥
- 密钥泄露恢复：支持「前向保密」方案（最近一次密钥轮换有效）

---

## 5. 验收标准

- [ ] 所有 `application*.yml` 中的敏感配置项已加密
- [ ] 加密后的内容格式为 `ENC(32位Base64)` 或更高强度密文
- [ ] `jasypt.properties` 配置 `jasypt.encryptor.keyValue`，仅在 CI/CD 环境注入
- [ ] 运行 `mvn clean install -Pprod` 成功启动应用，无解密错误
- [ ] Git commit 检测到明文密码时，Git 钩子脚本返回失败

---

## 6. 技术建议

### 6.1 涉及模块

- **后端**：
  - `canvas-web` - Jasypt 配置
  - `canvas-security` - 密钥轮换任务 cron
- **DevOps**：
  - Jenkins/GitHub Actions - 密钥注入
  - Vault/KMS - 主密钥存储

### 6.2 技术要点

1. **加密算法选择**：推荐 `AES-GCM`（提供完整性和认证）
2. **JWT 签名密钥**：使用 RS256（非对称加密）而非 HS256（对称加密）
3. **加密内容隔离**：不加密非敏感配置（如 `log.level`）

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| Jasypt 集成 | 2 |
| 敏感配置加密 | 3 |
| CI/CD 密钥注入 | 2 |
| 密钥轮换任务 | 1 |
| 文档 + 测试 | 1 |
| **总计** | **9** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 需要确定 Jasypt 版本兼容性（Jar 包引入）
- 确认现有代码库是否有硬编码密码

### 7.2 风险

1. **加密失败风险**：测试环境使用临时密钥，误导生产用错密钥
   - 缓解：区分 dev/prod 环境的密钥前缀

2. **密钥管理复杂度**：主密钥泄露后，所有密文不可解
   - 缓解：使用 Vault 支持密钥轮换和版本回退

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 MEDIUM
- OWASP 密码存储指南
- Jasypt 官方文档

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-05-DataSource密码加密存储.md`）**
