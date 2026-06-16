#!/usr/bin/env node

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const CANVAS_DSL_CONTROLLER = 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java'
const CANVAS_API_COMPATIBILITY_TEST = 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java'
const PLAYGROUND_LIVE_API_SMOKE = 'tools/open-source-growth/playground-live-api-smoke.mjs'
const PLAYGROUND_RUNTIME_SMOKE = 'tools/open-source-growth/playground-runtime-smoke.mjs'
const CANVAS_CLI_INDEX = 'tools/canvas-cli/src/index.mjs'
const CANVAS_CLI_SOURCE_DIR = 'tools/canvas-cli/src'

const CONTROLLER_ROUTES = [
  { method: 'POST', path: '/validate', fullPath: '/canvas/dsl/validate', annotation: 'PostMapping' },
  { method: 'POST', path: '/map', fullPath: '/canvas/dsl/map', annotation: 'PostMapping' },
  { method: 'POST', path: '/import', fullPath: '/canvas/dsl/import', annotation: 'PostMapping' },
  { method: 'GET', path: '/export/{canvasId}', fullPath: '/canvas/dsl/export/{canvasId}', annotation: 'GetMapping' },
  { method: 'POST', path: '/diff', fullPath: '/canvas/dsl/diff', annotation: 'PostMapping' },
]

const COMPATIBILITY_TEST_PATHS = [
  '/canvas/dsl/validate',
  '/canvas/dsl/map',
  '/canvas/dsl/import',
  '/canvas/dsl/export',
  '/canvas/dsl/diff',
]

const PUBLISH_GATED_MESSAGE = 'Publish is gated until a stable backend publish API is verified'
const REQUIRED_CLI_IMPORT_EXPORT_SURFACES = [
  { text: 'canvas-cli import <file>', label: 'canvas-cli import <file>' },
  { text: 'canvas-cli export <canvasId>', label: 'canvas-cli export <canvasId>' },
  { text: '/canvas/dsl/import', label: '/canvas/dsl/import' },
  { text: '/canvas/dsl/export/', label: '/canvas/dsl/export/' },
]
const FORBIDDEN_CLI_SURFACES = [
  { text: 'canvas-cli publish <canvasId>', label: 'canvas-cli publish <canvasId>' },
  { text: 'fetch(', label: 'fetch(' },
  { text: '/canvas/dsl/validate', label: '/canvas/dsl/validate' },
  { text: '/canvas/dsl/map', label: '/canvas/dsl/map' },
  { text: '/canvas/dsl/diff', label: '/canvas/dsl/diff' },
  { text: '/publish', label: '/publish' },
]

function readRequired(errors, root, relativePath) {
  const file = path.join(root, relativePath)
  if (!existsSync(file)) {
    errors.push(`${relativePath} is required`)
    return ''
  }
  const content = readFileSync(file, 'utf8')
  if (content.trim().length === 0) {
    errors.push(`${relativePath} is required`)
  }
  return content
}

function listFiles(dir) {
  if (!existsSync(dir)) {
    return []
  }
  const files = []
  for (const entry of readdirSync(dir)) {
    const entryPath = path.join(dir, entry)
    const stat = statSync(entryPath)
    if (stat.isDirectory()) {
      files.push(...listFiles(entryPath))
    } else {
      files.push(entryPath)
    }
  }
  return files
}

function hasAnnotation(content, annotation, routePath) {
  const escaped = routePath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`@${annotation}\\s*\\(\\s*(?:value\\s*=\\s*)?["']${escaped}["']`, 'm').test(content)
}

function verifyController(errors, content) {
  if (!content) {
    return
  }
  if (!hasAnnotation(content, 'RequestMapping', '/canvas/dsl')) {
    errors.push('CanvasDslController.java must expose base path /canvas/dsl')
  }
  for (const route of CONTROLLER_ROUTES) {
    if (!hasAnnotation(content, route.annotation, route.path)) {
      errors.push(`CanvasDslController.java must expose ${route.method} ${route.fullPath}`)
    }
  }
}

