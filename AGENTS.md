# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Marketing Canvas platform with a Java backend and Vite frontend. Backend code lives in `backend/`: `canvas-engine` is the Spring Boot application, and `canvas-cache-sdk` is the reusable tiered cache module. Java sources follow `src/main/java`, tests follow `src/test/java`, and Flyway migrations live in `backend/canvas-engine/src/main/resources/db/migration/`. Frontend code lives in `frontend/src`, organized by `pages`, `components`, `hooks`, `services`, `types`, and `auth`. Supporting docs are under `docs/`, local dependency config is in `docker-compose.local.yml`, and mock external APIs are in `wiremock/`.

## Build, Test, and Development Commands

Run local infrastructure from the repository root:

```bash
docker compose -f docker-compose.local.yml up -d
```

Backend commands:

```bash
cd backend
mvn clean install -DskipTests
mvn clean install
mvn -f canvas-engine/pom.xml spring-boot:run
mvn test -pl canvas-engine -Dtest=StartHandlerTest
```

Frontend commands:

```bash
cd frontend
npm install
npm run dev
npm run build
npm run test
npm run test:watch
```

Use Java 21, Maven 3.9+, Node.js 18+, and Docker 24+. The Vite dev server runs on `:3000` and proxies API routes to the backend on `:8080`.

## Coding Style & Naming Conventions

Java uses 4-space indentation, package names under `org.chovy`, PascalCase classes, camelCase methods, and JUnit test classes ending in `Test`. Add new node behavior through the `NodeHandler` pattern and `@NodeHandlerType` registration. Do not edit applied Flyway migrations; add a new migration instead. TypeScript uses 2-space indentation, ES modules, semicolon-free style, PascalCase React components, and camelCase helpers. Keep frontend tests beside the code they cover as `*.test.ts`.

## Testing Guidelines

Backend tests use JUnit 5 with AssertJ. Run module or targeted tests before changing engine, cache, persistence, or controller behavior. Frontend tests use Vitest in the Node environment. Prefer focused unit tests for graph helpers, config panels, services, and hooks. When changing migrations, include schema or migration verification tests where practical.

## Commit & Pull Request Guidelines

Recent commits use concise Conventional Commit prefixes such as `feat:`, `fix:`, `docs:`, and `refactor:`. Keep subjects imperative and scoped to one logical change. Pull requests should include a short problem statement, implementation summary, test commands run, linked issue or spec when available, and screenshots for visible UI changes.

## Security & Configuration Tips

Set `CANVAS_JWT_SECRET` locally; it must be at least 32 bytes. Keep production secrets, generated build outputs, `target/`, `dist/`, and `node_modules/` out of commits. Use WireMock and local Docker services for development integrations unless a task explicitly requires real external systems.
