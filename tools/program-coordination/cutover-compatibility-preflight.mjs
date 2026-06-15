#!/usr/bin/env node

import { readdir, readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const oldWebPath = 'backend/canvas-engine/src/main/java/org/chovy/canvas/web'
const canvasWebSourcePath = 'backend/canvas-web/src/main/java'
const compatibilityTestPath = 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'
const canvasBootPomPath = 'backend/canvas-boot/pom.xml'
const canvasBootApplicationPath = 'backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasBootApplication.java'
const requiredRunCommandFiles = [
  'README.md',
  'CONTRIBUTING.md',
  'AGENTS.md',
  'CLAUDE.md',
  'docs/open-source/quickstart.md',
  'docs/open-source/en/quickstart.md',
  'docs/canvas-examples/STARTUP.md',
  'docs/code-review/runtime-verification.md'
]
const requiredPackagedRuntimeFiles = [
  '.dockerignore',
  '.github/workflows/ci.yml',
  '.github/workflows/canvas-ci.yml',
  'deploy/helm/canvas/values.yaml',
  'docs/architecture/evidence/runbooks/release-deployment.md',
  'backend/canvas-boot/Dockerfile.perf',
  'scripts/release/build-image.sh',
  'scripts/release/check-flyway-migration.sh',
  'scripts/release/validate-production-profile.sh',
  'scripts/release/verify-helm-render.sh',
  'scripts/verify-flink-realtime-warehouse-live.sh'
]
const requiredProductionPreflightFiles = [
  'scripts/verify-flink-production-deployment.sh'
]
const activeRuntimeRunbookFiles = [
  'docs/stressTest/local-capacity-runbook.md',
  'docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md',
  'docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md',
  'docs/runbooks/marketing-content-production-readiness.md',
  'docs/runbooks/risk-control-rule-engine.md',
  'docs/runbooks/flyway-migration-history-repair-2026-06-06.md'
]

const requiredCompatibilityTests = [
  'CanvasApiCompatibilityTest',
  'ExecutionApiCompatibilityTest',
  'MarketingApiCompatibilityTest',
  'CdpApiCompatibilityTest',
  'BiApiCompatibilityTest',
  'RiskApiCompatibilityTest',
  'ConversationApiCompatibilityTest'
]

const routeGapCandidateLimit = 10
const representativeOldControllerFileLimit = 3

const httpMethodsByAnnotation = {
  GetMapping: ['GET'],
  PostMapping: ['POST'],
  PutMapping: ['PUT'],
  DeleteMapping: ['DELETE'],
  PatchMapping: ['PATCH']
}

export async function buildReport(rootDirectory) {
  const root = path.resolve(rootDirectory)
  const oldCounts = await countControllers(root, oldWebPath)
  const currentCounts = await countControllers(root, canvasWebSourcePath)
  const compatibilityTests = await inspectCompatibilityTests(root)
  const bootCutover = await inspectBootCutover(root)
  const runCommandCutover = await inspectRunCommandCutover(root)
  const packagedRuntimeCutover = await inspectPackagedRuntimeCutover(root)
  const productionPreflightCutover = await inspectProductionPreflightCutover(root)
  const activeRuntimeRunbookCutover = await inspectActiveRuntimeRunbookCutover(root)
  const serviceNameCompatibility = await inspectServiceNameCompatibility(root)
  const routeGapSummary = buildRouteGapSummary(oldCounts.controllers, currentCounts.controllers)
  const blockers = buildBlockers(oldCounts, currentCounts, compatibilityTests, routeGapSummary, bootCutover, runCommandCutover, packagedRuntimeCutover, productionPreflightCutover, activeRuntimeRunbookCutover, serviceNameCompatibility)

  return {
    root,
    oldCanvasEngineWeb: {
      path: oldWebPath,
      present: oldCounts.present,
      controllerCount: oldCounts.controllerCount,
      endpointCount: oldCounts.endpointCount
    },
    currentCanvasWeb: {
      controllers: {
        path: canvasWebSourcePath,
        present: currentCounts.present,
        controllerCount: currentCounts.controllerCount,
        endpointCount: currentCounts.endpointCount
      },
      compatibilityTests
    },
    currentCanvasBoot: bootCutover,
    runCommandCutover,
    packagedRuntimeCutover,
    productionPreflightCutover,
    activeRuntimeRunbookCutover,
    serviceNameCompatibility,
    routeGapSummary,
    cutoverReady: blockers.length === 0,
    blockers
  }
}

async function countControllers(root, sourcePath) {
  const directory = path.join(root, sourcePath)
  const javaDirectory = await listJavaFiles(directory)
  if (!javaDirectory.present) {
    return { present: false, controllerCount: 0, endpointCount: 0, controllers: [] }
  }

  const controllers = []

  for (const file of javaDirectory.files) {
    const source = stripJavaComments(await readFile(file, 'utf8'))
    if (!/@(?:RestController|Controller)\b/.test(source)) {
      continue
    }

    const endpoints = extractControllerEndpoints(source)
    controllers.push({
      file: toPosixPath(path.relative(root, file)),
      className: path.basename(file, '.java'),
      endpointCount: endpoints.length,
      endpoints
    })
  }

  return {
    present: true,
    controllerCount: controllers.length,
    endpointCount: controllers.reduce((count, controller) => count + controller.endpointCount, 0),
    controllers
  }
}

async function listJavaFiles(directory) {
  try {
    const directoryStat = await stat(directory)
    if (!directoryStat.isDirectory()) {
      return { present: false, files: [] }
    }
  } catch (error) {
    if (error.code === 'ENOENT') {
      return { present: false, files: [] }
    }
    throw error
  }

  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map(async (entry) => {
    const entryPath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      const nested = await listJavaFiles(entryPath)
      return nested.files
    }
    if (entry.isFile() && entry.name.endsWith('.java')) {
      return [entryPath]
    }
    return []
  }))

  return { present: true, files: files.flat().sort() }
}

