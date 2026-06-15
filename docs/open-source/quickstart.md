# Quickstart

This guide starts Marketing Canvas for local development with Docker-backed
dependencies, the Spring Boot backend, and the Vite frontend.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- Docker 24+

## 1. Start Local Infrastructure

From the repository root:

```bash
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

Local services:

| Service | Port | Purpose |
| --- | --- | --- |
| MySQL 8.0 | `3306` | `canvas_db` database |
| Redis 7 | `6379` | cache, route table, context persistence |
| WireMock | `8099` | mock external providers |
| RocketMQ Namesrv | `9876` | MQ trigger name server |
| RocketMQ Broker | `10909`, `10911`, `10912` | local broker |

RocketMQ is required for the current backend startup path. If MySQL, Redis, and
WireMock are running but RocketMQ is not, MQ trigger startup can fail.

For the lighter open-source demo shell, use the demo compose file instead:

```bash
docker compose -f docker-compose.demo.yml up -d
docker compose -f docker-compose.demo.yml ps
```

The demo compose starts MySQL, Redis, WireMock, and RocketMQ only. It does not
start the backend or frontend containers; run those from source in separate
terminals with the commands below. See [playground.md](playground.md) for the
mock catalog and golden-path guide.

Do not run `docker-compose.local.yml` and `docker-compose.demo.yml` at the same
time unless you change ports; both expose the same local service ports.

## 2. Start The Backend

Open a second terminal:

```bash
cd backend
CANVAS_JWT_SECRET=local-dev-jwt-secret-at-least-32-bytes \
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run
```

Backend URLs:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Prometheus metrics: http://localhost:8080/actuator/prometheus

Flyway runs automatically on first startup and seeds the initial local schema
and default user.

If Flyway reports duplicate migrations, clean stale build output:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-boot/pom.xml clean
```

## 3. Start The Frontend

Open a third terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000.

Default local account:

- username: `admin`
- password: `Admin@123`

## 4. Verify The Stack

Run these checks after the app starts:

```bash
curl http://localhost:8080/actuator/health
(cd frontend && npm run test)
node tools/open-source-growth/guardrail-verifier.mjs
```

## Common Development Commands

Backend:

```bash
cd backend
mvn clean install -DskipTests
mvn clean install
mvn -pl canvas-boot -am -Dtest=ModularArchitectureTest test
```

Frontend:

```bash
cd frontend
npm run build
npm run test
npm run test:watch
```

## Current Demo Limits

The current public quickstart and demo compose are local development paths, not
production plugin runtime claims. Template docs, the Playground mock catalog,
and MarketingOps as Code examples can be reviewed now, but backend public
extension/API write operations remain blocked until the G10 stability gate
passes.

Use WireMock and local Docker services for development integrations unless a
task explicitly requires a real external system. Never put real SMS, email,
coupon, approval, AI, or customer credentials into the repository.

## Next Steps

- Read [positioning.md](positioning.md) to understand the product scope.
- Run the [playground.md](playground.md) mock catalog for the demo golden path.
- Browse [templates/README.md](templates/README.md) for official journey
  examples.
- Review [marketingops-as-code.md](marketingops-as-code.md) for local DSL
  validation and diff workflows.
