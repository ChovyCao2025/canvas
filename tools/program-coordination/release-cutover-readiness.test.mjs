import test from 'node:test'
import assert from 'node:assert/strict'
import { chmod, mkdir, mkdtemp, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const toolPath = path.join(__dirname, 'release-cutover-readiness.mjs')

test('returns ok when all release cutover gates pass', async () => {
  const root = await createFixtureRoot()

  const result = runReadiness(root, '--json')

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.ok, true)
  assert.equal(report.readiness, 'ready')
  assert.equal(report.gates.every((gate) => gate.status === 'pass'), true)
  assert.deepEqual(report.gates.map((gate) => gate.name), [
    'cutover compatibility preflight',
    'open source growth guardrails',
    'G10 public API stability',
    'playground runtime smoke',
    'Helm render smoke',
    'release script syntax',
    'backend image dry-run',
    'pre-deploy dry-run',
    'post-deploy dry-run',
    'rollback drill dry-run'
  ])
})

test('fails closed when Helm render gate fails', async () => {
  const root = await createFixtureRoot({
    failures: {
      'scripts/release/verify-helm-render.sh': {
        exitCode: 23,
        stderr: 'helm template drifted'
      }
    }
  })

  const result = runReadiness(root, '--json')

  assert.notEqual(result.status, 0)
  const report = JSON.parse(result.stdout)
  const failingGate = report.gates.find((gate) => gate.name === 'Helm render smoke')
  assert.equal(failingGate.status, 'fail')
  assert.equal(failingGate.exitCode, 23)
  assert.match(failingGate.failureSummary, /helm template drifted/)
})

test('uses the current Node executable for child Node gates', async () => {
  const root = await createFixtureRoot()
  await mkdir(path.join(root, 'bin'), { recursive: true })
  await writeFile(path.join(root, 'bin/node'), '#!/usr/bin/env bash\necho wrong node >&2\nexit 42\n')
  await chmod(path.join(root, 'bin/node'), 0o755)

  const result = runReadiness(root, '--json', {
    env: {
      ...process.env,
      PATH: `${path.join(root, 'bin')}${path.delimiter}${process.env.PATH}`
    }
  })

  assert.equal(result.status, 0, result.stderr)
  const report = JSON.parse(result.stdout)
  assert.equal(report.ok, true)
  assert.equal(report.gates.every((gate) => gate.status === 'pass'), true)
})

test('fails closed and reports the failing child gate', async () => {
  const root = await createFixtureRoot({
    failures: {
      'tools/open-source-growth/g10-public-api-stability.mjs': {
        exitCode: 17,
        stderr: 'public API stability drifted\nfull diagnostic line that should be summarized\n'
      }
    }
  })

  const result = runReadiness(root, '--json')

  assert.notEqual(result.status, 0)
  const report = JSON.parse(result.stdout)
  assert.equal(report.ok, false)
  assert.equal(report.readiness, 'blocked')
  const failingGate = report.gates.find((gate) => gate.name === 'G10 public API stability')
  assert.equal(failingGate.status, 'fail')
  assert.equal(failingGate.exitCode, 17)
  assert.match(failingGate.failureSummary, /public API stability drifted/)
  assert.equal(report.failureSummary.length, 1)
  assert.equal(report.failureSummary[0].name, 'G10 public API stability')
})

test('--json emits parseable JSON without human log prefixes', async () => {
  const root = await createFixtureRoot()

  const result = runReadiness(root, '--json')

  assert.doesNotThrow(() => JSON.parse(result.stdout))
  assert.equal(result.stdout.trimStart().startsWith('{'), true)
  assert.equal(result.stderr, '')
})

async function createFixtureRoot(options = {}) {
  const root = await mkdtemp(path.join(tmpdir(), 'release-cutover-readiness-'))
  await mkdir(path.join(root, 'tools/program-coordination'), { recursive: true })
  await mkdir(path.join(root, 'tools/open-source-growth'), { recursive: true })
  await mkdir(path.join(root, 'scripts/release'), { recursive: true })

  for (const script of [
    'tools/program-coordination/cutover-compatibility-preflight.mjs',
    'tools/open-source-growth/guardrail-verifier.mjs',
    'tools/open-source-growth/g10-public-api-stability.mjs',
    'tools/open-source-growth/playground-runtime-smoke.mjs'
  ]) {
    await writeNodeGate(root, script, options.failures?.[script])
  }

  for (const script of [
    'scripts/release/build-image.sh',
    'scripts/release/verify-helm-render.sh',
    'scripts/release/pre-deploy-check.sh',
    'scripts/release/post-deploy-check.sh',
    'scripts/release/rollback-drill.sh'
  ]) {
    await writeShellGate(root, script, options.failures?.[script])
  }

  return root
}

async function writeNodeGate(root, relativePath, failure) {
  const source = failure
    ? `#!/usr/bin/env node\nconsole.error(${JSON.stringify(failure.stderr ?? 'gate failed\\n')})\nprocess.exit(${failure.exitCode ?? 1})\n`
    : '#!/usr/bin/env node\nconsole.log(JSON.stringify({ ok: true }))\n'
  const file = path.join(root, relativePath)
  await writeFile(file, source)
  await chmod(file, 0o755)
}

async function writeShellGate(root, relativePath, failure) {
  const source = failure
    ? `#!/usr/bin/env bash\necho ${JSON.stringify(failure.stderr ?? 'gate failed')} >&2\nexit ${failure.exitCode ?? 1}\n`
    : '#!/usr/bin/env bash\nexit 0\n'
  const file = path.join(root, relativePath)
  await writeFile(file, source)
  await chmod(file, 0o755)
}

function runReadiness(root, ...args) {
  const options = args.at(-1)
  const cliArgs = options && typeof options === 'object' && !Array.isArray(options)
    ? args.slice(0, -1)
    : args
  const spawnOptions = options && typeof options === 'object' && !Array.isArray(options) ? options : {}
  return spawnSync(process.execPath, [toolPath, root, ...cliArgs], {
    cwd: path.dirname(toolPath),
    encoding: 'utf8',
    ...spawnOptions
  })
}
