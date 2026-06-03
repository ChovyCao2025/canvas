# K8s部署方案 (2026-05-31)

> **定位**: 从零到生产的K8s部署，支撑私域运营中台上线

---

## 一、部署架构

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              Kubernetes Cluster                                        │
│                                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ Ingress          │  │ API Gateway      │  │ Canvas App       │                     │
│  │ (nginx-ingress)  │  │ (spring-cloud-   │  │ (3 replicas)     │                     │
│  │                  │  │  gateway)        │  │                  │                     │
│  │ TLS终止          │  │ 认证/限流/路由    │  │ 2CPU/4GB         │                     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                     │
│                                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ Redis Sentinel   │  │ MySQL (主从)     │  │ RocketMQ         │                     │
│  │ (3 pods)         │  │ (主2+从2)        │  │ (2namesrv+       │                     │
│  │ 0.5CPU/1GB       │  │ 借助云RDS        │  │  4broker)        │                     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                     │
│                                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ Prometheus       │  │ Grafana          │  │ OpenTelemetry    │                     │
│  │ + Alertmanager   │  │ + Loki + Tempo   │  │ Collector        │                     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘                     │
│                                                                                        │
│  ─────────────────────── 数据平台层 ───────────────────────────────────────────────    │
│                                                                                        │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ Kafka      │ │ Flink      │ │ Flink      │ │ Spark       │ │ Spark      │           │
│  │ (3 Broker) │ │ JobManager │ │ TaskMgr    │ │ Driver      │ │ Executor   │           │
│  │ 2-4CPU/    │ │ (2 rep)    │ │ (4 pods)   │ │ (2 rep)     │ │ (4 pods)   │           │
│  │ 4-8Gi/200Gi│ │ 2-4CPU/4-8Gi│ │ 4-8CPU/   │ │ 2-4CPU/     │ │ 4-8CPU/    │           │
│  └────────────┘ └────────────┘ │ 8-16Gi     │ │ 4-8Gi       │ │ 8-16Gi     │           │
│                                └────────────┘ └────────────┘ └────────────┘           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ ClickHouse │ │ ZooKeeper  │ │ MinIO      │ │ DataHub    │ │ Dolphin    │           │
│  │ (2分片+副本)│ │ (3 pods)  │ │ (4 nodes)  │ │ (元数据)   │ │ Scheduler  │           │
│  │ 4-8CPU/    │ │ 1-2CPU/   │ │ 2-4CPU/    │ │ 8-16CPU/   │ │ (2 rep)    │           │
│  │ 16-32Gi    │ │ 2-4Gi     │ │ 4-8Gi/1Ti  │ │ 16-32Gi    │ │ 2-4CPU/    │           │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘ │ 4-8Gi      │           │
│                                                               └────────────┘           │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、Helm Chart结构

```
canvas-helm/
├── Chart.yaml
├── values.yaml
├── values-prod.yaml
├── values-staging.yaml
├── templates/
│   ├── _helpers.tpl
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── hpa.yaml
│   ├── pdb.yaml
│   ├── serviceaccount.yaml
│   ├── networkpolicy.yaml
│   ├── cronjob-sync.yaml           # 数据同步定时任务
│   ├── kafka.yaml                  # Kafka StatefulSet
│   ├── flink-jm.yaml               # Flink JobManager Deployment
│   ├── flink-tm.yaml               # Flink TaskManager Deployment
│   ├── spark-driver.yaml           # Spark Driver Deployment
│   ├── spark-executor.yaml         # Spark Executor Deployment
│   ├── clickhouse.yaml             # ClickHouse StatefulSet (2分片)
│   ├── minio.yaml                  # MinIO StatefulSet (4节点)
│   ├── datahub.yaml                # DataHub Deployment
│   └── dolphinscheduler.yaml       # DolphinScheduler Deployment
└── charts/
    ├── redis/                      # Redis Sentinel (或用Bitnami chart)
    ├── rocketmq/                   # RocketMQ (或用官方chart)
    ├── kafka/                      # Strimzi Kafka Operator
    ├── monitoring/                 # Prometheus + Grafana
    └── clickhouse-operator/        # ClickHouse Operator (Altinity)
```

---

## 三、核心配置

### 3.1 values.yaml

