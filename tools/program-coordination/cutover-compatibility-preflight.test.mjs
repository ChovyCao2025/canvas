import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtemp, mkdir, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const toolPath = path.join(__dirname, 'cutover-compatibility-preflight.mjs')

const compatibilityTests = [
  'CanvasApiCompatibilityTest',
  'ExecutionApiCompatibilityTest',
  'MarketingApiCompatibilityTest',
  'CdpApiCompatibilityTest',
  'BiApiCompatibilityTest',
  'RiskApiCompatibilityTest',
  'ConversationApiCompatibilityTest'
]

test('reports ready when canvas-web matches old controllers and required compatibility tests exist', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/OldCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")',
      '@PostMapping'
    ]
  })
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicWebhookController.java', {
    classMapping: '{"/public/webhooks", "/public/legacy-webhooks"}',
    methods: [
      '@RequestMapping(value = "/{tenantId}", method = {RequestMethod.GET, RequestMethod.POST})'
    ]
  })

  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/OldCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")',
      '@PostMapping'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/PublicWebhookController.java', {
    classMapping: '{"/public/webhooks", "/public/legacy-webhooks"}',
    methods: [
      '@RequestMapping(value = "/{tenantId}", method = {RequestMethod.GET, RequestMethod.POST})'
    ]
  })
  await writeCompatibilityTests(root)

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  assert.equal(result.stderr, '')
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, true)
  assert.deepEqual(report.oldCanvasEngineWeb, {
    path: 'backend/canvas-engine/src/main/java/org/chovy/canvas/web',
    present: true,
    controllerCount: 2,
    endpointCount: 6
  })
  assert.deepEqual(report.currentCanvasWeb.controllers, {
    path: 'backend/canvas-web/src/main/java',
    present: true,
    controllerCount: 2,
    endpointCount: 6
  })
  assert.equal(report.currentCanvasWeb.compatibilityTests.present, true)
  assert.equal(report.currentCanvasWeb.compatibilityTests.presentCount, 7)
  assert.equal(report.currentCanvasWeb.compatibilityTests.missingCount, 0)
  assert.equal(report.currentCanvasWeb.compatibilityTests.required.every((entry) => entry.present), true)
  assert.deepEqual(report.currentCanvasBoot, {
    pom: {
      path: 'backend/canvas-boot/pom.xml',
      present: true,
      dependsOnCanvasWeb: true,
      dependsOnCanvasEngine: false
    },
    application: {
      path: 'backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasBootApplication.java',
      present: true,
      scanBasePackages: ['org.chovy.canvas'],
      scansOrgChovyCanvas: true
    }
  })
  assert.deepEqual(report.runCommandCutover, {
    requiredFiles: [
      {
        file: 'README.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'CONTRIBUTING.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'AGENTS.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'CLAUDE.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'docs/open-source/quickstart.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'docs/open-source/en/quickstart.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'docs/canvas-examples/STARTUP.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      },
      {
        file: 'docs/code-review/runtime-verification.md',
        present: true,
        startsCanvasBoot: true,
        startsCanvasEngine: false,
        usesLegacyCanvasEngineMavenModule: false,
        referencesLegacyEngineMigrationPath: false,
        referencesCanvasBootMigrationPath: false
      }
    ]
  })
  assert.deepEqual(report.packagedRuntimeCutover, {
    requiredFiles: [
      {
        file: '.dockerignore',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: '.github/workflows/ci.yml',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false,
        usesLegacyCanvasEngineMavenModule: false,
        hasBootRuntimeTestGate: true,
        hasHelmRenderGate: true,
        hasBootFlywayMigrationPolicyGate: true,
        hasProgramCoordinationGate: true
      },
      {
        file: '.github/workflows/canvas-ci.yml',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false,
        usesLegacyCanvasEngineMavenModule: false,
        hasHelmRenderGate: true,
        hasBootFlywayMigrationPolicyGate: true,
        hasProgramCoordinationGate: true
      },
      {
        file: 'deploy/helm/canvas/values.yaml',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'docs/architecture/evidence/runbooks/release-deployment.md',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'backend/canvas-boot/Dockerfile.perf',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'scripts/release/build-image.sh',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'scripts/release/check-flyway-migration.sh',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'scripts/release/validate-production-profile.sh',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'scripts/release/verify-helm-render.sh',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      },
      {
        file: 'scripts/verify-flink-realtime-warehouse-live.sh',
        present: true,
        usesCanvasBootArtifact: true,
        usesCanvasEngineArtifact: false
      }
    ]
  })
  assert.deepEqual(report.serviceNameCompatibility, {
    helmValues: {
      path: 'deploy/helm/canvas/values.yaml',
      present: true,
      backendName: 'canvas-engine',
      imageRepository: 'registry.example.com/marketing-canvas/canvas-boot',
      serviceAccountName: 'canvas-engine',
      secretName: 'canvas-engine-runtime',
      keepsCanvasEngineBackendName: true,
      usesCanvasBootImageRepository: true,
      keepsCanvasEngineServiceAccount: true,
      keepsCanvasEngineRuntimeSecret: true
    }
  })
  assert.deepEqual(report.productionPreflightCutover, {
    requiredFiles: [
      {
        file: 'scripts/verify-flink-production-deployment.sh',
        present: true,
        anchorsLegacyEngineSource: false,
        checksHelmPipelineContract: true
      }
    ]
  })
  assert.deepEqual(report.activeRuntimeRunbookCutover, {
    requiredFiles: [
      {
        file: 'docs/stressTest/local-capacity-runbook.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      },
      {
        file: 'docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      },
      {
        file: 'docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      },
      {
        file: 'docs/runbooks/marketing-content-production-readiness.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      },
      {
        file: 'docs/runbooks/risk-control-rule-engine.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      },
      {
        file: 'docs/runbooks/flyway-migration-history-repair-2026-06-06.md',
        present: true,
        usesLegacyCanvasEngineMavenModule: false,
        usesLegacyCanvasEnginePerfImage: false,
        usesCanvasBootRuntime: true
      }
    ]
  })
  assert.deepEqual(report.blockers, [])
})

test('default JSON mode exits zero and reports blockers when cutover is not ready', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/OldCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")',
      '@PostMapping'
    ]
  })
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicWebhookController.java', {
    classMapping: '{"/public/webhooks", "/public/legacy-webhooks"}',
    methods: [
      '@RequestMapping(value = "/{tenantId}", method = {RequestMethod.GET, RequestMethod.POST})'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/OldCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeCompatibilityTests(root, ['CanvasApiCompatibilityTest'])

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.equal(report.oldCanvasEngineWeb.controllerCount, 2)
  assert.equal(report.oldCanvasEngineWeb.endpointCount, 6)
  assert.equal(report.currentCanvasWeb.controllers.controllerCount, 1)
  assert.equal(report.currentCanvasWeb.controllers.endpointCount, 1)
  assert.equal(report.currentCanvasWeb.compatibilityTests.presentCount, 1)
  assert.equal(report.currentCanvasWeb.compatibilityTests.missingCount, 6)
  assert.deepEqual(report.blockers, [
    'route gap candidates remain: 2',
    'canvas-web endpoint count 1 is below old canvas-engine web endpoint count 6',
    'missing required compatibility tests: ExecutionApiCompatibilityTest, MarketingApiCompatibilityTest, CdpApiCompatibilityTest, BiApiCompatibilityTest, RiskApiCompatibilityTest, ConversationApiCompatibilityTest'
  ])
})

test('reports bounded route gap candidates when compatibility tests are seeded', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminUserController.java', {
    classMapping: '"/admin/users"',
    methods: [
      '@GetMapping',
      '@PostMapping'
    ]
  })
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminRoleController.java', {
    classMapping: '"/admin/roles"',
    methods: [
      '@GetMapping'
    ]
  })
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/ReportingController.java', {
    classMapping: '"/reporting"',
    methods: [
      '@GetMapping'
    ]
  })

  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/ReportingController.java', {
    classMapping: '"/reporting"',
    methods: [
      '@GetMapping'
    ]
  })
  await writeCompatibilityTests(root)

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.deepEqual(report.routeGapSummary, {
    candidateLimit: 10,
    candidateCount: 1,
    reportedCandidateCount: 1,
    candidates: [
      {
        group: 'route:/admin',
        oldControllerCount: 2,
        oldEndpointCount: 3,
        currentControllerCount: 0,
        currentEndpointCount: 0,
        representativeOldControllerFiles: [
          'backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminRoleController.java',
          'backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminUserController.java'
        ]
      }
    ]
  })
})

