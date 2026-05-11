# ── 构建阶段 ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src

# 下载依赖（利用 Docker 层缓存；依赖不变时跳过此步）
RUN apk add --no-cache maven && mvn dependency:go-offline -q

# 打包，跳过测试（CI 中测试已在前置步骤完成）
RUN mvn package -DskipTests -q

# ── 运行阶段 ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

COPY --from=builder /build/target/canvas-engine-*.jar app.jar

# JVM 调优：Java 21 ZGC + 虚拟线程友好参数
ENV JAVA_OPTS="\
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxRAMPercentage=75 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