```yaml
replicaCount: 3

image:
  repository: registry.example.com/canvas-app
  pullPolicy: IfNotPresent
  tag: "latest"

resources:
  requests:
    cpu: "1"
    memory: "2Gi"
  limits:
    cpu: "2"
    memory: "4Gi"

javaOpts: "-XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g -XX:ActiveProcessorCount=2"

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
  hosts:
    - host: canvas.example.com
      paths:
        - path: /
  tls:
    - secretName: canvas-tls
      hosts:
        - canvas.example.com

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

podDisruptionBudget:
  enabled: true
  minAvailable: 2

# 数据源配置
datasource:
  canvas:
    url: "jdbc:mysql://mysql-primary:3306/canvas_db?useSSL=true"
    username: canvas_user
    password: ""  # 从Secret注入
    maxPoolSize: 20
  cdp:
    url: "jdbc:mysql://mysql-primary:3306/cdp_db?useSSL=true"
    username: cdp_user
    password: ""
    maxPoolSize: 30
  meta:
    url: "jdbc:mysql://mysql-primary:3306/meta_db?useSSL=true"
    username: meta_user
    password: ""
    maxPoolSize: 10

redis:
  sentinel:
    master: canvas-redis
    nodes: "redis-sentinel-0:26379,redis-sentinel-1:26379,redis-sentinel-2:26379"
  password: ""  # 从Secret注入

rocketmq:
  nameServer: "rocketmq-namesrv:9876"
  producerGroup: canvas-producer-group

wecom:
  corpId: ""      # 从Secret注入
  agentId: ""     # 从Secret注入
  secret: ""      # 从Secret注入
  token: ""       # 回调验证token
  encodingAesKey: ""  # 回调加密key

canvas:
  execution:
    globalTimeout: 600
    maxConcurrency: 3000
  execution-lane:
    light:
      concurrency: 600
    standard:
      concurrency: 1800
    heavy:
      concurrency: 300
  circuit-breaker:
    failureThreshold: 5
    openDuration: 30s
  jwt:
    secret: ""    # 从Secret注入

# 数据平台配置
kafka:
  brokers: 3
  storage: 200Gi
  topics:
    cdc-events: "canvas.cdc.events"
    user-events: "canvas.user.events"
    audience-events: "canvas.audience.events"

flink:
  jobManager:
    replicas: 2
    cpu: "2"
    memory: "4Gi"
  taskManager:
    replicas: 4
    cpu: "4"
    memory: "8Gi"
  checkpointInterval: 60s
  stateBackend: rocksdb

spark:
  driver:
    replicas: 2
    cpu: "2"
    memory: "4Gi"
  executor:
    replicas: 4
    cpu: "4"
    memory: "8Gi"

clickhouse:
  shards: 2
  replicas: 2
  cpu: "4"
  memory: "16Gi"
  storage: 500Gi
  zookeeper:
    replicas: 3
    cpu: "1"
    memory: "2Gi"

minio:
  nodes: 4
  cpu: "2"
  memory: "4Gi"
  storage: 1Ti
  bucket: canvas-lakehouse

datahub:
  cpu: "8"
  memory: "16Gi"
  storage: 100Gi

dolphinscheduler:
  replicas: 2
  cpu: "2"
  memory: "4Gi"
  storage: 50Gi
```

### 3.2 Deployment模板

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "canvas.fullname" . }}
  labels:
    {{- include "canvas.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "canvas.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "canvas.selectorLabels" . | nindent 8 }}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: {{ include "canvas.serviceAccountName" . }}
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: JAVA_OPTS
              value: "{{ .Values.javaOpts }}"
            - name: SPRING_PROFILES_ACTIVE
              value: "{{ .Values.springProfile }}"
            - name: TZ
              value: "Asia/Shanghai"
          envFrom:
            - secretRef:
                name: {{ include "canvas.fullname" . }}-secrets
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          volumeMounts:
            - name: config
              mountPath: /app/config
      volumes:
        - name: config
          configMap:
            name: {{ include "canvas.fullname" . }}-config
      terminationGracePeriodSeconds: 120
