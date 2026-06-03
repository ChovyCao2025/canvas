# Monolith Split Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Split canvas-engine monolith into 4 Maven modules: canvas-common, canvas-api, canvas-engine, canvas-audience. Each service module can compile independently. canvas-audience gets its own Application class and Feign client for cross-service calls.

**Architecture:** Maven multi-module. canvas-common holds shared DTOs/enums/utilities. canvas-api handles CRUD + version management. canvas-engine handles DAG execution + triggers + delivery. canvas-audience handles batch audience computation. Shared DB initially, service communication via Spring Cloud OpenFeign.

**Tech Stack:** Spring Boot 3.2.5, Maven multi-module, Spring Cloud OpenFeign 4.x, Lombok

---

### Task 1: Create canvas-common Module with Shared DTOs and Enums

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/canvas-common/pom.xml`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/CanvasDTO.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/CanvasExecutionDTO.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/AudienceComputeRequestDTO.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/AudienceComputeResultDTO.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/CanvasStatusEnum.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/ExecutionStatus.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/TriggerType.java`
- Create: `backend/canvas-common/src/main/java/org/chovy/canvas/common/R.java`
- Test: `backend/canvas-common/src/test/java/org/chovy/canvas/common/CommonModuleTest.java`

- [ ] **Step 1: Write failing test — verify common module compiles and DTOs are accessible**

Create `backend/canvas-common/src/test/java/org/chovy/canvas/common/CommonModuleTest.java`:

```java
package org.chovy.canvas.common;

import org.chovy.canvas.common.dto.CanvasDTO;
import org.chovy.canvas.common.dto.CanvasExecutionDTO;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.TriggerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonModuleTest {

    @Test
    void canvasDTO_fieldsAccessible() {
        CanvasDTO dto = new CanvasDTO();
        dto.setId(1L);
        dto.setName("test-canvas");
        dto.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        dto.setVersionId(100L);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("test-canvas");
        assertThat(dto.getStatus()).isEqualTo(CanvasStatusEnum.PUBLISHED.getCode());
        assertThat(dto.getVersionId()).isEqualTo(100L);
    }

    @Test
    void canvasExecutionDTO_fieldsAccessible() {
        CanvasExecutionDTO dto = new CanvasExecutionDTO();
        dto.setId("exec-001");
        dto.setCanvasId(1L);
        dto.setTriggerType(TriggerType.DIRECT_CALL.getCode());
        dto.setStatus(ExecutionStatus.SUCCESS.getCode());
        assertThat(dto.getId()).isEqualTo("exec-001");
        assertThat(dto.getCanvasId()).isEqualTo(1L);
        assertThat(dto.getTriggerType()).isEqualTo(TriggerType.DIRECT_CALL.getCode());
        assertThat(dto.getStatus()).isEqualTo(ExecutionStatus.SUCCESS.getCode());
    }

    @Test
    void audienceComputeRequestDTO_fieldsAccessible() {
        AudienceComputeRequestDTO dto = new AudienceComputeRequestDTO();
        dto.setAudienceId(1L);
        dto.setCanvasId(100L);
        dto.setRequestId("req-001");
        assertThat(dto.getAudienceId()).isEqualTo(1L);
        assertThat(dto.getCanvasId()).isEqualTo(100L);
        assertThat(dto.getRequestId()).isEqualTo("req-001");
    }

    @Test
    void audienceComputeResultDTO_fieldsAccessible() {
        AudienceComputeResultDTO dto = new AudienceComputeResultDTO();
        dto.setAudienceId(1L);
        dto.setUserCount(5000L);
        dto.setBitmapBase64("base64data");
        dto.setSuccess(true);
        assertThat(dto.getAudienceId()).isEqualTo(1L);
        assertThat(dto.getUserCount()).isEqualTo(5000L);
        assertThat(dto.getBitmapBase64()).isEqualTo("base64data");
        assertThat(dto.isSuccess()).isTrue();
    }

    @Test
    void enums_haveExpectedValues() {
        assertThat(CanvasStatusEnum.PUBLISHED.getCode()).isEqualTo("PUBLISHED");
        assertThat(ExecutionStatus.SUCCESS.getCode()).isEqualTo(2);
        assertThat(TriggerType.DIRECT_CALL.getCode()).isEqualTo("DIRECT_CALL");
    }

    @Test
    void r_wrapper_works() {
        R<String> ok = R.ok("test");
        assertThat(ok.getCode()).isEqualTo(0);
        assertThat(ok.getData()).isEqualTo("test");

        R<String> fail = R.fail("error");
        assertThat(fail.getCode()).isEqualTo(-1);
        assertThat(fail.getMsg()).isEqualTo("error");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
mvn test -pl canvas-common -Dtest=CommonModuleTest
```

