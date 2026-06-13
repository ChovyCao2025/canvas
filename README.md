# Marketing Canvas

Open source marketing automation for building customer journeys with a visual
canvas, plugin-backed nodes, reusable templates, MarketingOps as Code, and
AI-assisted campaign operations.

Marketing Canvas is for growth, lifecycle, and marketing operations teams that
need a practical way to design journeys, review risk, dry-run execution, and
connect channel/provider integrations without turning every campaign change into
a custom engineering project.

> Screenshot/GIF placeholder: add the first demo walkthrough here after the
> public demo profile and sample journeys pass the Month 1 gate.

## Quick Start

```bash
docker compose -f docker-compose.local.yml up -d

cd backend
CANVAS_JWT_SECRET=local-dev-jwt-secret-at-least-32-bytes \
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
mvn -f canvas-engine/pom.xml -Dmaven.test.skip=true spring-boot:run

cd ../frontend
npm install
npm run dev
```

Open http://localhost:3000 and sign in with the local seed account:

- username: `admin`
- password: `Admin@123`

The Vite frontend runs on `:3000` and proxies API requests to the backend on
`:8080`. Backend dependencies come from `docker-compose.local.yml`: MySQL,
Redis, RocketMQ, and WireMock.

For the full setup, troubleshooting, and build commands, see
[docs/open-source/quickstart.md](docs/open-source/quickstart.md).

## What You Can Build

- Visual customer journeys with DAG-style nodes and edges.
- Template-based journeys for welcome, winback, coupon approval, AI copy review,
  lead assignment, retention, experiments, and risk-blocked outreach.
- Plugin-backed node capabilities for messaging, coupons, approvals, risk,
  AI, webhooks, and provider-style integrations.
- Local mock integrations through WireMock, so demo and development workflows do
  not require real SMS, email, coupon, approval, or model credentials.
- MarketingOps as Code artifacts for reviewing canvas definitions outside the
  UI before backend import/export APIs are stabilized.

## Current Readiness

Marketing Canvas is being opened in phases. The repository already contains the
current Spring Boot application, React canvas UI, local infrastructure, and
open-source growth contracts. Public extension/API write operations are still
behind the G10 stability gate, so documentation and examples should not claim
production plugin runtime readiness yet.

Use these entry points:

- [Positioning](docs/open-source/positioning.md): audience, non-goals, and
  current open-source narrative.
- [Quickstart](docs/open-source/quickstart.md): local development setup.
- [Template catalog](docs/open-source/templates/README.md): official template
  sidecar docs and sample journeys.
- [MarketingOps as Code](docs/open-source/marketingops-as-code.md): local DSL
  validation and diff workflow while backend write APIs remain gated.
- [Plugin manifest contract](docs/open-source-growth/contracts/plugin-manifest-v1.md):
  plugin metadata, permission, and readiness rules.
- [Template pack contract](docs/open-source-growth/contracts/template-pack-v1.md):
  template pack structure and import expectations.
- [Open Source Growth plan](docs/open-source-growth/open-source-growth-plan.md):
  phased roadmap and gate sequencing.

## Architecture At A Glance

```text
frontend/                 React 18, Vite, Ant Design, React Flow
backend/canvas-engine/    current Spring Boot application
backend/canvas-cache-sdk/ reusable tiered cache module
docs/open-source/         public-facing docs, templates, and quickstart
docs/open-source-growth/  contracts, guardrails, traceability, phase gates
wiremock/                 local mock external APIs
```

Backend code uses Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, and
AssertJ. Frontend code uses TypeScript, React, Vite, Vitest, and Ant Design.

## Development Commands

Backend:

```bash
cd backend
mvn clean install -DskipTests
mvn clean install
mvn -f canvas-engine/pom.xml spring-boot:run
mvn test -pl canvas-engine -Dtest=StartHandlerTest
```

Frontend:

```bash
cd frontend
npm install
npm run dev
npm run build
npm run test
```

Open-source guardrails:

```bash
node tools/open-source-growth/guardrail-verifier.mjs
```

## Contributing

Start with [CONTRIBUTING.md](CONTRIBUTING.md). Good first contributions are
docs improvements, reproducible bug reports, template examples, local mock
coverage, and contract-aligned plugin or DSL examples. Backend public extension
changes must follow the current phase gates and must not bypass the G10
stability gate.

Please also read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) and
[SECURITY.md](SECURITY.md) before opening community or security-sensitive work.