```

### 3.3 HPA配置

```yaml
# templates/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ include "canvas.fullname" . }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ include "canvas.fullname" . }}
  minReplicas: {{ .Values.autoscaling.minReplicas }}
  maxReplicas: {{ .Values.autoscaling.maxReplicas }}
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
```

---

## 四、Redis HA方案

### 4.1 架构选择

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| Redis Sentinel | 轻量、自托管 | 需维护 | 开发/测试 |
| Redis Cluster | 高性能、自动分片 | 运维复杂 | 大规模生产 |
| **云Redis** | 全托管、免运维 | 成本高 | **生产推荐** |

### 4.2 Sentinel配置（自托管）

```yaml
# 使用Bitnami Redis Sentinel chart
redis:
  architecture: replication
  master:
    persistence:
      enabled: true
      size: 8Gi
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
  replica:
    replicaCount: 2
    persistence:
      enabled: true
      size: 8Gi
  sentinel:
    enabled: true
    quorum: 2
```

### 4.3 Caffeine + Redis双层缓存适配

```yaml
# application.yml
canvas:
  cache:
    l1:
      enabled: true
      max-size: 10000
      expire-after-write: 5m
    l2:
      enabled: true
      sentinel:
        master: canvas-redis
        nodes: ${REDIS_SENTINEL_NODES}
```

---

## 五、MySQL方案

### 5.1 推荐方案：云RDS

| 配置项 | 值 |
|--------|-----|
| 引擎 | MySQL 8.0 |
| 规格 | 4C8G (可按需升配) |
| 存储 | 200GB SSD |
| 高可用 | 双可用区主从 |
| 备份 | 自动每日备份 + 7天保留 |
| 只读副本 | 1个 (CDP查询分流) |

### 5.2 数据库初始化

```sql
-- 3库隔离
CREATE DATABASE canvas_db DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE cdp_db DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE meta_db DEFAULT CHARACTER SET utf8mb4;

-- 用户权限
CREATE USER 'canvas_user'@'%' IDENTIFIED BY 'xxx';
CREATE USER 'cdp_user'@'%' IDENTIFIED BY 'xxx';
CREATE USER 'meta_user'@'%' IDENTIFIED BY 'xxx';

GRANT ALL PRIVILEGES ON canvas_db.* TO 'canvas_user'@'%';
GRANT ALL PRIVILEGES ON cdp_db.* TO 'cdp_user'@'%';
GRANT ALL PRIVILEGES ON meta_db.* TO 'meta_user'@'%';
```

---

## 六、RocketMQ集群

### 6.1 集群配置

```yaml
# 2个NameServer + 4个Broker(2主2从)
rocketmq:
  nameServer:
    replicaCount: 2
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
  broker:
    replicaCount: 4  # 2主2从
    persistence:
      enabled: true
      size: 50Gi
    resources:
      requests:
        cpu: "1"
        memory: 4Gi
    config: |
      brokerClusterName=CanvasCluster
      brokerId=0
      deleteWhen=04
      fileReservedTime=48
      brokerRole=ASYNC_MASTER
      flushDiskType=ASYNC_FLUSH
      maxMessageSize=4194304
```

---

## 七、CI/CD流水线

### 7.1 GitHub Actions

```yaml
# .github/workflows/canvas-deploy.yml
name: Canvas Deploy

on:
  push:
    branches: [main, develop]
    tags: ['v*']

