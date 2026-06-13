import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const SCRIPT = path.resolve('docs/ddd-rewrite/inventory/check-inventory-readiness.sh')

function writeJava(root, relativePath) {
  const full = path.join(root, relativePath)
  mkdirSync(path.dirname(full), { recursive: true })
  writeFileSync(full, 'class Fixture {}\n')
}

function row({
  source = 'org.chovy.canvas.web.FixtureController',
  currentPath,
  targetModule = 'canvas-context-marketing',
  targetPackage = 'org.chovy.canvas.marketing.application',
  targetRole = '',
  owningWorker = 'DDD-W03',
  requiredTests = 'FixtureTest',
  coordinatorDecision = '',
}) {
  return [
    'old class:',
    `  ${source}`,
    '',
    'current path:',
    `  ${currentPath}`,
    '',
    'target module:',
    `  ${targetModule}`,
    '',
    targetPackage ? 'target package:' : 'target role:',
    `  ${targetPackage || targetRole}`,
    '',
    owningWorker ? 'owning worker:' : 'coordinator decision:',
    `  ${owningWorker || coordinatorDecision}`,
    '',
    'required tests:',
    `  ${requiredTests}`,
    '',
  ].join('\n')
}

function crossContextRow() {
  return [
    'source path:',
    '  org.chovy.canvas.engine.CanvasExecutionService -> org.chovy.canvas.domain.risk.RiskPolicyService',
    '',
    'current path:',
    '  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/CanvasExecutionService.java',
    '',
    'target module:',
    '  canvas-context-execution',
    '',
    'target role:',
    '  cross-context dependency',
    '',
    'coordinator decision:',
    '  execution calls risk through public API',
    '',
  ].join('\n')
}

async function fixture() {
  const root = await mkdtemp(path.join(tmpdir(), 'ddd-inventory-'))
  mkdirSync(path.join(root, 'docs/ddd-rewrite/inventory'), { recursive: true })
  mkdirSync(path.join(root, 'docs/program-coordination'), { recursive: true })

  writeJava(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/FooController.java')
  writeJava(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/FooDO.java')
  writeJava(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/FooMapper.java')
  writeJava(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/domain/FooService.java')
  writeJava(root, 'backend/canvas-engine/src/test/java/org/chovy/canvas/FooTest.java')

  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/http-api-inventory.md'),
    row({
      source: 'org.chovy.canvas.web.FooController',
      currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/FooController.java',
    }),
  )
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/persistence-ownership.md'),
    [
      row({
        source: 'org.chovy.canvas.dal.dataobject.FooDO',
        currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/FooDO.java',
      }),
      row({
        source: 'org.chovy.canvas.dal.mapper.FooMapper',
        currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/FooMapper.java',
      }),
    ].join('\n'),
  )
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/service-ownership.md'),
    row({
      source: 'org.chovy.canvas.domain.FooService',
      currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/domain/FooService.java',
      targetPackage: '',
      targetRole: 'application service',
    }),
  )
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/test-ownership.md'),
    row({
      source: 'org.chovy.canvas.FooTest',
      currentPath: 'backend/canvas-engine/src/test/java/org/chovy/canvas/FooTest.java',
    }),
  )
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/cross-context-dependencies.md'),
    crossContextRow(),
  )
  writeFileSync(
    path.join(root, 'docs/program-coordination/subagent-worker-packets.md'),
    Array.from({ length: 8 }, (_, index) => [
      `### DDD-W0${index + 1}: Worker`,
      '',
      '```text',
      'Inventory rows required:',
      '```',
    ].join('\n')).join('\n'),
  )
  return root
}

function runInventory(root) {
  return execFileSync('bash', [SCRIPT, root], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })
}

function runInventoryFailure(root) {
  try {
    runInventory(root)
    assert.fail('expected inventory readiness to fail')
  } catch (error) {
    return `${error.stdout ?? ''}${error.stderr ?? ''}`
  }
}

test('passes when inventory rows exactly match discovered source paths', async () => {
  const root = await fixture()

  const output = runInventory(root)

  assert.match(output, /Inventory readiness passed\./)
})

test('rejects duplicate rows that hide missing source paths', async () => {
  const root = await fixture()
  writeJava(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/BarController.java')
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/http-api-inventory.md'),
    [
      row({
        source: 'org.chovy.canvas.web.FooController',
        currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/FooController.java',
      }),
      row({
        source: 'org.chovy.canvas.web.FooController',
        currentPath: 'backend/canvas-engine/src/main/java/org/chovy/canvas/web/FooController.java',
      }),
    ].join('\n'),
  )

  const output = runInventoryFailure(root)

  assert.match(output, /duplicate source row/)
  assert.match(output, /duplicate current path/)
  assert.match(output, /missing current path.*backend\/canvas-engine\/src\/main\/java\/org\/chovy\/canvas\/web\/BarController\.java/)
})

test('rejects empty row values and rows with multiple ownership decisions', async () => {
  const root = await fixture()
  writeFileSync(
    path.join(root, 'docs/ddd-rewrite/inventory/service-ownership.md'),
    [
      'old class:',
      '  org.chovy.canvas.domain.FooService',
      '',
      'current path:',
      '  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/FooService.java',
      '',
      'target module:',
      '',
      'target role:',
      '  application service',
      '',
      'owning worker:',
      '  DDD-W03',
      '',
      'coordinator decision:',
      '  coordinator also claimed this row',
      '',
      'required tests:',
      '  FooServiceTest',
      '',
    ].join('\n'),
  )

  const output = runInventoryFailure(root)

  assert.match(output, /has empty target module:/)
  assert.match(output, /must contain exactly one of owning worker: or coordinator decision:/)
})