test('does not report a family route gap when split final controllers cover old family endpoints', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")',
      '@PutMapping("/{id}")',
      '@GetMapping("/{id}/project-folder-metadata")',
      '@PutMapping("/{id}/project-folder-metadata")',
      ...Array.from({ length: 22 }, (_, index) => `@GetMapping("/route-${index}")`)
    ]
  })

  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")',
      '@PutMapping("/{id}")',
      ...Array.from({ length: 22 }, (_, index) => `@GetMapping("/route-${index}")`)
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasProjectFolderMetadataController.java', {
    classMapping: '"/canvas/{id}/project-folder-metadata"',
    methods: [
      '@GetMapping',
      '@PutMapping'
    ]
  })
  await writeCompatibilityTests(root)

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, true)
  assert.deepEqual(report.routeGapSummary, {
    candidateLimit: 10,
    candidateCount: 0,
    reportedCandidateCount: 0,
    candidates: []
  })
})

test('does not report an ops family gap when final class-level prefix covers old full-path mappings', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java', {
    classMapping: null,
    methods: [
      '@PostMapping("/ops/cache/invalidate/{id}")',
      '@PostMapping("/ops/recovery/runtime-state/rebuild")',
      '@GetMapping("/ops/runtime/status")',
      '@GetMapping("/ops/audit-events")'
    ]
  })

  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java', {
    classMapping: '"/ops"',
    methods: [
      '@PostMapping("/cache/invalidate/{canvasId}")',
      '@PostMapping("/recovery/runtime-state/rebuild")',
      '@GetMapping("/runtime/status")',
      '@GetMapping("/audit-events")'
    ]
  })
  await writeCompatibilityTests(root)

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.deepEqual(report.routeGapSummary, {
    candidateLimit: 10,
    candidateCount: 0,
    reportedCandidateCount: 0,
    candidates: []
  })
})