function stripJavaComments(source) {
  return source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/^\s*\/\/.*$/gm, '')
}

function countControllerEndpoints(source) {
  return extractControllerEndpoints(source).length
}

function extractControllerEndpoints(source) {
  const classIndex = source.search(/\bclass\b/)
  const header = classIndex === -1 ? source : source.slice(0, classIndex)
  const body = classIndex === -1 ? source : source.slice(classIndex)
  const classPaths = extractClassPaths(header)
  const mappings = extractMappingAnnotations(body)
  const endpoints = []

  for (const mapping of mappings) {
    const methodPaths = extractMappingPaths(mapping.argumentsText)
    const httpMethods = extractHttpMethods(mapping.name, mapping.argumentsText)
    for (const classPath of classPaths) {
      for (const methodPath of methodPaths) {
        for (const httpMethod of httpMethods) {
          endpoints.push({
            httpMethod,
            path: joinRoutePaths(classPath, methodPath)
          })
        }
      }
    }
  }

  return endpoints
}

function extractClassPaths(header) {
  const mappings = extractMappingAnnotations(header).filter((mapping) => mapping.name === 'RequestMapping')
  if (mappings.length === 0) {
    return ['']
  }
  return extractMappingPaths(mappings[mappings.length - 1].argumentsText)
}

function extractMappingAnnotations(source) {
  const annotations = []
  const annotationPattern = /@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\b/g
  let match

  while ((match = annotationPattern.exec(source)) !== null) {
    const name = match[1]
    let cursor = annotationPattern.lastIndex
    while (/\s/.test(source[cursor] ?? '')) {
      cursor += 1
    }

    let argumentsText = ''
    if (source[cursor] === '(') {
      const parsed = readBalancedParentheses(source, cursor)
      argumentsText = parsed.text
      annotationPattern.lastIndex = parsed.endIndex
    } else {
      annotationPattern.lastIndex = cursor
    }

    annotations.push({ name, argumentsText })
  }

  return annotations
}

function readBalancedParentheses(source, openingIndex) {
  let depth = 0
  let quote = null
  let escaped = false

  for (let index = openingIndex; index < source.length; index += 1) {
    const character = source[index]

    if (quote) {
      if (escaped) {
        escaped = false
      } else if (character === '\\') {
        escaped = true
      } else if (character === quote) {
        quote = null
      }
      continue
    }

    if (character === '"' || character === "'") {
      quote = character
      continue
    }

    if (character === '(') {
      depth += 1
      continue
    }

    if (character === ')') {
      depth -= 1
      if (depth === 0) {
        return {
          text: source.slice(openingIndex + 1, index),
          endIndex: index + 1
        }
      }
    }
  }

  return {
    text: source.slice(openingIndex + 1),
    endIndex: source.length
  }
}

function extractMappingPaths(argumentsText) {
  const trimmed = argumentsText.trim()
  if (trimmed === '') {
    return ['']
  }

  const explicitPath = readNamedAttributeValue(trimmed, 'path') ?? readNamedAttributeValue(trimmed, 'value')
  const pathExpression = explicitPath ?? readPositionalPathValue(trimmed)
  const paths = extractQuotedStrings(pathExpression)
  return paths.length === 0 ? [''] : paths
}

