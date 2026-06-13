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
    'canvas-web controller count 1 is below old canvas-engine web controller count 2',
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
  return root
}

async function writeController(root, relativePath, { classMapping, methods }) {
  const filePath = path.join(root, relativePath)
  await mkdir(path.dirname(filePath), { recursive: true })
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
@RequestMapping(${classMapping})
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

function runPreflight(root, ...args) {
  return spawnSync(process.execPath, [toolPath, root, ...args], {
    encoding: 'utf8'
  })
}