test('does not block cutover on raw controller count when final controllers cover every old endpoint', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasBatchController.java', {
    classMapping: '"/canvas/batch"',
    methods: [
      '@PostMapping'
    ]
  })

  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")',
      '@PostMapping("/batch")'
    ]
  })
  await writeCompatibilityTests(root)

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.oldCanvasEngineWeb.controllerCount, 2)
  assert.equal(report.currentCanvasWeb.controllers.controllerCount, 1)
  assert.equal(report.routeGapSummary.candidateCount, 0)
  assert.deepEqual(report.blockers, [])
  assert.equal(report.cutoverReady, true)
})

test('blocks service compatibility cutover when Helm stable names are mechanically renamed to canvas-boot', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writePackagedRuntimeFiles(root, {
    helmValuesContent: `backend:
  name: canvas-boot
  image:
    repository: registry.example.com/marketing-canvas/canvas-boot
  serviceAccountName: canvas-boot
  secretName: canvas-boot-runtime
`
  })

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'Helm backend.name must remain canvas-engine until service/DNS compatibility cutover',
    'Helm serviceAccountName must remain canvas-engine until RBAC compatibility cutover',
    'Helm runtime secret name must remain canvas-engine-runtime until secret compatibility cutover'
  ])
})

test('blocks cutover when active developer command docs still point tests or migrations at legacy engine', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, 'CLAUDE.md'), [
    'mvn -f canvas-boot/pom.xml spring-boot:run',
    'mvn test -pl canvas-engine -Dtest=ClassName',
    'backend/canvas-engine/src/main/resources/db/migration'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'run-command cutover file still uses legacy canvas-engine Maven module: CLAUDE.md',
    'run-command cutover file still documents legacy canvas-engine migration path: CLAUDE.md'
  ])
})

test('blocks cutover when CI stops rendering Helm chart variants', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, '.github/workflows/canvas-ci.yml'), [
    'CANVAS_IMAGE_TAG=${{ github.sha }} bash scripts/release/build-image.sh --image canvas-boot',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -pl canvas-boot -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'CI workflow does not run Helm render gate: .github/workflows/canvas-ci.yml',
    'CI workflow does not run DDD cutover preflight gate: .github/workflows/canvas-ci.yml'
  ])
})

test('blocks cutover when CI does not enforce program coordination gates', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, '.github/workflows/canvas-ci.yml'), [
    'CANVAS_IMAGE_TAG=${{ github.sha }} bash scripts/release/build-image.sh --image canvas-boot',
    'uses: azure/setup-helm@v4',
    'bash scripts/release/verify-helm-render.sh',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -pl canvas-boot -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'CI workflow does not run DDD cutover preflight gate: .github/workflows/canvas-ci.yml'
  ])
})