function readNamedAttributeValue(text, attributeName) {
  const attributePattern = new RegExp(`\\b${attributeName}\\s*=`, 'g')
  const match = attributePattern.exec(text)
  if (!match) {
    return null
  }

  let cursor = attributePattern.lastIndex
  while (/\s/.test(text[cursor] ?? '')) {
    cursor += 1
  }

  return readAnnotationValueExpression(text, cursor)
}

function readPositionalPathValue(text) {
  const trimmed = text.trimStart()
  if (trimmed.startsWith('"') || trimmed.startsWith('{')) {
    return readAnnotationValueExpression(trimmed, 0)
  }
  return ''
}

function readAnnotationValueExpression(text, startIndex) {
  const first = text[startIndex]
  if (first === '{') {
    let depth = 0
    let quote = null
    let escaped = false

    for (let index = startIndex; index < text.length; index += 1) {
      const character = text[index]

      if (quote) {
        if (escaped) {
          escaped = false
        } else if (character === '\\') {
          escaped = true
        } else if (character === quote) {
          quote = null
        }
        continue
      }

      if (character === '"' || character === "'") {
        quote = character
        continue
      }

      if (character === '{') {
        depth += 1
        continue
      }

      if (character === '}') {
        depth -= 1
        if (depth === 0) {
          return text.slice(startIndex, index + 1)
        }
      }
    }
  }

  if (first === '"') {
    let escaped = false
    for (let index = startIndex + 1; index < text.length; index += 1) {
      const character = text[index]
      if (escaped) {
        escaped = false
      } else if (character === '\\') {
        escaped = true
      } else if (character === '"') {
        return text.slice(startIndex, index + 1)
      }
    }
  }

  const commaIndex = text.indexOf(',', startIndex)
  return commaIndex === -1 ? text.slice(startIndex) : text.slice(startIndex, commaIndex)
}

function extractQuotedStrings(text) {
  const strings = []
  const stringPattern = /"((?:\\.|[^"\\])*)"/g
  let match
  while ((match = stringPattern.exec(text)) !== null) {
    strings.push(match[1])
  }
  return strings
}

function extractHttpMethods(annotationName, argumentsText) {
  const fixedMethods = httpMethodsByAnnotation[annotationName]
  if (fixedMethods) {
    return fixedMethods
  }

  const methods = new Set()
  const methodPattern = /RequestMethod\.(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)\b/g
  let match
  while ((match = methodPattern.exec(argumentsText)) !== null) {
    methods.add(match[1])
  }

  return methods.size === 0 ? ['REQUEST'] : [...methods].sort()
}

function joinRoutePaths(classPath, methodPath) {
  const segments = [classPath, methodPath]
    .filter((routePart) => routePart !== '')
    .map((routePart) => routePart.replace(/^\/+|\/+$/g, ''))
    .filter((routePart) => routePart !== '')
  return normalizeRoutePath(segments.join('/'))
}

function normalizeRoutePath(routePath) {
  if (routePath === '') {
    return '/'
  }
  const normalized = `/${routePath}`.replace(/\/+/g, '/')
  return normalized.length > 1 ? normalized.replace(/\/$/g, '') : normalized
}

function buildRouteGapSummary(oldControllers, currentControllers) {
  const topLevelStats = buildTopLevelRouteStats([...oldControllers, ...currentControllers])
  const uncoveredOldControllers = filterUncoveredOldControllerEndpoints(oldControllers, currentControllers)
  const oldGroups = groupControllersByRoute(uncoveredOldControllers, topLevelStats)
  const currentGroups = groupControllersByRoute(currentControllers, topLevelStats)
  const candidateGroups = [...oldGroups.keys()].filter((group) => {
    const oldGroup = oldGroups.get(group)
    const currentGroup = currentGroups.get(group) ?? emptyRouteGroup(group)
    return oldGroup.endpointCount > 0 ||
      oldGroup.controllerFiles.size > currentGroup.controllerFiles.size
  })

  const candidates = candidateGroups
    .map((group) => {
      const oldGroup = oldGroups.get(group)
      const currentGroup = currentGroups.get(group) ?? emptyRouteGroup(group)
      return {
        group,
        oldControllerCount: oldGroup.controllerFiles.size,
        oldEndpointCount: oldGroup.endpointCount,
        currentControllerCount: currentGroup.controllerFiles.size,
        currentEndpointCount: currentGroup.endpointCount,
        representativeOldControllerFiles: [...oldGroup.controllerFiles]
          .sort()
          .slice(0, representativeOldControllerFileLimit)
      }
    })
    .sort((left, right) => (
      (right.oldEndpointCount - right.currentEndpointCount) - (left.oldEndpointCount - left.currentEndpointCount) ||
      (right.oldControllerCount - right.currentControllerCount) - (left.oldControllerCount - left.currentControllerCount) ||
      right.oldEndpointCount - left.oldEndpointCount ||
      left.group.localeCompare(right.group)
    ))

  return {
    candidateLimit: routeGapCandidateLimit,
    candidateCount: candidates.length,
    reportedCandidateCount: Math.min(candidates.length, routeGapCandidateLimit),
    candidates: candidates.slice(0, routeGapCandidateLimit)
  }
}