function verifyCompatibilityTest(errors, content) {
  if (!content) {
    return
  }
  for (const routePath of COMPATIBILITY_TEST_PATHS) {
    if (!content.includes(routePath)) {
      const required = routePath === '/canvas/dsl/export'
        ? '/canvas/dsl/export/{canvasId} or /canvas/dsl/export/'
        : routePath
      errors.push(`CanvasApiCompatibilityTest.java must cover ${required}`)
    }
  }
}

function verifyLiveSmoke(errors, content) {
  if (!content) {
    return
  }
  if (!content.includes('/canvas/dsl/map')) {
    errors.push('playground-live-api-smoke.mjs must call /canvas/dsl/map')
  }
  for (const shapeField of ['templateKey', 'graphJson', 'violations']) {
    if (!content.includes(shapeField)) {
      errors.push(`playground-live-api-smoke.mjs must validate map response ${shapeField}`)
    }
  }
}

function verifyRuntimeSmoke(errors, content) {
  if (!content) {
    return
  }
  if (!content.includes('verifyPlaygroundRuntimeSmoke')) {
    errors.push('playground-runtime-smoke.mjs must export verifyPlaygroundRuntimeSmoke')
  }
}

function verifyCliGating(errors, indexContent, allSourceContent) {
  if (!indexContent) {
    return
  }
  if (!indexContent.includes(PUBLISH_GATED_MESSAGE)) {
    errors.push('canvas-cli must keep publish gated until a stable backend publish API is verified')
  }
  for (const surface of REQUIRED_CLI_IMPORT_EXPORT_SURFACES) {
    if (!allSourceContent.includes(surface.text)) {
      const verb = surface.text.startsWith('/') ? 'call backend API path' : 'expose'
      errors.push(`canvas-cli must ${verb} ${surface.label} after explicit G10 import/export unlock`)
    }
  }
  for (const command of ['import', 'export']) {
    const commandPattern = new RegExp(`['"]${command}['"]`)
    if (!commandPattern.test(indexContent)) {
      errors.push(`canvas-cli must explicitly handle ${command} after G10 import/export unlock`)
    }
  }
  if (!/['"]publish['"]/.test(indexContent)) {
    errors.push('canvas-cli must explicitly gate publish until a stable backend publish API is verified')
  }
  for (const surface of FORBIDDEN_CLI_SURFACES) {
    if (allSourceContent.includes(surface.text)) {
      if (surface.text === '/publish') {
        errors.push(`canvas-cli must not call publish backend path ${surface.label}`)
        continue
      }
      const verb = surface.text.startsWith('/') ? 'call unexpected backend API path' : 'expose'
      errors.push(`canvas-cli must not ${verb} ${surface.label} after G10 import/export unlock`)
    }
  }
}

export function verifyG10PublicApiStability(rootDir = process.cwd()) {
  const root = path.resolve(rootDir)
  const errors = []

  verifyController(errors, readRequired(errors, root, CANVAS_DSL_CONTROLLER))
  verifyCompatibilityTest(errors, readRequired(errors, root, CANVAS_API_COMPATIBILITY_TEST))
  verifyLiveSmoke(errors, readRequired(errors, root, PLAYGROUND_LIVE_API_SMOKE))
  verifyRuntimeSmoke(errors, readRequired(errors, root, PLAYGROUND_RUNTIME_SMOKE))
  const cliIndex = readRequired(errors, root, CANVAS_CLI_INDEX)
  const cliSourceContent = listFiles(path.join(root, CANVAS_CLI_SOURCE_DIR))
    .filter(file => file.endsWith('.mjs') || file.endsWith('.js'))
    .map(file => readFileSync(file, 'utf8'))
    .join('\n')
  verifyCliGating(errors, cliIndex, cliSourceContent)

  const ok = errors.length === 0
  return {
    ok,
    status: ok ? 'PASS' : 'FAIL',
    summary: ok
      ? 'Canvas DSL public API stability evidence is present and CLI import/export are unlocked while publish remains gated.'
      : 'Canvas DSL public API stability evidence is missing, CLI import/export drifted, or publish was prematurely ungated.',
    errors,
  }
}

function printResult(result) {
  console.log(JSON.stringify({
    status: result.status,
    summary: result.summary,
    errors: result.errors,
  }))
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) {
  const result = verifyG10PublicApiStability(process.argv[2] || process.cwd())
  printResult(result)
  if (!result.ok) {
    process.exitCode = 1
  }
}