test('blocks cutover when CI keeps Flyway migration policy on legacy engine module', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, '.github/workflows/ci.yml'), [
    'CANVAS_IMAGE_TAG=${{ github.sha }} bash scripts/release/build-image.sh --image canvas-boot',
    'mvn -f backend/pom.xml -pl canvas-boot -am -Dtest=ModularArchitectureTest,CanvasBootApplicationSmokeTest test',
    'uses: azure/setup-helm@v4',
    'bash scripts/release/verify-helm-render.sh',
    'node tools/program-coordination/check-dispatch-state.mjs .',
    'node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs',
    'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'CI workflow still runs legacy canvas-engine Maven module: .github/workflows/ci.yml',
    'CI workflow does not run canvas-boot Flyway migration policy gate: .github/workflows/ci.yml'
  ])
})

test('blocks cutover when CI backend tests run the legacy engine module', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, '.github/workflows/ci.yml'), [
    'mvn -f backend/pom.xml -pl canvas-engine -am test',
    'mvn -f backend/pom.xml -pl canvas-boot -am -Dtest=ModularArchitectureTest,CanvasBootApplicationSmokeTest test',
    'uses: azure/setup-helm@v4',
    'bash scripts/release/verify-helm-render.sh',
    'node tools/program-coordination/check-dispatch-state.mjs .',
    'node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs',
    'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -f backend/pom.xml -pl canvas-boot -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'CI workflow still runs legacy canvas-engine Maven module: .github/workflows/ci.yml'
  ])
})

test('blocks production preflight cutover when deployment checks still anchor legacy engine source', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, 'scripts/verify-flink-production-deployment.sh'), [
    'service="backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessService.java"',
    'deploy/helm/canvas/values.yaml',
    'deploy/k8s/canvas-flink-job-submitter.yaml',
    'docs/runbooks/flink-production-deployment.md',
    '--pipeline-key=${pipeline}'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'production preflight still anchors legacy canvas-engine source: scripts/verify-flink-production-deployment.sh'
  ])
})

test('blocks cutover when active runtime runbooks still point operators at legacy engine runtime', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/LegacyCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{canvasId}")'
    ]
  })
  await writeCompatibilityTests(root)
  await writeFile(path.join(root, 'docs/stressTest/local-capacity-runbook.md'), [
    'mvn -q -pl canvas-engine -am clean package -DskipTests',
    'docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .'
  ].join('\n'))

  const result = runPreflight(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.deepEqual(report.blockers, [
    'active runtime runbook does not reference canvas-boot runtime: docs/stressTest/local-capacity-runbook.md',
    'active runtime runbook still uses legacy canvas-engine Maven module: docs/stressTest/local-capacity-runbook.md',
    'active runtime runbook still uses legacy canvas-engine perf image: docs/stressTest/local-capacity-runbook.md'
  ])
})

test('--require-ready exits one with the same JSON report when blockers remain', async () => {
  const root = await createFixtureRoot()
  await writeController(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/OldCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })

  const defaultResult = runPreflight(root, '--json')
  const requiredResult = runPreflight(root, '--require-ready', '--json')

  assert.equal(defaultResult.status, 0, defaultResult.stderr)
  assert.equal(requiredResult.status, 1)
  assert.deepEqual(JSON.parse(requiredResult.stdout), JSON.parse(defaultResult.stdout))
})

test('reports missing controller source directories as blockers', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'canvas-cutover-preflight-missing-'))
  await writeController(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/CurrentCanvasController.java', {
    classMapping: '"/canvas"',
    methods: [
      '@GetMapping("/{id}")'
    ]
  })
  await mkdir(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'), { recursive: true })
  await writeCompatibilityTests(root)
  await writeBootCutoverFiles(root)
  await writeRunCommandDocs(root)
  await writePackagedRuntimeFiles(root)

  const result = runPreflight(root, '--json')
  const requiredResult = runPreflight(root, '--require-ready', '--json')

  assert.equal(result.status, 0, result.stderr)
  assert.equal(requiredResult.status, 1, requiredResult.stderr)
  assert.deepEqual(JSON.parse(requiredResult.stdout), JSON.parse(result.stdout))
  const report = JSON.parse(result.stdout)
  assert.equal(report.cutoverReady, false)
  assert.equal(report.oldCanvasEngineWeb.present, false)
  assert.equal(report.currentCanvasWeb.controllers.present, true)
  assert.deepEqual(report.blockers, [
    'old canvas-engine web source path is missing: backend/canvas-engine/src/main/java/org/chovy/canvas/web'
  ])
})