function filterUncoveredOldControllerEndpoints(oldControllers, currentControllers) {
  const currentEndpointKeys = new Set(currentControllers.flatMap((controller) => (
    controller.endpoints.map(endpointRouteKey)
  )))

  return oldControllers
    .flatMap((controller) => {
      if (controller.endpoints.length === 0) {
        return [controller]
      }

      const endpoints = controller.endpoints.filter((endpoint) => !currentEndpointKeys.has(endpointRouteKey(endpoint)))
      if (endpoints.length === 0) {
        return []
      }

      return {
        ...controller,
        endpointCount: endpoints.length,
        endpoints
      }
    })
}

function endpointRouteKey(endpoint) {
  return `${endpoint.httpMethod} ${normalizeRoutePathVariables(endpoint.path)}`
}

function normalizeRoutePathVariables(routePath) {
  return normalizeRoutePath(routePath).replace(/\{[^}/]+\}/g, '{}')
}

function buildTopLevelRouteStats(controllers) {
  const stats = new Map()
  for (const controller of controllers) {
    const endpointPaths = controller.endpoints.length === 0 ? ['/'] : controller.endpoints.map((endpoint) => endpoint.path)
    for (const endpointPath of endpointPaths) {
      const firstSegment = readRouteSegments(endpointPath)[0] ?? ''
      const key = firstSegment === '' ? controllerFamilyGroup(controller.className) : firstSegment
      const entry = stats.get(key) ?? { endpointCount: 0, controllerFiles: new Set() }
      entry.endpointCount += 1
      entry.controllerFiles.add(controller.file)
      stats.set(key, entry)
    }
  }
  return stats
}

function groupControllersByRoute(controllers, topLevelStats) {
  const groups = new Map()
  for (const controller of controllers) {
    if (controller.endpoints.length === 0) {
      addRouteGroup(groups, controllerFamilyGroup(controller.className), controller.file, 0)
      continue
    }

    for (const endpoint of controller.endpoints) {
      addRouteGroup(groups, routeGroupForEndpoint(endpoint.path, controller, topLevelStats), controller.file, 1)
    }
  }
  return groups
}

function routeGroupForEndpoint(endpointPath, controller, topLevelStats) {
  const segments = readRouteSegments(endpointPath)
  if (segments.length === 0) {
    return controllerFamilyGroup(controller.className)
  }

  const topLevel = segments[0]
  const topLevelEntry = topLevelStats.get(topLevel)
  const splitBroadPrefix = topLevelEntry && (topLevelEntry.controllerFiles.size > 5 || topLevelEntry.endpointCount > 25)
  if (!splitBroadPrefix) {
    return `route:/${topLevel}`
  }

  const secondSegment = segments[1]
  if (!secondSegment || isRouteVariable(secondSegment)) {
    return controllerFamilyGroup(controller.className)
  }
  return `route:/${topLevel}/${secondSegment}`
}

function readRouteSegments(routePath) {
  return normalizeRoutePath(routePath)
    .split('/')
    .filter((segment) => segment !== '')
}

function isRouteVariable(segment) {
  return segment.startsWith('{') && segment.endsWith('}')
}

function controllerFamilyGroup(className) {
  const baseName = className.replace(/Controller$/u, '')
  const familyParts = baseName.match(/[A-Z]+(?=[A-Z][a-z]|$)|[A-Z]?[a-z]+|\d+/g) ?? [baseName]
  return `family:${familyParts.slice(0, 2).join('') || baseName}`
}

function addRouteGroup(groups, group, controllerFile, endpointCount) {
  const entry = groups.get(group) ?? emptyRouteGroup(group)
  entry.controllerFiles.add(controllerFile)
  entry.endpointCount += endpointCount
  groups.set(group, entry)
}

function emptyRouteGroup(group) {
  return {
    group,
    controllerFiles: new Set(),
    endpointCount: 0
  }
}

function toPosixPath(filePath) {
  return filePath.split(path.sep).join('/')
}