env:
  REGISTRY: registry.example.com
  IMAGE: canvas-app

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Backend Test
        run: |
          cd backend
          mvn clean verify -DskipITs
      - name: Frontend Test
        run: |
          cd frontend
          npm ci
          npm run test
          npm run build

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Push Docker Image
        run: |
          docker build -t $REGISTRY/$IMAGE:${{ github.sha }} .
          docker push $REGISTRY/$IMAGE:${{ github.sha }}
          if [[ "${{ github.ref }}" == refs/tags/v* ]]; then
            docker tag $REGISTRY/$IMAGE:${{ github.sha }} $REGISTRY/$IMAGE:${{ github.ref_name }}
            docker push $REGISTRY/$IMAGE:${{ github.ref_name }}
          fi

  deploy-staging:
    needs: build
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Staging
        run: |
          helm upgrade canvas-app ./canvas-helm \
            --namespace canvas-staging \
            --set image.tag=${{ github.sha }} \
            --values ./canvas-helm/values-staging.yaml

  deploy-data-platform:
    needs: build
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy Data Platform to Staging
        run: |
          helm upgrade canvas-data ./canvas-helm/charts/kafka \
            --namespace canvas-staging
          helm upgrade canvas-flink ./canvas-helm \
            --namespace canvas-staging \
            --values ./canvas-helm/values-staging.yaml \
            --set-file flink.enabled=true
          helm upgrade canvas-clickhouse ./canvas-helm/charts/clickhouse-operator \
            --namespace canvas-staging
          helm upgrade canvas-minio ./canvas-helm \
            --namespace canvas-staging \
            --values ./canvas-helm/values-staging.yaml

  deploy-prod:
    needs: build
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to Production
        run: |
          helm upgrade canvas-app ./canvas-helm \
            --namespace canvas-prod \
            --set image.tag=${{ github.ref_name }} \
            --values ./canvas-helm/values-prod.yaml
      - name: Deploy Data Platform to Production
        run: |
          helm upgrade canvas-data ./canvas-helm/charts/kafka \
            --namespace canvas-prod
          helm upgrade canvas-flink ./canvas-helm \
            --namespace canvas-prod \
            --values ./canvas-helm/values-prod.yaml
          helm upgrade canvas-clickhouse ./canvas-helm/charts/clickhouse-operator \
            --namespace canvas-prod
          helm upgrade canvas-minio ./canvas-helm \
            --namespace canvas-prod \
            --values ./canvas-helm/values-prod.yaml
```

### 7.2 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 多阶段构建产物
COPY backend/canvas-app/target/canvas-app.jar app.jar
COPY frontend/dist/ /app/static/

# 健康检查
HEALTHCHECK --interval=15s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 八、可观测性

### 8.1 Prometheus配置

```yaml
# templates/prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: canvas-alerts
spec:
  groups:
    - name: canvas.rules
      rules:
        - alert: CanvasHighErrorRate
          expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Canvas错误率超过5%"

        - alert: CanvasExecutionTimeout
          expr: rate(canvas_execution_total{outcome="TIMEOUT"}[5m]) > 0
          for: 2m
          labels:
            severity: warning
          annotations:
            summary: "画布执行超时"

        - alert: CanvasHighMemoryUsage
          expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "JVM堆内存使用超过85%"

        - alert: RedisConnectionPoolExhausted
          expr: lettuce_connection_pool_active / lettuce_connection_pool_max > 0.9
          for: 3m
          labels:
            severity: critical
          annotations:
            summary: "Redis连接池接近耗尽"

        # 数据平台告警
        - alert: KafkaUnderReplicatedPartitions
          expr: kafka_server_replicamanager_underreplicatedpartitions > 0
          for: 2m
          labels:
            severity: critical
          annotations:
            summary: "Kafka存在副本同步不足的分区"

        - alert: KafkaConsumerGroupLag
          expr: kafka_consumergroup_lag{group=~"canvas-.*"} > 10000
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Kafka消费组延迟超过10000条"

        - alert: FlinkCheckpointDuration
          expr: flink_jobmanager_job_lastCheckpointDuration > 60000
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Flink Checkpoint耗时超过60秒"

        - alert: FlinkBackpressure
          expr: flink_taskmanager_job_task_backPressuredTimeMsPerSecond > 60000
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Flink任务持续背压超过1分钟"

        - alert: FlinkCheckpointFailure
          expr: rate(flink_jobmanager_job_numberOfFailedCheckpoints[5m]) > 0
          for: 2m
          labels:
            severity: critical
          annotations:
            summary: "Flink Checkpoint连续失败"

        - alert: ClickHouseQueryLatencyP99
          expr: histogram_quantile(0.99, rate(clickhouse_query_duration_seconds_bucket[5m])) > 10
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "ClickHouse P99查询延迟超过10秒"

        - alert: ClickHouseDiskUsage
          expr: clickhouse_disk_free_bytes / clickhouse_disk_total_bytes < 0.15
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "ClickHouse磁盘剩余空间不足15%"

        - alert: MinIODiskUsage
          expr: minio_disk_free_bytes / minio_disk_total_bytes < 0.15
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "MinIO磁盘剩余空间不足15%"

        - alert: MinIONodeDown
          expr: minio_nodes_offline > 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "MinIO节点离线"