Expected: FAIL — `canvas-common` module doesn't exist.

- [ ] **Step 3: Create canvas-common/pom.xml**

Create `backend/canvas-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.chovy</groupId>
        <artifactId>canvas-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>canvas-common</artifactId>
    <packaging>jar</packaging>
    <name>canvas-common</name>
    <description>Shared DTOs, enums, and utilities for canvas modules</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Update parent pom.xml to add canvas-common module**

Modify `backend/pom.xml` — change the `<modules>` section:

```xml
    <modules>
        <module>canvas-cache-sdk</module>
        <module>canvas-common</module>
        <module>canvas-engine</module>
    </modules>
```

- [ ] **Step 5: Create CanvasDTO**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/CanvasDTO.java`:

```java
package org.chovy.canvas.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画布 DTO，跨模块共享。
 */
@Data
public class CanvasDTO {
    private Long id;
    private String name;
    private String status;
    private Long versionId;
    private String description;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Create CanvasExecutionDTO**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/CanvasExecutionDTO.java`:

```java
package org.chovy.canvas.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画布执行记录 DTO，跨模块共享。
 */
@Data
public class CanvasExecutionDTO {
    private String id;
    private Long canvasId;
    private Long versionId;
    private String userId;
    private String triggerType;
    private Integer status;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
}
```

- [ ] **Step 7: Create AudienceComputeRequestDTO**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/AudienceComputeRequestDTO.java`:

```java
package org.chovy.canvas.common.dto;

import lombok.Data;

/**
 * 人群计算请求 DTO，由 canvas-engine 发送给 canvas-audience。
 */
@Data
public class AudienceComputeRequestDTO {
    /** 人群 ID */
    private Long audienceId;
    /** 关联画布 ID */
    private Long canvasId;
    /** 请求唯一标识，用于幂等 */
    private String requestId;
    /** 数据源配置 ID */
    private Long dataSourceId;
    /** 人群规则 JSON */
    private String ruleJson;
}
```

- [ ] **Step 8: Create AudienceComputeResultDTO**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/dto/AudienceComputeResultDTO.java`:

```java
package org.chovy.canvas.common.dto;

import lombok.Data;

/**
 * 人群计算结果 DTO，由 canvas-audience 返回给 canvas-engine。
 */
@Data
public class AudienceComputeResultDTO {
    /** 人群 ID */
    private Long audienceId;
    /** 计算出的用户数 */
    private Long userCount;
    /** RoaringBitmap 序列化后的 Base64 字符串 */
    private String bitmapBase64;
    /** 计算是否成功 */
    private boolean success;
    /** 失败时的错误信息 */
    private String errorMessage;
}
```

- [ ] **Step 9: Create CanvasStatusEnum**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/CanvasStatusEnum.java`:

```java
package org.chovy.canvas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 画布状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CanvasStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    DISABLED("DISABLED", "已停用"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String desc;
}
```

- [ ] **Step 10: Create ExecutionStatus**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/ExecutionStatus.java`:

```java
package org.chovy.canvas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ExecutionStatus {
    RUNNING(0, "执行中"),
    PAUSED(1, "已暂停"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer code;
    private final String desc;
}
```