async function inspectCompatibilityTests(root) {
  const testDirectory = path.join(root, compatibilityTestPath)
  const required = await Promise.all(requiredCompatibilityTests.map(async (className) => {
    const file = `${compatibilityTestPath}/${className}.java`
    return {
      className,
      file,
      present: await fileExists(path.join(root, file))
    }
  }))
  const presentCount = required.filter((entry) => entry.present).length

  return {
    path: compatibilityTestPath,
    present: await directoryExists(testDirectory),
    presentCount,
    missingCount: required.length - presentCount,
    required
  }
}

async function inspectBootCutover(root) {
  const pomFile = path.join(root, canvasBootPomPath)
  const applicationFile = path.join(root, canvasBootApplicationPath)
  const pomPresent = await fileExists(pomFile)
  const applicationPresent = await fileExists(applicationFile)
  const pomSource = pomPresent ? await readFile(pomFile, 'utf8') : ''
  const applicationSource = applicationPresent ? stripJavaComments(await readFile(applicationFile, 'utf8')) : ''
  const scanBasePackages = extractSpringBootScanBasePackages(applicationSource)

  return {
    pom: {
      path: canvasBootPomPath,
      present: pomPresent,
      dependsOnCanvasWeb: pomSource.includes('<artifactId>canvas-web</artifactId>'),
      dependsOnCanvasEngine: pomSource.includes('<artifactId>canvas-engine</artifactId>')
    },
    application: {
      path: canvasBootApplicationPath,
      present: applicationPresent,
      scanBasePackages,
      scansOrgChovyCanvas: scanBasePackages.includes('org.chovy.canvas')
    }
  }
}

async function inspectRunCommandCutover(root) {
  const requiredFiles = await Promise.all(requiredRunCommandFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return {
      file,
      present,
      startsCanvasBoot: usesSpringBootRunForModule(content, 'canvas-boot'),
      startsCanvasEngine: usesSpringBootRunForModule(content, 'canvas-engine'),
      usesLegacyCanvasEngineMavenModule: usesMavenModuleCommand(content, 'canvas-engine'),
      referencesLegacyEngineMigrationPath: /backend\/canvas-engine\/src\/main\/resources\/db\/migration/.test(content),
      referencesCanvasBootMigrationPath: /backend\/canvas-boot\/src\/main\/resources\/db\/migration/.test(content)
    }
  }))

  return { requiredFiles }
}

async function inspectPackagedRuntimeCutover(root) {
  const requiredFiles = await Promise.all(requiredPackagedRuntimeFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return {
      file,
      present,
      usesCanvasBootArtifact: content.includes('canvas-boot'),
      usesCanvasEngineArtifact: /canvas-engine(?:\/pom\.xml|-\*\.jar|-1\.0\.0-SNAPSHOT\.jar|\/target\/canvas-engine| -am package|\/Dockerfile)/.test(content),
      usesLegacyCanvasEngineMavenModule: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? usesMavenModuleCommand(content, 'canvas-engine')
        : undefined,
      hasBootRuntimeTestGate: file === '.github/workflows/ci.yml'
        ? content.includes('-pl canvas-boot') && content.includes('ModularArchitectureTest') && content.includes('CanvasBootApplicationSmokeTest')
        : undefined,
      hasHelmRenderGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? content.includes('azure/setup-helm') && content.includes('scripts/release/verify-helm-render.sh')
        : undefined,
      hasBootFlywayMigrationPolicyGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? content.includes('scripts/release/check-flyway-migration.sh') &&
          content.includes('-pl canvas-boot') &&
          content.includes('FlywayMigrationPolicyTest') &&
          !content.includes('-pl canvas-engine -am -Dtest=FlywayMigrationPolicyTest')
        : undefined,
      hasProgramCoordinationGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? content.includes('tools/program-coordination/check-dispatch-state.mjs') &&
          content.includes('node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs') &&
          content.includes('tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready')
        : undefined
    }
  }))

  return { requiredFiles }
}

async function inspectProductionPreflightCutover(root) {
  const requiredFiles = await Promise.all(requiredProductionPreflightFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return {
      file,
      present,
      anchorsLegacyEngineSource: /backend\/canvas-engine\/src\/main\/java|canvas-engine\/src\/main\/java/.test(content),
      checksHelmPipelineContract: content.includes('deploy/helm/canvas/values.yaml') &&
        content.includes('deploy/k8s/canvas-flink-job-submitter.yaml') &&
        content.includes('docs/runbooks/flink-production-deployment.md') &&
        content.includes('--pipeline-key=${pipeline}')
    }
  }))

  return { requiredFiles }
}