```

### 8.2 Grafana Dashboard

| 面板 | 指标 | 告警阈值 |
|------|------|---------|
| HTTP请求QPS | http_server_requests_seconds_count | - |
| HTTP延迟P99 | http_server_requests_seconds{quantile="0.99"} | >2s |
| 画布执行次数 | canvas_execution_total | - |
| 画布执行耗时 | canvas_execution_duration_seconds | >600s |
| DAG节点失败率 | canvas_node_result_total{outcome="FAIL"} | >5% |
| 数据库连接池 | hikaricp_connections_active | >80% |
| Redis延迟 | lettuce_command_latency | >50ms |
| Kafka消费延迟 | kafka_consumergroup_lag | >10000 |
| Flink Checkpoint耗时 | flink_jobmanager_job_lastCheckpointDuration | >60s |
| ClickHouse查询P99 | clickhouse_query_duration_seconds | >10s |
| MinIO磁盘使用率 | minio_disk_used / minio_disk_total | >85% |

---

## 九、安全配置

### 9.1 NetworkPolicy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: canvas-network-policy
spec:
  podSelector:
    matchLabels:
      app: canvas-app
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
      ports:
        - port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: redis
      ports:
        - port: 6379
    - to:
        - podSelector:
            matchLabels:
              app: mysql
      ports:
        - port: 3306
    - to:
        - podSelector:
            matchLabels:
              app: rocketmq
      ports:
        - port: 9876
    - to:
        - podSelector:
            matchLabels:
              app: kafka
      ports:
        - port: 9092
    - to:
        - podSelector:
            matchLabels:
              app: clickhouse
      ports:
        - port: 8123
          protocol: TCP
        - port: 9000
          protocol: TCP
    - to:
        - podSelector:
            matchLabels:
              app: minio
      ports:
        - port: 9000
          protocol: TCP
    - to:
        - podSelector:
            matchLabels:
              app: zookeeper
      ports:
        - port: 2181
          protocol: TCP
```

### 9.2 Secret管理

```bash
# 创建Secret
kubectl create secret generic canvas-secrets \
  --from-literal=DB_CANVAS_PASSWORD=xxx \
  --from-literal=DB_CDP_PASSWORD=xxx \
  --from-literal=DB_META_PASSWORD=xxx \
  --from-literal=REDIS_PASSWORD=xxx \
  --from-literal=JWT_SECRET=xxx \
  --from-literal=WECOM_CORP_ID=xxx \
  --from-literal=WECOM_SECRET=xxx \
  --from-literal=WECOM_TOKEN=xxx \
  --from-literal=WECOM_ENCODING_AES_KEY=xxx \
  -n canvas-prod
```

---

## 十、实施步骤

| 步骤 | 内容 | 时间 |
|------|------|------|
| 1 | 创建Helm Chart骨架 | 4h |
| 2 | 编写Dockerfile + 构建脚本 | 4h |
| 3 | 配置GitHub Actions CI/CD | 8h |
| 4 | 部署开发环境(minikube) | 8h |
| 5 | Redis HA配置 | 4h |
| 6 | MySQL多库初始化 | 4h |
| 7 | RocketMQ集群部署 | 8h |
| 8 | Prometheus + Grafana部署 | 8h |
| 9 | 告警规则配置 | 4h |
| 10 | 生产环境部署验证 | 8h |
| 11 | Kafka集群部署 (Strimzi Operator) | 8h |
| 12 | MinIO对象存储集群部署 | 4h |
| 13 | ClickHouse集群部署 (2分片+ZK) | 8h |
| 14 | Flink集群部署 (JM+TM) | 8h |
| 15 | Spark集群部署 | 8h |
| 16 | DataHub元数据平台部署 | 8h |
| 17 | DolphinScheduler调度平台部署 | 4h |
| 18 | 数据平台组件可观测性接入 | 4h |
| 19 | NetworkPolicy数据端口放通 | 2h |
| **合计** | | **110h** |

---

## 十一、相关文档

- [目标架构总览](target-architecture-overview.md)
- [数据平台架构设计](data-platform-architecture.md)
- [企微SCRM模块设计](wecom-scrm-module-design.md)
- [WebFlux→Spring MVC迁移](webflux-to-mvc-migration.md)
- [多数据源隔离方案](multi-datasource-isolation.md)
