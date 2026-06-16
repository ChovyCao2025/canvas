import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { verifyG10PublicApiStability } from './g10-public-api-stability.mjs'

async function fixture() {
  const root = await mkdtemp(path.join(tmpdir(), 'osg-g10-api-'))
  mkdirSync(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'), { recursive: true })
  mkdirSync(path.join(root, 'tools/open-source-growth'), { recursive: true })
  mkdirSync(path.join(root, 'tools/canvas-cli/src'), { recursive: true })

  writeFileSync(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java'), [
    '@RestController',
    '@RequestMapping("/canvas/dsl")',
    'class CanvasDslController {',
    '  @PostMapping("/validate")',
    '  void validate() {}',
    '  @PostMapping("/map")',
    '  void map() {}',
    '  @PostMapping("/import")',
    '  void importDsl() {}',
    '  @GetMapping("/export/{canvasId}")',
    '  void exportDsl() {}',
    '  @PostMapping("/diff")',
    '  void diff() {}',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java'), [
    'class CanvasApiCompatibilityTest {',
    '  void validateRoute() { webClient().post().uri("/canvas/dsl/validate"); }',
    '  void mapRoute() { webClient().post().uri("/canvas/dsl/map"); }',
    '  void importRoute() { webClient().post().uri("/canvas/dsl/import"); }',
    '  void exportRoute() { webClient().get().uri("/canvas/dsl/export/99"); }',
    '  void diffRoute() { webClient().post().uri("/canvas/dsl/diff"); }',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-live-api-smoke.mjs'), [
    'const DSL_MAP_PATH = "/canvas/dsl/map"',
    'function validateMappingResponse(errors, responseJson) {',
    '  if (responseJson.templateKey !== "new-user-welcome") errors.push("templateKey")',
    '  if (typeof responseJson.graphJson !== "string") errors.push("graphJson")',
    '  if (!Array.isArray(responseJson.violations)) errors.push("violations")',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-runtime-smoke.mjs'), 'export function verifyPlaygroundRuntimeSmoke() {}\n')
  writeFileSync(path.join(root, 'tools/canvas-cli/src/index.mjs'), [
    "const PUBLISH_GATED_MESSAGE = 'Publish is gated until a stable backend publish API is verified; import/export preview is available after G10 unlock.'",
    'const usage = `Canvas CLI',
    '  canvas-cli validate <file>',
    '  canvas-cli diff <before> <after>',
    '  canvas-cli import <file> --api-url <url>',
    '  canvas-cli export <canvasId> --api-url <url> --tenant-id <tenantId>',
    'Current boundary:',
    '  import and export are unlocked after G10 public extension/API stability.',
    '  publish remains blocked until a stable backend publish API is verified.`',
    "if (command === 'import') runImport()",
    "if (command === 'export') runExport()",
    'requestJson(apiUrl, "POST", "/canvas/dsl/import", { document })',
    'requestJson(apiUrl, "GET", `/canvas/dsl/export/${canvasId}`, undefined, { "x-tenant-id": tenantId })',
    "if (command === 'publish') console.error(PUBLISH_GATED_MESSAGE)",
  ].join('\n'))

  return root
}

test('accepts current G10 public API stability evidence with CLI import/export unlocked and publish gated', async () => {
  const root = await fixture()

  const result = verifyG10PublicApiStability(root)

  assert.equal(result.ok, true)
  assert.equal(result.status, 'PASS')
  assert.match(result.summary, /Canvas DSL public API stability evidence is present/)
  assert.deepEqual(result.errors, [])
})

test('rejects missing required DSL route and compatibility coverage', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java'), [
    '@RequestMapping("/canvas/dsl")',
    'class CanvasDslController {',
    '  @PostMapping("/validate") void validate() {}',
    '  @PostMapping("/map") void map() {}',
    '  @PostMapping("/import") void importDsl() {}',
    '  @GetMapping("/export/{canvasId}") void exportDsl() {}',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java'), [
    'class CanvasApiCompatibilityTest {',
    '  void validateRoute() { webClient().post().uri("/canvas/dsl/validate"); }',
    '  void mapRoute() { webClient().post().uri("/canvas/dsl/map"); }',
    '  void importRoute() { webClient().post().uri("/canvas/dsl/import"); }',
    '  void exportRoute() { webClient().get().uri("/canvas/dsl/export/99"); }',
    '}',
  ].join('\n'))

  const result = verifyG10PublicApiStability(root)

  assert.equal(result.ok, false)
  assert.equal(result.status, 'FAIL')
  assert.match(result.errors.join('\n'), /CanvasDslController\.java must expose POST \/canvas\/dsl\/diff/)
  assert.match(result.errors.join('\n'), /CanvasApiCompatibilityTest\.java must cover \/canvas\/dsl\/diff/)
})

test('rejects CLI publish unlock before a stable backend publish endpoint is verified', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/src/index.mjs'), [
    'const usage = `Canvas CLI',
    '  canvas-cli import <file>',
    '  canvas-cli export <canvasId>',
    '  canvas-cli publish <canvasId>`',
    'async function runPublish(canvasId) {',
    '  return requestJson(apiUrl, "POST", `/canvas/publish/${canvasId}`)',
    '}',
  ].join('\n'))

  const result = verifyG10PublicApiStability(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /canvas-cli must keep publish gated until a stable backend publish API is verified/)
  assert.match(result.errors.join('\n'), /canvas-cli must not expose canvas-cli publish <canvasId>/)
  assert.match(result.errors.join('\n'), /canvas-cli must not call publish backend path/)
})

test('rejects CLI backend API drift outside import/export after explicit G10 unlock', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/src/backend-api.mjs'), [
    'export function callBackend(document) {',
    '  return requestJson(apiUrl, "POST", "/canvas/dsl/map", document)',
    '}',
  ].join('\n'))

  const result = verifyG10PublicApiStability(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /canvas-cli must not call unexpected backend API path \/canvas\/dsl\/map after G10 import\/export unlock/)
})
