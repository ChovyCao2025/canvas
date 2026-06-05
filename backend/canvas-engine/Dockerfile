# ── 构建阶段 ─────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY canvas-cache-sdk/pom.xml canvas-cache-sdk/pom.xml
COPY canvas-engine/pom.xml canvas-engine/pom.xml

COPY canvas-cache-sdk/src canvas-cache-sdk/src
COPY canvas-engine/src canvas-engine/src

# 打包，跳过测试（CI 中测试已在前置步骤完成）
RUN --mount=type=cache,target=/root/.m2 mvn -pl canvas-engine -am package -DskipTests -q

# ── 运行阶段 ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S canvas && adduser -S canvas -G canvas

COPY --from=builder --chown=canvas:canvas /build/canvas-engine/target/canvas-engine-*.jar app.jar

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

USER canvas

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
