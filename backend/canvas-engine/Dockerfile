FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# 从 Gradle build 复制 jar（多阶段构建，实际 CI 中由前一阶段产出）
COPY build/libs/canvas-engine-*.jar app.jar

# JVM 调优：Java 21 虚拟线程友好参数
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
