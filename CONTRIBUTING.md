# Contributing To Marketing Canvas

Thanks for helping improve Marketing Canvas. This project is moving toward an
open-source marketing automation platform through phased docs, demo, plugin,
template, DSL, CLI, and AI work. Contributions should stay aligned with the
current phase gates.

## Before You Start

- Read [docs/open-source/positioning.md](docs/open-source/positioning.md) for
  the product scope and non-goals.
- Read [docs/open-source/quickstart.md](docs/open-source/quickstart.md) and
  make sure the local stack can run.
- For open-source growth work, check
  [docs/open-source-growth/phase-gates.md](docs/open-source-growth/phase-gates.md)
  and
  [docs/open-source-growth/implementation-guardrails.md](docs/open-source-growth/implementation-guardrails.md).
- For plugin, template, DSL, or AI surface changes, check the matching contract
  under [docs/open-source-growth/contracts](docs/open-source-growth/contracts/README.md).

## Development Setup

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

Use Java 21, Maven 3.9+, Node.js 18+, and Docker 24+.

## Branch And PR Workflow

- Keep each branch focused on one logical change.
- Use concise Conventional Commit subjects such as `feat:`, `fix:`, `docs:`,
  `test:`, or `refactor:`.
- Include the relevant OSG requirement or issue in your PR description.
- Include screenshots for visible UI changes.
- Do not commit `target/`, `dist/`, `node_modules/`, local secrets, or generated
  build outputs.

## Scope And Guardrails

- Do not modify applied Flyway migrations. Add a new migration when schema
  changes are required.
- Do not put demo mock configuration into production or staging profiles.
- Do not add real provider credentials, model keys, or customer data.
- Do not introduce a second plugin registry or runtime jar loading.
- Public extension/API write operations remain gated by G10 until the contracts,
  ownership split, and tests are stable.

## Test Commands

Backend:

```bash
cd backend
mvn clean install
mvn test -pl canvas-engine -Dtest=StartHandlerTest
```

Frontend:

```bash
cd frontend
npm run build
npm run test
```

Open-source guardrails:

```bash
node tools/open-source-growth/guardrail-verifier.mjs
```

Run the narrowest useful tests for your change, then include the commands and
results in the PR.

## Contribution Ideas

- Improve quickstart troubleshooting for a clean machine.
- Add or refine official template sidecar docs.
- Add reproducible WireMock examples for local integrations.
- Improve contract examples for plugin manifests, template packs, or DSL.
- Add focused tests around graph helpers, config panels, services, or hooks.