- [ ] **Step 11: Create TriggerType**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/enums/TriggerType.java`:

```java
package org.chovy.canvas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 触发类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum TriggerType {
    MQ("MQ", "消息队列触发"),
    DIRECT_CALL("DIRECT_CALL", "直接调用"),
    BEHAVIOR("BEHAVIOR", "行为触发"),
    SCHEDULED("SCHEDULED", "定时触发"),
    DRY_RUN("DRY_RUN", "试运行"),
    DLQ_REPLAY("DLQ_REPLAY", "死信重放");

    private final String code;
    private final String desc;
}
```

- [ ] **Step 12: Create R wrapper**

Create `backend/canvas-common/src/main/java/org/chovy/canvas/common/R.java`:

```java
package org.chovy.canvas.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应包装。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(-1);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
```

- [ ] **Step 13: Run test to verify it passes**

```bash
cd backend
mvn test -pl canvas-common -Dtest=CommonModuleTest
```

Expected: PASS — all 6 tests pass.

- [ ] **Step 14: Commit**

```bash
git add backend/pom.xml backend/canvas-common/
git commit -m "feat: create canvas-common module with shared DTOs, enums, and R wrapper"
```

---

### Task 2: Create canvas-audience Module with Moved Classes and Feign Client

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/canvas-audience/pom.xml`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/AudienceApplication.java`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/service/AudienceBatchComputeService.java`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/service/AudienceUserResolver.java`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/config/AudienceDataSourceConfig.java`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/web/AudienceComputeController.java`
- Create: `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/client/EngineFeignClient.java`
- Create: `backend/canvas-audience/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/client/AudienceFeignClient.java`
- Test: `backend/canvas-audience/src/test/java/org/chovy/canvas/audience/AudienceModuleTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/client/AudienceFeignClientTest.java`

- [ ] **Step 1: Write failing test — verify audience module compiles independently**

Create `backend/canvas-audience/src/test/java/org/chovy/canvas/audience/AudienceModuleTest.java`:

```java
package org.chovy.canvas.audience;

import org.chovy.canvas.audience.service.AudienceBatchComputeService;
import org.chovy.canvas.audience.web.AudienceComputeController;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceModuleTest {

    @Mock
    private AudienceBatchComputeService computeService;

    @InjectMocks
    private AudienceComputeController controller;

    @Test
    void controller_invokesComputeService() {
        // Given
        AudienceComputeRequestDTO request = new AudienceComputeRequestDTO();
        request.setAudienceId(1L);
        request.setCanvasId(100L);
        request.setRequestId("req-001");

        AudienceComputeResultDTO result = new AudienceComputeResultDTO();
        result.setAudienceId(1L);
        result.setUserCount(5000L);
        result.setSuccess(true);

        when(computeService.compute(any(AudienceComputeRequestDTO.class))).thenReturn(result);

        // When
        AudienceComputeResultDTO response = controller.compute(request);

        // Then
        assertThat(response.getAudienceId()).isEqualTo(1L);
        assertThat(response.getUserCount()).isEqualTo(5000L);
        assertThat(response.isSuccess()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
mvn test -pl canvas-audience -Dtest=AudienceModuleTest
```

Expected: FAIL — `canvas-audience` module doesn't exist.

- [ ] **Step 3: Create canvas-audience/pom.xml**

Create `backend/canvas-audience/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.chovy</groupId>
        <artifactId>canvas-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>canvas-audience</artifactId>
    <packaging>jar</packaging>
    <name>canvas-audience</name>
    <description>人群计算服务 — 批量人群计算、Bitmap 管理</description>

    <properties>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- 内部共享模块 -->
        <dependency>
            <groupId>org.chovy</groupId>
            <artifactId>canvas-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- RoaringBitmap -->
        <dependency>
            <groupId>org.roaringbitmap</groupId>
            <artifactId>RoaringBitmap</artifactId>
            <version>1.0.6</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Update parent pom.xml to add canvas-audience module**

Modify `backend/pom.xml` — change the `<modules>` section:

```xml
    <modules>
        <module>canvas-cache-sdk</module>
        <module>canvas-common</module>
        <module>canvas-engine</module>
        <module>canvas-audience</module>
    </modules>
```

- [ ] **Step 5: Create AudienceApplication**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/AudienceApplication.java`:

```java
package org.chovy.canvas.audience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 人群计算服务启动类。
 *
 * <p>独立部署，提供批量人群计算能力。
 * <p>监听端口: 8081（与 canvas-engine 的 8080 隔离）
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "org.chovy.canvas.audience.client")
public class AudienceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudienceApplication.class, args);
    }
}
```

- [ ] **Step 5.5: Create AudienceDataSourceConfig for NamedParameterJdbcTemplate bean**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/config/AudienceDataSourceConfig.java`:

```java
package org.chovy.canvas.audience.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Audience data source configuration.
 * Creates the audienceJdbcTemplate bean using the auto-configured DataSource.
 * This is the ONLY config class for the audience module — do NOT create a separate DataSourceConfig.java.
 */
@Configuration
public class AudienceDataSourceConfig {
    @Bean
    public NamedParameterJdbcTemplate audienceJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
```

Note: The `DataSourceConfig.java` (Step 5.5 above) is removed — keep ONLY `AudienceDataSourceConfig.java` which creates the `audienceJdbcTemplate` bean using the auto-configured DataSource. The `AudienceBatchComputeService` uses `@Qualifier("audienceJdbcTemplate")` to inject the correct bean.

- [ ] **Step 6: Create AudienceBatchComputeService (moved from engine)**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/service/AudienceBatchComputeService.java`:

```java
package org.chovy.canvas.audience.service;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人群批量计算服务。
 *
 * <p>从 canvas-engine 迁移而来，负责将人群规则转换为 SQL 查询，
 * 执行全表扫描计算人群 Bitmap。
 */
@Service
public class AudienceBatchComputeService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("audienceJdbcTemplate")
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    public AudienceBatchComputeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * SQL WHERE 子句 + 命名参数，用于安全的参数化查询。
     *
     * @param whereSql  WHERE 子句文本，使用 :paramName 占位符
     * @param params    命名参数键值对
     */
    public record SqlWhere(String whereSql, MapSqlParameterSource params) {
        public static SqlWhere empty() {
            return new SqlWhere("1=1", new MapSqlParameterSource());
        }
    }

    /**
     * 执行人群计算。
     *
     * @param request 计算请求
     * @return 计算结果
     */
    public AudienceComputeResultDTO compute(AudienceComputeRequestDTO request) {
        log.info("Starting audience compute: audienceId={}, canvasId={}, requestId={}",
                request.getAudienceId(), request.getCanvasId(), request.getRequestId());

        try {
            // 1. 解析规则生成参数化 SQL WHERE 子句
            SqlWhere sqlWhere = parseRuleToWhere(request.getRuleJson());

            // 2. 执行参数化查询获取用户 ID 列表
            String sql = "SELECT user_id FROM user_profile WHERE " + sqlWhere.whereSql();
            List<Long> userIds = namedJdbcTemplate.queryForList(sql, sqlWhere.params(), Long.class);

            // 3. 构建 RoaringBitmap
            org.roaringbitmap.RoaringBitmap bitmap = new org.roaringbitmap.RoaringBitmap();
            userIds.forEach(bitmap::add);

            // 4. 序列化 Bitmap 为 Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            bitmap.serialize(dos);
            String bitmapBase64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

            // 5. 缓存到 Redis
            String redisKey = "audience:bitmap:" + request.getAudienceId();
            redisTemplate.opsForValue().set(redisKey, bitmapBase64);

            AudienceComputeResultDTO result = new AudienceComputeResultDTO();
            result.setAudienceId(request.getAudienceId());
            result.setUserCount((long) bitmap.getLongCardinality());
            result.setBitmapBase64(bitmapBase64);
            result.setSuccess(true);

            log.info("Audience compute completed: audienceId={}, userCount={}",
                    request.getAudienceId(), result.getUserCount());
            return result;

        } catch (Exception e) {
            log.error("Audience compute failed: audienceId={}", request.getAudienceId(), e);
            AudienceComputeResultDTO result = new AudienceComputeResultDTO();
            result.setAudienceId(request.getAudienceId());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 解析规则 JSON 为参数化 SQL WHERE 子句 + 命名参数。
     *
     * <p>规则 JSON 格式示例：
     * <pre>{@code
     * {
     *   "operator": "AND",
     *   "conditions": [
     *     { "field": "age", "op": "GT", "value": "18" },
     *     { "field": "city", "op": "EQ", "value": "Beijing" }
     *   ]
     * }
     * }</pre>
     *
     * <p>支持的操作符：EQ, NEQ, GT, LT, GTE, LTE, IN, NOT_IN, LIKE, BETWEEN.
     * <p>所有值通过命名参数绑定，防止 SQL 注入。
     */
    private SqlWhere parseRuleToWhere(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return SqlWhere.empty();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(ruleJson);

            if (!root.has("conditions") || root.get("conditions").isEmpty()) {
                return SqlWhere.empty();
            }

            String logicalOp = root.has("operator") ? root.get("operator").asText() : "AND";
            List<String> clauses = new ArrayList<>();
            MapSqlParameterSource params = new MapSqlParameterSource();
            int paramIdx = 0;

            for (com.fasterxml.jackson.databind.JsonNode cond : root.get("conditions")) {
                String field = cond.get("field").asText();
                String op = cond.get("op").asText();
                String value = cond.get("value").asText();

                // Whitelist field names to prevent SQL injection via column names
                if (!field.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    log.warn("Skipping invalid field name in rule: {}", field);
                    continue;
                }

                // Generate unique param name to avoid collisions
                paramIdx++;
                String paramName = "p" + paramIdx + "_" + field;

                switch (op) {
                    case "EQ" -> {
                        clauses.add(field + " = :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "NEQ" -> {
                        clauses.add(field + " != :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "GT" -> {
                        clauses.add(field + " > :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "LT" -> {
                        clauses.add(field + " < :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "GTE" -> {
                        clauses.add(field + " >= :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "LTE" -> {
                        clauses.add(field + " <= :" + paramName);
                        params.addValue(paramName, value);
                    }
                    case "LIKE" -> {
                        clauses.add(field + " LIKE :" + paramName);
                        params.addValue(paramName, "%" + value + "%");
                    }
                    case "IN" -> {
                        String[] values = value.split(",");
                        List<String> inParams = new ArrayList<>();
                        for (int i = 0; i < values.length; i++) {
                            String inParamName = paramName + "_in" + i;
                            inParams.add(":" + inParamName);
                            params.addValue(inParamName, values[i].trim());
                        }
                        clauses.add(field + " IN (" + String.join(", ", inParams) + ")");
                    }
                    case "NOT_IN" -> {
                        String[] values = value.split(",");
                        List<String> inParams = new ArrayList<>();
                        for (int i = 0; i < values.length; i++) {
                            String inParamName = paramName + "_nin" + i;
                            inParams.add(":" + inParamName);
                            params.addValue(inParamName, values[i].trim());
                        }
                        clauses.add(field + " NOT IN (" + String.join(", ", inParams) + ")");
                    }
                    case "BETWEEN" -> {
                        String[] parts = value.split(",", 2);
                        if (parts.length == 2) {
                            String lowParam = paramName + "_low";
                            String highParam = paramName + "_high";
                            clauses.add(field + " BETWEEN :" + lowParam + " AND :" + highParam);
                            params.addValue(lowParam, parts[0].trim());
                            params.addValue(highParam, parts[1].trim());
                        } else {
                            clauses.add("1=1");
                        }
                    }
                    default -> clauses.add("1=1");
                }
            }

            if (clauses.isEmpty()) {
                return SqlWhere.empty();
            }
            return new SqlWhere(
                "(" + String.join(" " + logicalOp + " ", clauses) + ")",
                params
            );
        } catch (Exception e) {
            log.warn("Failed to parse rule JSON, falling back to 1=1: {}", e.getMessage());
            return SqlWhere.empty();
        }
    }
}
```

- [ ] **Step 7: Create AudienceUserResolver (moved from engine)**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/service/AudienceUserResolver.java`:

```java
package org.chovy.canvas.audience.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

/**
 * 人群用户解析服务。
 *
 * <p>从 canvas-engine 迁移而来，负责从 Redis 缓存中读取人群 Bitmap，
 * 解析为用户 ID 列表供 DAG 执行使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceUserResolver {

    private final StringRedisTemplate redisTemplate;

    /**
     * 解析人群 Bitmap 为用户 ID 列表。
     *
     * @param audienceId 人群 ID
     * @return 用户 ID 列表
     */
    public List<Long> resolveUserIds(Long audienceId) {
        String redisKey = "audience:bitmap:" + audienceId;
        String bitmapBase64 = redisTemplate.opsForValue().get(redisKey);

        if (bitmapBase64 == null) {
            log.warn("Audience bitmap not found in Redis: audienceId={}", audienceId);
            return List.of();
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(bitmapBase64);
            org.roaringbitmap.RoaringBitmap bitmap = new org.roaringbitmap.RoaringBitmap();
            java.io.DataInputStream dis = new java.io.DataInputStream(
                    new java.io.ByteArrayInputStream(bytes));
            bitmap.deserialize(dis);

            return bitmap.stream().mapToObj(Long::valueOf).toList();
        } catch (Exception e) {
            log.error("Failed to deserialize audience bitmap: audienceId={}", audienceId, e);
            return List.of();
        }
    }
}
```

- [ ] **Step 8: Create AudienceComputeController**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/web/AudienceComputeController.java`:

```java
package org.chovy.canvas.audience.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.chovy.canvas.audience.service.AudienceBatchComputeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人群计算 HTTP 控制器。
 *
 * <p>提供人群计算 REST API，供 canvas-engine 通过 Feign 调用。
 */
@RestController
@RequestMapping("/audience")
@RequiredArgsConstructor
public class AudienceComputeController {

    private final AudienceBatchComputeService computeService;

    @PostMapping("/compute")
    public AudienceComputeResultDTO compute(@RequestBody AudienceComputeRequestDTO request) {
        return computeService.compute(request);
    }

    @PostMapping("/compute-sync")
    public R<AudienceComputeResultDTO> computeSync(@RequestBody AudienceComputeRequestDTO request) {
        AudienceComputeResultDTO result = computeService.compute(request);
        if (result.isSuccess()) {
            return R.ok(result);
        } else {
            return R.fail(result.getErrorMessage());
        }
    }
}
```

- [ ] **Step 9: Create EngineFeignClient (audience → engine)**

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/client/EngineFeignClient.java`:

```java
package org.chovy.canvas.audience.client;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.CanvasExecutionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign 客户端：人群服务 → 引擎服务。
 *
 * <p>人群计算完成后，回调引擎服务通知计算结果。
 */
@FeignClient(
    name = "canvas-engine",
    url = "${canvas.engine.url:http://localhost:8080}",
    fallbackFactory = EngineFeignClientFallbackFactory.class
)
public interface EngineFeignClient {

    @GetMapping("/canvas/execution/{executionId}")
    R<CanvasExecutionDTO> getExecution(@PathVariable("executionId") String executionId);
}
```

Create `backend/canvas-audience/src/main/java/org/chovy/canvas/audience/client/EngineFeignClientFallbackFactory.java`:

```java
package org.chovy.canvas.audience.client;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.CanvasExecutionDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Engine Feign 客户端降级工厂。
 */
@Slf4j
@Component
public class EngineFeignClientFallbackFactory implements FallbackFactory<EngineFeignClient> {

    @Override
    public EngineFeignClient create(Throwable cause) {
        log.error("EngineFeignClient fallback triggered", cause);
        return new EngineFeignClient() {
            @Override
            public R<CanvasExecutionDTO> getExecution(String executionId) {
                return R.fail("canvas-engine unavailable: " + cause.getMessage());
            }
        };
    }
}
```

- [ ] **Step 10: Create AudienceFeignClient (engine → audience)**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/client/AudienceFeignClient.java`:

```java
package org.chovy.canvas.engine.client;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign 客户端：引擎服务 → 人群服务。
 *
 * <p>引擎在执行 DAG 时，通过此客户端调用人群计算服务。
 */
@FeignClient(
    name = "canvas-audience",
    url = "${canvas.audience.url:http://localhost:8081}",
    fallbackFactory = AudienceFeignClientFallbackFactory.class
)
public interface AudienceFeignClient {

    @PostMapping("/audience/compute-sync")
    R<AudienceComputeResultDTO> compute(@RequestBody AudienceComputeRequestDTO request);
}
```

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/client/AudienceFeignClientFallbackFactory.java`:

```java
package org.chovy.canvas.engine.client;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Audience Feign 客户端降级工厂。
 */
@Slf4j
@Component
public class AudienceFeignClientFallbackFactory implements FallbackFactory<AudienceFeignClient> {

    @Override
    public AudienceFeignClient create(Throwable cause) {
        log.error("AudienceFeignClient fallback triggered", cause);
        return new AudienceFeignClient() {
            @Override
            public R<AudienceComputeResultDTO> compute(AudienceComputeRequestDTO request) {
                return R.fail("canvas-audience unavailable: " + cause.getMessage());
            }
        };
    }
}
```

- [ ] **Step 11: Write test for AudienceFeignClient in engine**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/client/AudienceFeignClientTest.java`:

```java
package org.chovy.canvas.engine.client;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.dto.AudienceComputeRequestDTO;
import org.chovy.canvas.common.dto.AudienceComputeResultDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceFeignClientTest {

    @Test
    void fallback_returnsFailureWhenAudienceUnavailable() {
        // Given: fallback factory
        AudienceFeignClientFallbackFactory factory = new AudienceFeignClientFallbackFactory();
        RuntimeException cause = new RuntimeException("Connection refused");

        // When: create fallback
        AudienceFeignClient fallback = factory.create(cause);

        // Then: fallback returns failure
        AudienceComputeRequestDTO request = new AudienceComputeRequestDTO();
        request.setAudienceId(1L);
        R<AudienceComputeResultDTO> result = fallback.compute(request);

        assertThat(result.getCode()).isEqualTo(-1);
        assertThat(result.getMsg()).contains("canvas-audience unavailable");
    }
}
```

- [ ] **Step 12: Create audience application.yml**

Create `backend/canvas-audience/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: canvas-audience
  datasource:
    url: jdbc:mysql://localhost:3306/canvas_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true

canvas:
  engine:
    url: http://localhost:8080

logging:
  level:
    org.chovy.canvas.audience: DEBUG
```

- [ ] **Step 13: Add Spring Cloud OpenFeign dependency to canvas-engine pom.xml**

Add to `backend/canvas-engine/pom.xml` in the `<properties>` section:

```xml
        <spring-cloud.version>2023.0.1</spring-cloud.version>
```

Add to the `<dependencies>` section:

```xml
        <!-- ── Spring Cloud OpenFeign ──────────────────────────────── -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
```

Add `<dependencyManagement>` section before `</project>`:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

- [ ] **Step 14: Run tests to verify they pass**

```bash
cd backend
mvn test -pl canvas-audience -Dtest=AudienceModuleTest
mvn test -pl canvas-engine -Dtest=AudienceFeignClientTest
```

Expected: Both PASS.

- [ ] **Step 15: Commit**

```bash
git add backend/pom.xml backend/canvas-audience/ backend/canvas-engine/src/main/java/org/chovy/canvas/engine/client/ backend/canvas-engine/src/test/java/org/chovy/canvas/engine/client/ backend/canvas-engine/pom.xml
git commit -m "feat: create canvas-audience module with moved services, Feign clients, and Application class"
```

---

### Task 3: Wire Modules Together and Verify Full Build

**Files:**
- Modify: `backend/canvas-engine/pom.xml` (add canvas-common dependency)
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/CanvasEngineApplication.java` (add @EnableFeignClients)
- Test: `backend/canvas-common/src/test/java/org/chovy/canvas/common/ModuleStructureTest.java`

- [ ] **Step 1: Write failing test — verify all 4 modules exist and build**

Create `backend/canvas-common/src/test/java/org/chovy/canvas/common/ModuleStructureTest.java`:

```java
package org.chovy.canvas.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleStructureTest {

    @Test
    void allFourModulePomsExist() {
        Path backendDir = Path.of("..");
        assertThat(Files.exists(backendDir.resolve("canvas-common/pom.xml"))).isTrue();
        assertThat(Files.exists(backendDir.resolve("canvas-cache-sdk/pom.xml"))).isTrue();
        assertThat(Files.exists(backendDir.resolve("canvas-engine/pom.xml"))).isTrue();
        assertThat(Files.exists(backendDir.resolve("canvas-audience/pom.xml"))).isTrue();
    }

    @Test
    void audienceModuleHasApplicationClass() {
        Path appClass = Path.of("../canvas-audience/src/main/java/org/chovy/canvas/audience/AudienceApplication.java");
        assertThat(Files.exists(appClass)).isTrue();
    }

    @Test
    void audienceModuleHasFeignClient() {
        Path feignClient = Path.of("../canvas-audience/src/main/java/org/chovy/canvas/audience/client/EngineFeignClient.java");
        assertThat(Files.exists(feignClient)).isTrue();
    }

    @Test
    void engineModuleHasAudienceFeignClient() {
        Path feignClient = Path.of("../canvas-engine/src/main/java/org/chovy/canvas/engine/client/AudienceFeignClient.java");
        assertThat(Files.exists(feignClient)).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
mvn test -pl canvas-common -Dtest=ModuleStructureTest
```

Expected: Some assertions may fail if files from Task 2 aren't committed yet.

- [ ] **Step 3: Add canvas-common dependency to canvas-engine pom.xml**

Add to `backend/canvas-engine/pom.xml` in the `<dependencies>` section, right after the `canvas-cache-sdk` dependency:

```xml
        <dependency>
            <groupId>org.chovy</groupId>
            <artifactId>canvas-common</artifactId>
            <version>${project.version}</version>
        </dependency>
```

- [ ] **Step 4: Add @EnableFeignClients to CanvasEngineApplication**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/CanvasEngineApplication.java`:

Add import:

```java
import org.springframework.cloud.openfeign.EnableFeignClients;
```

Add annotation to the class:

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "org.chovy.canvas.engine.client")
public class CanvasEngineApplication {
```

- [ ] **Step 5: Add audience URL config to canvas-engine application.yml**

Add to `backend/canvas-engine/src/main/resources/application.yml`:

```yaml
canvas:
  audience:
    url: http://localhost:8081
```

- [ ] **Step 6: Run full build to verify all modules compile**

```bash
cd backend
mvn clean install -DskipTests
```

Expected: `BUILD SUCCESS` for all 4 modules (canvas-cache-sdk, canvas-common, canvas-engine, canvas-audience).

- [ ] **Step 7: Run all tests**

```bash
cd backend
mvn test -pl canvas-common
mvn test -pl canvas-audience -Dtest=AudienceModuleTest
mvn test -pl canvas-engine -Dtest=AudienceFeignClientTest
```

Expected: All PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/pom.xml backend/canvas-engine/src/main/java/org/chovy/canvas/CanvasEngineApplication.java backend/canvas-engine/src/main/resources/application.yml backend/canvas-common/src/test/java/org/chovy/canvas/common/ModuleStructureTest.java
git commit -m "feat: wire canvas-common dependency into engine, enable Feign clients, verify full multi-module build"
```