async function inspectActiveRuntimeRunbookCutover(root) {
  const requiredFiles = await Promise.all(activeRuntimeRunbookFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return {
      file,
      present,
      usesLegacyCanvasEngineMavenModule: /\bmvn\b[^\n]*(?:-pl\s+canvas-engine|-f\s+(?:backend\/)?canvas-engine\/pom\.xml)/.test(content),
      usesLegacyCanvasEnginePerfImage: /backend\/canvas-engine\/Dockerfile\.perf|canvas-engine:perf/.test(content),
      usesCanvasBootRuntime: content.includes('canvas-boot')
    }
  }))

  return { requiredFiles }
}

async function inspectServiceNameCompatibility(root) {
  const helmValuesPath = 'deploy/helm/canvas/values.yaml'
  const helmValuesFile = path.join(root, helmValuesPath)
  const helmValuesPresent = await fileExists(helmValuesFile)
  const helmValues = helmValuesPresent ? await readFile(helmValuesFile, 'utf8') : ''

  return {
    helmValues: {
      path: helmValuesPath,
      present: helmValuesPresent,
      backendName: readSimpleYamlScalar(helmValues, 'backend', 'name'),
      imageRepository: readNestedYamlScalar(helmValues, ['backend', 'image'], 'repository'),
      serviceAccountName: readSimpleYamlScalar(helmValues, 'backend', 'serviceAccountName'),
      secretName: readSimpleYamlScalar(helmValues, 'backend', 'secretName'),
      keepsCanvasEngineBackendName: readSimpleYamlScalar(helmValues, 'backend', 'name') === 'canvas-engine',
      usesCanvasBootImageRepository: readNestedYamlScalar(helmValues, ['backend', 'image'], 'repository')?.endsWith('/canvas-boot') === true,
      keepsCanvasEngineServiceAccount: readSimpleYamlScalar(helmValues, 'backend', 'serviceAccountName') === 'canvas-engine',
      keepsCanvasEngineRuntimeSecret: readSimpleYamlScalar(helmValues, 'backend', 'secretName') === 'canvas-engine-runtime'
    }
  }
}

function readSimpleYamlScalar(content, sectionName, key) {
  const section = readTopLevelYamlSection(content, sectionName)
  const match = section.match(new RegExp(`^  ${escapeRegExp(key)}:\\s*"?([^"\\n]+)"?\\s*$`, 'm'))
  return match?.[1]?.trim()
}

function readNestedYamlScalar(content, sectionPath, key) {
  let section = content
  let indent = 0
  for (const sectionName of sectionPath) {
    section = readYamlSection(section, sectionName, indent)
    indent += 2
  }
  const match = section.match(new RegExp(`^${' '.repeat(indent)}${escapeRegExp(key)}:\\s*"?([^"\\n]+)"?\\s*$`, 'm'))
  return match?.[1]?.trim()
}

function readTopLevelYamlSection(content, sectionName) {
  return readYamlSection(content, sectionName, 0)
}