async function createFixtureRoot() {
  const root = await mkdtemp(path.join(tmpdir(), 'canvas-cutover-preflight-'))
  await mkdir(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web'), { recursive: true })
  await mkdir(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web'), { recursive: true })
  await mkdir(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'), { recursive: true })
  await writeRunCommandDocs(root)
  await writePackagedRuntimeFiles(root)
  await writeBootCutoverFiles(root)
  return root
}

async function writeController(root, relativePath, { classMapping, methods }) {
  const filePath = path.join(root, relativePath)
  await mkdir(path.dirname(filePath), { recursive: true })
  const classMappingSource = classMapping == null ? '' : `@RequestMapping(${classMapping})\n`
  const methodSource = methods.map((annotation, index) => `
    ${annotation}
    public String endpoint${index}() {
        return "ok";
    }
`).join('\n')
  await writeFile(filePath, `package org.chovy.canvas.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
${classMappingSource}\
public class ${path.basename(relativePath, '.java')} {
${methodSource}
}
`)
}

async function writeCompatibilityTests(root, present = compatibilityTests) {
  await Promise.all(present.map((className) => {
    const filePath = path.join(root, `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/${className}.java`)
    return writeFile(filePath, `package org.chovy.canvas.web.compat;

class ${className} {
}
`)
  }))
}

async function writeBootCutoverFiles(root, {
  dependsOnCanvasWeb = true,
  dependsOnCanvasEngine = false,
  scanBasePackages = '"org.chovy.canvas"'
} = {}) {
  const pomPath = path.join(root, 'backend/canvas-boot/pom.xml')
  const applicationPath = path.join(root, 'backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasBootApplication.java')
  await mkdir(path.dirname(pomPath), { recursive: true })
  await mkdir(path.dirname(applicationPath), { recursive: true })
  await writeFile(pomPath, `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <dependencies>
    ${dependsOnCanvasWeb ? '<dependency><artifactId>canvas-web</artifactId></dependency>' : ''}
    ${dependsOnCanvasEngine ? '<dependency><artifactId>canvas-engine</artifactId></dependency>' : ''}
  </dependencies>
</project>
`)
  await writeFile(applicationPath, `package org.chovy.canvas.boot;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = ${scanBasePackages})
public class CanvasBootApplication {
}
`)
}

async function writeRunCommandDocs(root, {
  rootReadmeCommand = 'mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run',
  contributingCommand = 'mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run',
  agentsCommand = 'mvn -f canvas-boot/pom.xml spring-boot:run',
  claudeCommand = 'mvn -f canvas-boot/pom.xml spring-boot:run',
  quickstartCommand = 'mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run',
  englishQuickstartCommand = 'mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run',
  examplesStartupCommand = 'mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run',
  runtimeVerificationCommand = 'mvn -f backend/pom.xml -pl canvas-boot spring-boot:run'
} = {}) {
  await mkdir(path.join(root, 'docs/open-source/en'), { recursive: true })
  await mkdir(path.join(root, 'docs/canvas-examples'), { recursive: true })
  await mkdir(path.join(root, 'docs/code-review'), { recursive: true })
  await writeFile(path.join(root, 'README.md'), `# Fixture

\`\`\`bash
${rootReadmeCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'CONTRIBUTING.md'), `# Contributing

\`\`\`bash
${contributingCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'AGENTS.md'), `# Agents

\`\`\`bash
${agentsCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'CLAUDE.md'), `# Claude

\`\`\`bash
${claudeCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'docs/open-source/quickstart.md'), `# Quickstart

\`\`\`bash
${quickstartCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'docs/open-source/en/quickstart.md'), `# Quickstart

\`\`\`bash
${englishQuickstartCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'docs/canvas-examples/STARTUP.md'), `# Startup

\`\`\`bash
${examplesStartupCommand}
\`\`\`
`)
  await writeFile(path.join(root, 'docs/code-review/runtime-verification.md'), `# Runtime Verification

\`\`\`bash
${runtimeVerificationCommand}
\`\`\`
`)
}

async function writePackagedRuntimeFiles(root, {
  dockerfileContent = 'COPY backend/canvas-boot/target/canvas-boot-*.jar /app/app.jar',
  buildImageContent = 'mvn -pl canvas-boot -am package -DskipTests\nbackend/canvas-boot/target/canvas-boot-*.jar',
  liveVerifierContent = 'mvn -f canvas-boot/pom.xml package\nbackend/canvas-boot/target/canvas-boot-1.0.0-SNAPSHOT.jar',
  helmValuesContent = `backend:
  name: canvas-engine
  image:
    repository: registry.example.com/marketing-canvas/canvas-boot
  serviceAccountName: canvas-engine
  secretName: canvas-engine-runtime
`
} = {}) {
  await mkdir(path.join(root, 'backend/canvas-boot'), { recursive: true })
  await mkdir(path.join(root, '.github/workflows'), { recursive: true })
  await mkdir(path.join(root, 'deploy/helm/canvas'), { recursive: true })
  await mkdir(path.join(root, 'docs/architecture/evidence/runbooks'), { recursive: true })
  await mkdir(path.join(root, 'docs/stressTest'), { recursive: true })
  await mkdir(path.join(root, 'docs/product-evolution/runbooks'), { recursive: true })
  await mkdir(path.join(root, 'docs/runbooks'), { recursive: true })
  await mkdir(path.join(root, 'scripts/release'), { recursive: true })
  await mkdir(path.join(root, 'scripts'), { recursive: true })
  await writeFile(path.join(root, '.dockerignore'), '!backend/canvas-boot/target/*.jar')
  await writeFile(path.join(root, '.github/workflows/ci.yml'), [
    'CANVAS_IMAGE_TAG=${{ github.sha }} bash scripts/release/build-image.sh --image canvas-boot',
    'mvn -f backend/pom.xml -pl canvas-boot -am -Dtest=ModularArchitectureTest,CanvasBootApplicationSmokeTest test',
    'uses: azure/setup-helm@v4',
    'bash scripts/release/verify-helm-render.sh',
    'node tools/program-coordination/check-dispatch-state.mjs .',
    'node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs',
    'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -f backend/pom.xml -pl canvas-boot -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))
  await writeFile(path.join(root, '.github/workflows/canvas-ci.yml'), [
    'CANVAS_IMAGE_TAG=${{ github.sha }} bash scripts/release/build-image.sh --image canvas-boot',
    'uses: azure/setup-helm@v4',
    'bash scripts/release/verify-helm-render.sh',
    'node tools/program-coordination/check-dispatch-state.mjs .',
    'node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs',
    'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready',
    'bash scripts/release/check-flyway-migration.sh',
    'mvn -pl canvas-boot -am -Dtest=FlywayMigrationPolicyTest test'
  ].join('\n'))
  await writeFile(path.join(root, 'deploy/helm/canvas/values.yaml'), helmValuesContent)
  await writeFile(path.join(root, 'docs/architecture/evidence/runbooks/release-deployment.md'), 'CANVAS_IMAGE_NAME="registry.example.com/canvas-boot"')
  await writeFile(path.join(root, 'backend/canvas-boot/Dockerfile.perf'), dockerfileContent)
  await writeFile(path.join(root, 'scripts/release/build-image.sh'), buildImageContent)
  await writeFile(path.join(root, 'scripts/release/check-flyway-migration.sh'), 'backend/canvas-boot/src/main/resources/db/migration')
  await writeFile(path.join(root, 'scripts/release/validate-production-profile.sh'), 'backend/canvas-boot/src/main/resources/application-prod.yml')
  await writeFile(path.join(root, 'scripts/release/verify-helm-render.sh'), 'registry.example.com/marketing-canvas/canvas-boot\ncanvas-engine-runtime')
  await writeFile(path.join(root, 'scripts/verify-flink-realtime-warehouse-live.sh'), liveVerifierContent)
  await writeFile(path.join(root, 'scripts/verify-flink-production-deployment.sh'), [
    'deploy/helm/canvas/values.yaml',
    'deploy/k8s/canvas-flink-job-submitter.yaml',
    'docs/runbooks/flink-production-deployment.md',
    '--pipeline-key=${pipeline}'
  ].join('\n'))
  await Promise.all([
    'docs/stressTest/local-capacity-runbook.md',
    'docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md',
    'docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md',
    'docs/runbooks/marketing-content-production-readiness.md',
    'docs/runbooks/risk-control-rule-engine.md',
    'docs/runbooks/flyway-migration-history-repair-2026-06-06.md'
  ].map((file) => writeFile(path.join(root, file), 'mvn -f backend/pom.xml -pl canvas-boot -am test\nbackend/canvas-boot/Dockerfile.perf\ncanvas-boot:perf\n')))
}

function runPreflight(root, ...args) {
  return spawnSync(process.execPath, [toolPath, root, ...args], {
    encoding: 'utf8'
  })
}
