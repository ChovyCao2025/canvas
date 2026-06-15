# Quickstart

Use the current local development quickstart in
[../quickstart.md](../quickstart.md). This English page is a public-facing
summary and does not replace the root README or the canonical local command
details.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- Docker 24+

## Local Startup Path

From the repository root, start local dependencies:

```bash
docker compose -f docker-compose.local.yml up -d
```

Start the backend:

```bash
cd backend
CANVAS_JWT_SECRET=local-dev-jwt-secret-at-least-32-bytes \
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000.

Default local account:

- username: `admin`
- password: `Admin@123`

## Verify The Stack

```bash
curl http://localhost:8080/actuator/health
cd frontend && npm run test
node tools/open-source-growth/guardrail-verifier.mjs
```

## Current Limits

- RocketMQ is part of the current backend startup path.
- The current quickstart is a local development path, not a finalized public
  one-command demo.
- Template docs and local DSL validation can be reviewed now.
- Backend public extension/API write operations remain blocked until G10.
- Use WireMock and local mock providers for external integrations during local
  testing.

## Next Steps

- Browse the [template catalog](../templates/README.md).
- Review [MarketingOps as Code](../marketingops-as-code.md).
- Read the [ecosystem guide](ecosystem.md) before describing plugin, DSL, CLI,
  or AI readiness publicly.