function readYamlSection(content, sectionName, indent) {
  const escaped = escapeRegExp(sectionName)
  const pattern = new RegExp(`^${' '.repeat(indent)}${escaped}:\\s*\\n([\\s\\S]*?)(?=^${' '.repeat(indent)}\\S|(?![\\s\\S]))`, 'm')
  return pattern.exec(content)?.[1] ?? ''
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function usesSpringBootRunForModule(content, moduleName) {
  return content.includes(`mvn -f ${moduleName}/pom.xml`) && content.includes('spring-boot:run') ||
    content.includes(`mvn -pl ${moduleName}`) && content.includes('spring-boot:run') ||
    content.includes(`mvn -f backend/pom.xml -pl ${moduleName}`) && content.includes('spring-boot:run') ||
    content.includes(`cd ${moduleName}`) && content.includes('spring-boot:run') ||
    content.includes(`cd backend/${moduleName}`) && content.includes('spring-boot:run')
}

function usesMavenModuleCommand(content, moduleName) {
  const modulePattern = escapeRegExp(moduleName)
  return new RegExp(`\\bmvn\\b[^\\n]*(?:-pl\\s+${modulePattern}\\b|-f\\s+(?:backend/)?${modulePattern}/pom\\.xml)`).test(content)
}

function extractSpringBootScanBasePackages(source) {
  const annotation = extractAnnotation(source, 'SpringBootApplication')
  if (!annotation) {
    return []
  }

  const explicitScan = readNamedAttributeValue(annotation.argumentsText, 'scanBasePackages') ??
    readNamedAttributeValue(annotation.argumentsText, 'scanBasePackageClasses')
  if (!explicitScan) {
    return []
  }

  return extractQuotedStrings(explicitScan).sort()
}

function extractAnnotation(source, annotationName) {
  const annotationPattern = new RegExp(`@${annotationName}\\b`, 'g')
  const match = annotationPattern.exec(source)
  if (!match) {
    return null
  }

  let cursor = annotationPattern.lastIndex
  while (/\s/.test(source[cursor] ?? '')) {
    cursor += 1
  }

  let argumentsText = ''
  if (source[cursor] === '(') {
    argumentsText = readBalancedParentheses(source, cursor).text
  }

  return { name: annotationName, argumentsText }
}

async function directoryExists(directoryPath) {
  try {
    const directoryStat = await stat(directoryPath)
    return directoryStat.isDirectory()
  } catch (error) {
    if (error.code === 'ENOENT') {
      return false
    }
    throw error
  }
}

async function fileExists(filePath) {
  try {
    const fileStat = await stat(filePath)
    return fileStat.isFile()
  } catch (error) {
    if (error.code === 'ENOENT') {
      return false
    }
    throw error
  }
}

function buildBlockers(oldCounts, currentCounts, compatibilityTests, routeGapSummary, bootCutover, runCommandCutover, packagedRuntimeCutover, productionPreflightCutover, activeRuntimeRunbookCutover, serviceNameCompatibility) {
  const blockers = []
  if (!oldCounts.present) {
    blockers.push(`old canvas-engine web source path is missing: ${oldWebPath}`)
  }
  if (!currentCounts.present) {
    blockers.push(`canvas-web source path is missing: ${canvasWebSourcePath}`)
  }
  if (routeGapSummary.candidateCount > 0) {
    blockers.push(`route gap candidates remain: ${routeGapSummary.candidateCount}`)
  }
  if (currentCounts.endpointCount < oldCounts.endpointCount) {
    blockers.push(`canvas-web endpoint count ${currentCounts.endpointCount} is below old canvas-engine web endpoint count ${oldCounts.endpointCount}`)
  }
  if (!bootCutover.pom.present) {
    blockers.push(`canvas-boot pom is missing: ${canvasBootPomPath}`)
  } else {
    if (!bootCutover.pom.dependsOnCanvasWeb) {
      blockers.push('canvas-boot does not depend on final canvas-web module')
    }
    if (bootCutover.pom.dependsOnCanvasEngine) {
      blockers.push('canvas-boot still depends on legacy canvas-engine module')
    }
  }
  if (!bootCutover.application.present) {
    blockers.push(`canvas-boot application is missing: ${canvasBootApplicationPath}`)
  } else if (!bootCutover.application.scansOrgChovyCanvas) {
    blockers.push('canvas-boot application does not scan org.chovy.canvas')
  }
  for (const runCommandFile of runCommandCutover.requiredFiles) {
    if (!runCommandFile.present) {
      blockers.push(`run-command cutover file is missing: ${runCommandFile.file}`)
      continue
    }
    if (!runCommandFile.startsCanvasBoot) {
      blockers.push(`run-command cutover file does not start canvas-boot: ${runCommandFile.file}`)
    }
    if (runCommandFile.startsCanvasEngine) {
      blockers.push(`run-command cutover file still starts canvas-engine: ${runCommandFile.file}`)
    }
    if (runCommandFile.usesLegacyCanvasEngineMavenModule) {
      blockers.push(`run-command cutover file still uses legacy canvas-engine Maven module: ${runCommandFile.file}`)
    }
    if (runCommandFile.referencesLegacyEngineMigrationPath) {
      blockers.push(`run-command cutover file still documents legacy canvas-engine migration path: ${runCommandFile.file}`)
    }
  }
  for (const runtimeFile of packagedRuntimeCutover.requiredFiles) {
    if (!runtimeFile.present) {
      blockers.push(`packaged runtime cutover file is missing: ${runtimeFile.file}`)
      continue
    }
    if (!runtimeFile.usesCanvasBootArtifact) {
      blockers.push(`packaged runtime cutover file does not use canvas-boot artifact: ${runtimeFile.file}`)
    }
    if (runtimeFile.usesCanvasEngineArtifact) {
      blockers.push(`packaged runtime cutover file still uses canvas-engine artifact: ${runtimeFile.file}`)
    }
    if (runtimeFile.usesLegacyCanvasEngineMavenModule) {
      blockers.push(`CI workflow still runs legacy canvas-engine Maven module: ${runtimeFile.file}`)
    }
    if (runtimeFile.file === '.github/workflows/ci.yml' && !runtimeFile.hasBootRuntimeTestGate) {
      blockers.push('CI workflow does not run canvas-boot runtime gate tests')
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.hasHelmRenderGate) {
      blockers.push(`CI workflow does not run Helm render gate: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.hasBootFlywayMigrationPolicyGate) {
      blockers.push(`CI workflow does not run canvas-boot Flyway migration policy gate: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.hasProgramCoordinationGate) {
      blockers.push(`CI workflow does not run DDD cutover preflight gate: ${runtimeFile.file}`)
    }
  }
  for (const productionFile of productionPreflightCutover.requiredFiles) {
    if (!productionFile.present) {
      blockers.push(`production preflight cutover file is missing: ${productionFile.file}`)
      continue
    }
    if (productionFile.anchorsLegacyEngineSource) {
      blockers.push(`production preflight still anchors legacy canvas-engine source: ${productionFile.file}`)
    }
    if (!productionFile.checksHelmPipelineContract) {
      blockers.push(`production preflight does not verify Helm/static/runbook pipeline contract: ${productionFile.file}`)
    }
  }
  for (const runbookFile of activeRuntimeRunbookCutover.requiredFiles) {
    if (!runbookFile.present) {
      blockers.push(`active runtime runbook is missing: ${runbookFile.file}`)
      continue
    }
    if (!runbookFile.usesCanvasBootRuntime) {
      blockers.push(`active runtime runbook does not reference canvas-boot runtime: ${runbookFile.file}`)
    }
    if (runbookFile.usesLegacyCanvasEngineMavenModule) {
      blockers.push(`active runtime runbook still uses legacy canvas-engine Maven module: ${runbookFile.file}`)
    }
    if (runbookFile.usesLegacyCanvasEnginePerfImage) {
      blockers.push(`active runtime runbook still uses legacy canvas-engine perf image: ${runbookFile.file}`)
    }
  }
  if (!serviceNameCompatibility.helmValues.present) {
    blockers.push(`service-name compatibility Helm values file is missing: ${serviceNameCompatibility.helmValues.path}`)
  } else {
    if (!serviceNameCompatibility.helmValues.usesCanvasBootImageRepository) {
      blockers.push('Helm backend image repository is not cut over to canvas-boot')
    }
    if (!serviceNameCompatibility.helmValues.keepsCanvasEngineBackendName) {
      blockers.push('Helm backend.name must remain canvas-engine until service/DNS compatibility cutover')
    }
    if (!serviceNameCompatibility.helmValues.keepsCanvasEngineServiceAccount) {
      blockers.push('Helm serviceAccountName must remain canvas-engine until RBAC compatibility cutover')
    }
    if (!serviceNameCompatibility.helmValues.keepsCanvasEngineRuntimeSecret) {
      blockers.push('Helm runtime secret name must remain canvas-engine-runtime until secret compatibility cutover')
    }
  }

  const missingTests = compatibilityTests.required
    .filter((entry) => !entry.present)
    .map((entry) => entry.className)
  if (missingTests.length > 0) {
    blockers.push(`missing required compatibility tests: ${missingTests.join(', ')}`)
  }

  return blockers
}

function parseArgs(argv) {
  const options = {
    root: '.',
    requireReady: false,
    json: false
  }
  const positional = []

  for (const arg of argv) {
    if (arg === '--require-ready') {
      options.requireReady = true
    } else if (arg === '--json') {
      options.json = true
    } else if (arg === '--help' || arg === '-h') {
      options.help = true
    } else if (arg.startsWith('--')) {
      throw new Error(`Unknown option: ${arg}`)
    } else {
      positional.push(arg)
    }
  }

  if (positional.length > 1) {
    throw new Error(`Expected at most one root directory, received ${positional.length}`)
  }
  if (positional.length === 1) {
    options.root = positional[0]
  }

  return options
}

function printUsage() {
  console.log(`Usage: node tools/program-coordination/cutover-compatibility-preflight.mjs [root] [--json] [--require-ready]

Prints a deterministic JSON readiness report for DDD-C09 cutover compatibility.
Default mode prints JSON and exits 0 even when cutoverReady is false.
--require-ready exits 1 when cutoverReady is false.`)
}

async function main() {
  let options
  try {
    options = parseArgs(process.argv.slice(2))
  } catch (error) {
    console.error(error.message)
    process.exitCode = 2
    return
  }

  if (options.help) {
    printUsage()
    return
  }

  const report = await buildReport(options.root)
  console.log(`${JSON.stringify(report, null, 2)}\n`)

  if (options.requireReady && !report.cutoverReady) {
    process.exitCode = 1
  }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.stack ?? error.message)
    process.exitCode = 2
  })
}
