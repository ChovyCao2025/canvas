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
  'docs/runbooks/flink-realtime-warehouse.md',
  'docs/runbooks/flink-production-deployment.md',
  'docs/runbooks/flyway-migration-history-repair-2026-06-06.md'
]
const serviceNameCompatibilityPolicyPath = 'docs/runbooks/backend-service-name-cutover.md'
const helmCompatibilityValuesFiles = [
  'deploy/helm/canvas/values.yaml',
  'deploy/helm/canvas/values-staging.yaml',
  'deploy/helm/canvas/values-prod.yaml'
]
const frontendPackagePath = 'frontend/package.json'
const frontendPackageLockPath = 'frontend/package-lock.json'
const requiredFrontendNodeEngine = '^20.19.0 || >=22.12.0'
const requiredCiNodeVersion = '20.19.0'
const requiredFrontendNodeDocText = 'Node.js 20.19+ or 22.12+'
const frontendNodeVersionDocFiles = [
  'AGENTS.md',
  'CONTRIBUTING.md',
  'docs/open-source/quickstart.md',
  'docs/open-source/en/quickstart.md'
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
  const frontendToolchain = await inspectFrontendToolchain(root, packagedRuntimeCutover)
  const productionPreflightCutover = await inspectProductionPreflightCutover(root)
  const activeRuntimeRunbookCutover = await inspectActiveRuntimeRunbookCutover(root)
  const serviceNameCompatibility = await inspectServiceNameCompatibility(root)
  const routeGapSummary = buildRouteGapSummary(oldCounts.controllers, currentCounts.controllers)
  const blockers = buildBlockers(oldCounts, currentCounts, compatibilityTests, routeGapSummary, bootCutover, runCommandCutover, packagedRuntimeCutover, frontendToolchain, productionPreflightCutover, activeRuntimeRunbookCutover, serviceNameCompatibility)

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
    frontendToolchain,
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
  const classConditionSignature = extractClassConditionSignature(header)
  const mappings = extractMappingAnnotations(body)
  const endpoints = []

  for (const mapping of mappings) {
    const methodPaths = extractMappingPaths(mapping.argumentsText)
    const httpMethods = extractHttpMethods(mapping.name, mapping.argumentsText)
    const conditionSignature = mergeMappingConditionSignatures(
      classConditionSignature,
      extractMappingConditionSignature(mapping.argumentsText)
    )
    for (const classPath of classPaths) {
      for (const methodPath of methodPaths) {
        for (const httpMethod of httpMethods) {
          endpoints.push({
            httpMethod,
            path: joinRoutePaths(classPath, methodPath),
            conditionSignature
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

function extractClassConditionSignature(header) {
  const mappings = extractMappingAnnotations(header).filter((mapping) => mapping.name === 'RequestMapping')
  if (mappings.length === 0) {
    return ''
  }
  return extractMappingConditionSignature(mappings[mappings.length - 1].argumentsText)
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

function extractMappingConditionSignature(argumentsText) {
  const parts = []
  for (const attribute of ['params', 'headers', 'consumes', 'produces']) {
    const expression = readNamedAttributeValue(argumentsText, attribute)
    if (expression == null) {
      continue
    }
    const values = normalizeAnnotationConditionValues(expression)
    parts.push(`${attribute}=${values.join('|')}`)
  }
  return parts.join(';')
}

function normalizeAnnotationConditionValues(expression) {
  const quoted = extractQuotedStrings(expression)
  if (quoted.length > 0) {
    return quoted.sort()
  }
  const normalized = expression
    .replace(/^\{\s*|\s*}$/g, '')
    .split(',')
    .map((value) => value.trim().replace(/\s+/g, ' '))
    .filter(Boolean)
    .sort()
  return normalized.length > 0 ? normalized : ['']
}

function mergeMappingConditionSignatures(...signatures) {
  return signatures
    .filter(Boolean)
    .flatMap((signature) => signature.split(';').filter(Boolean))
    .sort()
    .join(';')
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
  const currentEndpoints = currentControllers.flatMap((controller) => controller.endpoints)

  return oldControllers
    .flatMap((controller) => {
      if (controller.endpoints.length === 0) {
        return [controller]
      }

      const endpoints = controller.endpoints.filter((endpoint) => !isEndpointCoveredByCurrent(endpoint, currentEndpoints))
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

function isEndpointCoveredByCurrent(oldEndpoint, currentEndpoints) {
  const oldPath = normalizeRoutePathVariables(oldEndpoint.path)
  const comparableEndpoints = currentEndpoints.filter((currentEndpoint) => (
    currentEndpoint.httpMethod === oldEndpoint.httpMethod &&
    normalizeRoutePathVariables(currentEndpoint.path) === oldPath
  ))
  if (comparableEndpoints.length === 0) {
    return false
  }

  const oldConditions = parseConditionSignature(oldEndpoint.conditionSignature)
  const matchingFixedConditionEndpoints = comparableEndpoints.filter((currentEndpoint) => {
    const currentConditions = parseConditionSignature(currentEndpoint.conditionSignature)
    return ['headers', 'consumes', 'produces'].every((attribute) => (
      conditionValuesEqual(oldConditions.get(attribute), currentConditions.get(attribute))
    ))
  })

  if (matchingFixedConditionEndpoints.some((currentEndpoint) => (
    conditionValuesEqual(
      oldConditions.get('params'),
      parseConditionSignature(currentEndpoint.conditionSignature).get('params')
    )
  ))) {
    return true
  }

  const oldParams = oldConditions.get('params')
  if (oldParams && oldParams.length > 0) {
    return false
  }

  return hasCurrentParamsPartitionCoverage(matchingFixedConditionEndpoints)
}

function hasCurrentParamsPartitionCoverage(currentEndpoints) {
  const partitionValuesByName = new Map()

  for (const currentEndpoint of currentEndpoints) {
    const currentParams = parseConditionSignature(currentEndpoint.conditionSignature).get('params') ?? []
    if (currentParams.length !== 1) {
      continue
    }

    const parsed = parseSimpleParamCondition(currentParams[0])
    if (!parsed) {
      continue
    }

    const values = partitionValuesByName.get(parsed.name) ?? new Set()
    values.add(parsed.negated ? 'negated' : 'present')
    partitionValuesByName.set(parsed.name, values)
  }

  return [...partitionValuesByName.values()].some((values) => (
    values.has('present') && values.has('negated')
  ))
}

function parseSimpleParamCondition(paramCondition) {
  const trimmed = paramCondition.trim()
  const match = trimmed.match(/^(!)?([A-Za-z_$][\w$.-]*)$/u)
  if (!match) {
    return null
  }
  return {
    negated: Boolean(match[1]),
    name: match[2]
  }
}

function parseConditionSignature(signature) {
  const conditions = new Map()
  if (!signature) {
    return conditions
  }

  for (const part of signature.split(';').filter(Boolean)) {
    const separatorIndex = part.indexOf('=')
    if (separatorIndex === -1) {
      continue
    }
    const attribute = part.slice(0, separatorIndex)
    const values = part.slice(separatorIndex + 1).split('|').filter(Boolean)
    conditions.set(attribute, [
      ...(conditions.get(attribute) ?? []),
      ...values
    ].sort())
  }
  return conditions
}

function conditionValuesEqual(left = [], right = []) {
  if (left.length !== right.length) {
    return false
  }
  return left.every((value, index) => value === right[index])
}

function endpointRouteKey(endpoint) {
  const baseKey = `${endpoint.httpMethod} ${normalizeRoutePathVariables(endpoint.path)}`
  return endpoint.conditionSignature ? `${baseKey} ${endpoint.conditionSignature}` : baseKey
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
  const helmValuesPath = 'deploy/helm/canvas/values.yaml'
  const helmValuesFile = path.join(root, helmValuesPath)
  const helmValues = await fileExists(helmValuesFile) ? await readFile(helmValuesFile, 'utf8') : ''
  const helmBackendImageRepository = readNestedYamlScalar(helmValues, ['backend', 'image'], 'repository')
  const helmBackendSecretName = readSimpleYamlScalar(helmValues, 'backend', 'secretName')
  const requiredFiles = await Promise.all(requiredPackagedRuntimeFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    const workflow = file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
      ? inspectWorkflowGateJobs(content, file)
      : undefined
	    const finalBuildJob = file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
	      ? workflowFinalImageBuildJob(content, file)
	      : { needs: [], canRunAfterFailedNeeds: false }
	    const finalBuildNeeds = finalBuildJob.needs
	    const releaseRunbookImageRepositories = file === 'docs/architecture/evidence/runbooks/release-deployment.md'
	      ? readReleaseRunbookImageRepositories(content)
	      : []
		    return {
	      file,
	      present,
	      usesCanvasBootArtifact: content.includes('canvas-boot'),
	      usesCanvasEngineArtifact: /canvas-engine(?:\/pom\.xml|-\*\.jar|-1\.0\.0-SNAPSHOT\.jar|\/target\/canvas-engine| -am package|\/Dockerfile)/.test(content),
	      ...(file === 'docs/architecture/evidence/runbooks/release-deployment.md'
		        ? {
		            appliesStaticCanvasEngineManifests: /^\s*(?:kubectl\s+apply[^\n]*deploy\/k8s\/canvas-engine-|envsubst\s+<\s*deploy\/k8s\/canvas-engine-)/m.test(content),
		            usesHelmReleasePath: /helm\s+(?:upgrade|template)/.test(content) && /deploy\/helm\/canvas/.test(content),
		            releaseRunbookImageRepositories,
		            releaseRunbookImageRepository: releaseRunbookImageRepositories[0],
		            mismatchedReleaseRunbookImageRepositories: releaseRunbookImageRepositories.filter((repository) => repository !== helmBackendImageRepository),
		            matchesHelmImageRepository: releaseRunbookImageRepositories.length > 0 &&
		              releaseRunbookImageRepositories.every((repository) => repository === helmBackendImageRepository),
		            releaseRunbookSecretName: readKubectlCreateSecretName(content),
		            matchesHelmRuntimeSecret: readKubectlCreateSecretName(content) === helmBackendSecretName,
		            documentsStableServiceAccountPrerequisite: documentsStableServiceAccountPrerequisite(content)
		          }
		        : {}),
	      usesLegacyCanvasEngineMavenModule: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
	        ? usesMavenModuleCommand(content, 'canvas-engine')
	        : undefined,
      hasBootRuntimeTestGate: file === '.github/workflows/ci.yml'
        ? workflow.hasBootRuntimeTestGate
        : undefined,
      hasHelmRenderGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.hasHelmRenderGate
        : undefined,
      hasBootFlywayMigrationPolicyGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.hasBootFlywayMigrationPolicyGate &&
          !content.includes('-pl canvas-engine -am -Dtest=FlywayMigrationPolicyTest')
        : undefined,
      hasProgramCoordinationGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.hasProgramCoordinationGate
        : undefined,
      hasReleaseDryRunGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.hasReleaseDryRunGate
        : undefined,
      hasProductionFlinkDeploymentPreflightGate: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.hasProductionFlinkDeploymentPreflightGate
        : undefined,
      requiredGateJobsAllowFailure: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? workflow.requiredGateJobsAllowFailure
        : undefined,
      finalBuildDependsOnRequiredGates: file === '.github/workflows/ci.yml'
        ? finalBuildNeeds.includes('open-source-growth-guardrails') &&
          finalBuildNeeds.includes('cutover-preflight') &&
          finalBuildNeeds.includes('helm-render') &&
          finalBuildNeeds.includes('release-dry-run') &&
          finalBuildNeeds.includes('migration-validation')
        : file === '.github/workflows/canvas-ci.yml'
          ? finalBuildNeeds.includes('open-source-growth-guardrails') &&
            finalBuildNeeds.includes('cutover-preflight') &&
            finalBuildNeeds.includes('deployment-config') &&
            finalBuildNeeds.includes('flyway-migration-policy')
        : undefined,
      finalBuildDependsOnBackendValidationGates: file === '.github/workflows/ci.yml'
        ? finalBuildNeeds.includes('backend-test') &&
          finalBuildNeeds.includes('boot-runtime-test')
        : file === '.github/workflows/canvas-ci.yml'
          ? finalBuildNeeds.includes('backend-tests') &&
            finalBuildNeeds.includes('backend-integration-tests') &&
            finalBuildNeeds.includes('profile-validation')
        : undefined,
      finalBuildDependsOnFrontendValidationGates: file === '.github/workflows/ci.yml'
        ? finalBuildNeeds.includes('frontend-test') &&
          finalBuildNeeds.includes('frontend-build')
        : file === '.github/workflows/canvas-ci.yml'
          ? finalBuildNeeds.includes('frontend-tests')
        : undefined,
      finalBuildCanRunAfterFailedRequiredGates: file === '.github/workflows/ci.yml' || file === '.github/workflows/canvas-ci.yml'
        ? finalBuildJob.canRunAfterFailedNeeds
        : undefined
    }
  }))

  return { requiredFiles }
}

async function inspectFrontendToolchain(root) {
  const packageFile = path.join(root, frontendPackagePath)
  const lockFile = path.join(root, frontendPackageLockPath)
  const packagePresent = await fileExists(packageFile)
  const lockPresent = await fileExists(lockFile)
  const packageJson = packagePresent ? JSON.parse(await readFile(packageFile, 'utf8')) : {}
  const lockJson = lockPresent ? JSON.parse(await readFile(lockFile, 'utf8')) : {}
  const workflowFiles = await Promise.all(
    ['.github/workflows/ci.yml', '.github/workflows/canvas-ci.yml'].map(async (file) => {
      const absolutePath = path.join(root, file)
      const present = await fileExists(absolutePath)
      const content = present ? await readFile(absolutePath, 'utf8') : ''
      const nodeVersions = extractWorkflowNodeVersions(content)
      const declaresNodeRuntime = /actions\/setup-node/.test(content) || nodeVersions.length > 0
      return {
        file,
        present,
        nodeVersions,
        usesRequiredNodeVersion: !declaresNodeRuntime ||
          (nodeVersions.length > 0 && nodeVersions.every((version) => version === requiredCiNodeVersion))
      }
    })
  )
  const documentationFiles = await Promise.all(frontendNodeVersionDocFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return {
      file,
      present,
      documentsRequiredNodeVersion: content.includes(requiredFrontendNodeDocText)
    }
  }))

  return {
    packageJson: {
      path: frontendPackagePath,
      present: packagePresent,
      nodeEngine: packageJson.engines?.node,
      usesRequiredNodeEngine: packageJson.engines?.node === requiredFrontendNodeEngine
    },
    packageLock: {
      path: frontendPackageLockPath,
      present: lockPresent,
      nodeEngine: lockJson.packages?.['']?.engines?.node,
      usesRequiredNodeEngine: lockJson.packages?.['']?.engines?.node === requiredFrontendNodeEngine
    },
    workflowFiles,
    documentationFiles
  }
}

function workflowFinalImageBuildJob(content, file) {
  const jobs = extractWorkflowJobs(content)
  if (jobs.length === 0) {
    return {
      needs: extractLooseWorkflowNeeds(content),
      canRunAfterFailedNeeds: false
    }
  }
  const nonFinalImageBuildJobNames = new Set([
    file === '.github/workflows/ci.yml' ? 'release-dry-run' : 'deployment-config'
  ])
  const finalBuildJob = jobs.find((job) => {
    if (nonFinalImageBuildJobNames.has(job.name)) {
      return false
    }
    return extractWorkflowRunCommands(job.body).some((command) => (
      isCanvasBootImageBuildCommand(command, { dryRun: false })
    ))
  })

  return {
    needs: finalBuildJob ? extractWorkflowNeeds(finalBuildJob.body) : [],
    canRunAfterFailedNeeds: finalBuildJob ? workflowJobHasAlwaysCondition(finalBuildJob.body) : false
  }
}

function inspectWorkflowGateJobs(content, file) {
  const jobs = extractWorkflowJobs(content)
  if (jobs.length === 0) {
    return inspectLooseWorkflowGates(content)
  }
  const jobByName = new Map(jobs.map((job) => [job.name, job]))
  const requiredGateJobNames = workflowRequiredGateJobNames(file)
  const gateJobName = file === '.github/workflows/ci.yml' ? 'release-dry-run' : 'deployment-config'
  const flywayJobName = file === '.github/workflows/ci.yml' ? 'migration-validation' : 'flyway-migration-policy'
  const helmJobName = file === '.github/workflows/ci.yml' ? 'helm-render' : 'deployment-config'

  return {
    hasBootRuntimeTestGate: file === '.github/workflows/ci.yml'
      ? jobRunsCommand(jobByName.get('boot-runtime-test'), (command) => (
        commandIncludesAll(command, ['-pl canvas-boot', 'ModularArchitectureTest', 'CanvasBootApplicationSmokeTest'])
      ))
      : undefined,
    hasHelmRenderGate: jobRunsCommand(jobByName.get(helmJobName), (command) => (
      command === 'bash scripts/release/verify-helm-render.sh'
    )),
    hasBootFlywayMigrationPolicyGate: jobRunsCommands(jobByName.get(flywayJobName), [
      (command) => command === 'bash scripts/release/check-flyway-migration.sh',
      (command) => commandIncludesAll(command, ['-pl canvas-boot', 'FlywayMigrationPolicyTest']) &&
        !command.includes('-pl canvas-engine')
    ]),
    hasProgramCoordinationGate: jobRunsCommands(jobByName.get('cutover-preflight'), [
      (command) => command === 'bash docs/program-coordination/checks/program-coordination-checks.sh .',
      (command) => command === 'node tools/program-coordination/check-dispatch-state.mjs .',
      (command) => command === 'node --test tools/program-coordination/*.test.mjs',
      (command) => command === 'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready'
    ]),
    hasReleaseDryRunGate: jobRunsCommands(jobByName.get(gateJobName), [
      (command) => command === 'bash -n scripts/release/*.sh',
      (command) => isCanvasBootImageBuildCommand(command, { dryRun: true }),
      (command) => command === 'bash scripts/release/pre-deploy-check.sh --dry-run',
      (command) => command === 'bash scripts/release/post-deploy-check.sh --dry-run',
      (command) => command === 'bash scripts/release/rollback-drill.sh --dry-run'
    ]),
    hasProductionFlinkDeploymentPreflightGate: jobRunsCommand(jobByName.get(gateJobName), (command) => (
      command === 'bash scripts/verify-flink-production-deployment.sh'
    )),
    requiredGateJobsAllowFailure: requiredGateJobNames.filter((jobName) => (
      workflowJobContinuesOnError(jobByName.get(jobName)?.body ?? '')
    ))
  }
}

function inspectLooseWorkflowGates(content) {
  return {
    hasBootRuntimeTestGate: content.includes('-pl canvas-boot') &&
      content.includes('ModularArchitectureTest') &&
      content.includes('CanvasBootApplicationSmokeTest'),
    hasHelmRenderGate: content.includes('azure/setup-helm') &&
      content.includes('scripts/release/verify-helm-render.sh'),
    hasBootFlywayMigrationPolicyGate: content.includes('scripts/release/check-flyway-migration.sh') &&
      content.includes('-pl canvas-boot') &&
      content.includes('FlywayMigrationPolicyTest'),
    hasProgramCoordinationGate: content.includes('bash docs/program-coordination/checks/program-coordination-checks.sh .') &&
      content.includes('tools/program-coordination/check-dispatch-state.mjs') &&
      content.includes('node --test tools/program-coordination/*.test.mjs') &&
      content.includes('tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready'),
    hasReleaseDryRunGate: content.includes('bash -n scripts/release/*.sh') &&
      content.includes('scripts/release/build-image.sh --dry-run --image canvas-boot') &&
      content.includes('scripts/release/pre-deploy-check.sh --dry-run') &&
      content.includes('scripts/release/post-deploy-check.sh --dry-run') &&
      content.includes('scripts/release/rollback-drill.sh --dry-run'),
    hasProductionFlinkDeploymentPreflightGate: content.includes('scripts/verify-flink-production-deployment.sh'),
    requiredGateJobsAllowFailure: []
  }
}

function workflowRequiredGateJobNames(file) {
  if (file === '.github/workflows/ci.yml') {
    return [
      'backend-test',
      'boot-runtime-test',
      'frontend-test',
      'frontend-build',
      'open-source-growth-guardrails',
      'cutover-preflight',
      'helm-render',
      'release-dry-run',
      'migration-validation'
    ]
  }
  if (file === '.github/workflows/canvas-ci.yml') {
    return [
      'backend-tests',
      'backend-integration-tests',
      'frontend-tests',
      'profile-validation',
      'open-source-growth-guardrails',
      'cutover-preflight',
      'deployment-config',
      'flyway-migration-policy'
    ]
  }
  return []
}

function jobRunsCommands(job, predicates) {
  const commands = extractWorkflowRunCommands(job?.body ?? '')
  return predicates.every((predicate) => commands.some(predicate))
}

function jobRunsCommand(job, predicate) {
  return jobRunsCommands(job, [predicate])
}

function extractWorkflowRunCommands(jobBody) {
  return jobBody
    .split(/\r?\n/)
    .map((line) => line.match(/^\s{6,}-\s+run:\s*(.*)$/) ?? line.match(/^\s{8,}run:\s*(.*)$/))
    .filter(Boolean)
    .map((match) => normalizeWorkflowScalar(match[1]))
    .filter((command) => !/^echo(?:\s|$)/.test(command))
}

function extractWorkflowNodeVersions(content) {
  return [...content.matchAll(/^\s*node-version:\s*(.*)$/gm)]
    .map((match) => normalizeWorkflowScalar(match[1]))
}

function normalizeWorkflowScalar(value) {
  const trimmed = value.trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function commandIncludesAll(command, parts) {
  return parts.every((part) => command.includes(part))
}

function isCanvasBootImageBuildCommand(command, { dryRun }) {
  const normalized = stripShellComments(command)
    .replace(/^(?:[A-Za-z_][A-Za-z0-9_]*=(?:\$\{\{[^}]+}}|[^ ]+)\s+)+/, '')
    .trim()
  return (
    (
      normalized.startsWith('bash scripts/release/build-image.sh ') ||
      normalized.startsWith('scripts/release/build-image.sh ')
    ) &&
    normalized.includes('--image canvas-boot') &&
    normalized.includes('--dry-run') === dryRun
  )
}

function stripShellComments(command) {
  let quote = null
  let escaped = false

  for (let index = 0; index < command.length; index += 1) {
    const character = command[index]

    if (escaped) {
      escaped = false
      continue
    }
    if (character === '\\' && quote !== "'") {
      escaped = true
      continue
    }
    if (quote) {
      if (character === quote) {
        quote = null
      }
      continue
    }
    if (character === '"' || character === "'") {
      quote = character
      continue
    }
    if (character === '#') {
      return command.slice(0, index)
    }
  }

  return command
}

function workflowJobHasAlwaysCondition(jobBody) {
  return jobBody
    .split(/\r?\n/)
    .some((line) => {
      const condition = line.match(/^\s{4}if:\s*(.+)$/)
      return condition && /\balways\s*\(\s*\)/.test(normalizeWorkflowScalar(condition[1]))
    })
}

function workflowJobContinuesOnError(jobBody) {
  return jobBody
    .split(/\r?\n/)
    .some((line) => {
      const setting = line.match(/^\s{4}continue-on-error:\s*(.+)$/)
      return setting && normalizeWorkflowScalar(setting[1]).trim() !== 'false'
    })
}

function extractLooseWorkflowNeeds(content) {
  return content
    .split(/\r?\n/)
    .map((line) => line.match(/^\s*-\s*([A-Za-z0-9_-]+)\s*$/))
    .filter(Boolean)
    .map((match) => match[1])
}

function extractWorkflowJobs(content) {
  const lines = content.split(/\r?\n/)
  const jobs = []
  let inJobs = false
  let current = null

  for (const line of lines) {
    if (/^jobs:\s*$/.test(line)) {
      inJobs = true
      continue
    }
    if (!inJobs) {
      continue
    }
    if (/^[^ \t#][^:]*:\s*$/.test(line)) {
      break
    }

    const jobMatch = line.match(/^  ([A-Za-z0-9_-]+):\s*$/)
    if (jobMatch) {
      if (current) {
        jobs.push(current)
      }
      current = { name: jobMatch[1], body: '' }
      continue
    }
    if (current) {
      current.body += `${line}\n`
    }
  }

  if (current) {
    jobs.push(current)
  }
  return jobs
}

function extractWorkflowNeeds(jobBody) {
  const lines = jobBody.split(/\r?\n/)
  const needs = []

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const inline = line.match(/^\s{4}needs:\s*\[([^\]]*)\]\s*$/)
    if (inline) {
      return inline[1].split(',').map((entry) => entry.trim().replace(/^['"]|['"]$/g, '')).filter(Boolean)
    }

    const scalar = line.match(/^\s{4}needs:\s*([A-Za-z0-9_-]+)\s*$/)
    if (scalar) {
      return [scalar[1]]
    }

    if (/^\s{4}needs:\s*$/.test(line)) {
      for (let needsIndex = index + 1; needsIndex < lines.length; needsIndex += 1) {
        const needsLine = lines[needsIndex]
        if (/^\s{4}[A-Za-z0-9_-]+:/.test(needsLine)) {
          break
        }
        const listItem = needsLine.match(/^\s{6}-\s*([A-Za-z0-9_-]+)\s*$/)
        if (listItem) {
          needs.push(listItem[1])
        }
      }
      return needs
    }
  }

  return needs
}

async function inspectProductionPreflightCutover(root) {
  const requiredFiles = await Promise.all(requiredProductionPreflightFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    const activeContent = activeShellScriptContent(content)
    return {
      file,
      present,
      anchorsLegacyEngineSource: /backend\/canvas-engine\/src\/main\/java|canvas-engine\/src\/main\/java/.test(content),
      checksHelmPipelineContract: activeContent.includes('deploy/helm/canvas/values.yaml') &&
        activeContent.includes('deploy/k8s/canvas-flink-job-submitter.yaml') &&
        activeContent.includes('docs/runbooks/flink-production-deployment.md') &&
        activeContent.includes('--pipeline-key=${pipeline}')
    }
  }))

  return { requiredFiles }
}

function activeShellScriptContent(content) {
  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#') && !/^(?:echo|printf)(?:\s|$)/.test(line))
    .join('\n')
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
  const helmValueFiles = await Promise.all(helmCompatibilityValuesFiles.map(async (file) => {
    const absolutePath = path.join(root, file)
    const present = await fileExists(absolutePath)
    const content = present ? await readFile(absolutePath, 'utf8') : ''
    return inspectHelmCompatibilityValuesFile(file, present, content, file === helmValuesPath)
  }))
  const policyFile = path.join(root, serviceNameCompatibilityPolicyPath)
  const policyPresent = await fileExists(policyFile)
  const policy = policyPresent ? await readFile(policyFile, 'utf8') : ''

  return {
    helmValues: {
      path: helmValuesPath,
      present: helmValuesPresent,
      backendName: readSimpleYamlScalar(helmValues, 'backend', 'name'),
      imageRepository: readNestedYamlScalar(helmValues, ['backend', 'image'], 'repository'),
      imageTag: readNestedYamlScalar(helmValues, ['backend', 'image'], 'tag'),
      serviceAccountName: readSimpleYamlScalar(helmValues, 'backend', 'serviceAccountName'),
      secretName: readSimpleYamlScalar(helmValues, 'backend', 'secretName'),
      keepsCanvasEngineBackendName: readSimpleYamlScalar(helmValues, 'backend', 'name') === 'canvas-engine',
      usesCanvasBootImageRepository: readNestedYamlScalar(helmValues, ['backend', 'image'], 'repository')?.endsWith('/canvas-boot') === true,
      usesImmutableBackendImageTag: readNestedYamlScalar(helmValues, ['backend', 'image'], 'tag') !== 'latest',
      keepsCanvasEngineServiceAccount: readSimpleYamlScalar(helmValues, 'backend', 'serviceAccountName') === 'canvas-engine',
      keepsCanvasEngineRuntimeSecret: readSimpleYamlScalar(helmValues, 'backend', 'secretName') === 'canvas-engine-runtime'
    },
    helmValueFiles,
    policy: {
      path: serviceNameCompatibilityPolicyPath,
      present: policyPresent,
      documentsCanvasBootRuntimeImage: /canvas-boot/.test(policy),
      documentsStableCanvasEngineServiceNames: /canvas-engine/.test(policy) && /canvas-engine-runtime/.test(policy),
      requiresDnsCompatibilityBeforeRename: /DNS/.test(policy) && /compatibility/i.test(policy) && /before rename/i.test(policy),
      rejectsMechanicalRename: /mechanical rename/i.test(policy) && /do not/i.test(policy)
    }
  }
}

function inspectHelmCompatibilityValuesFile(file, present, content, requireBaseValues) {
  const backendName = readSimpleYamlScalar(content, 'backend', 'name')
  const imageRepository = readNestedYamlScalar(content, ['backend', 'image'], 'repository')
  const imageTag = readNestedYamlScalar(content, ['backend', 'image'], 'tag')
  const serviceAccountName = readSimpleYamlScalar(content, 'backend', 'serviceAccountName')
  const secretName = readSimpleYamlScalar(content, 'backend', 'secretName')
  return {
    file,
    present,
    backendName,
    imageRepository,
    imageTag,
    serviceAccountName,
    secretName,
    keepsCanvasEngineBackendName: requireBaseValues ? backendName === 'canvas-engine' : backendName === undefined || backendName === 'canvas-engine',
    usesCanvasBootImageRepository: requireBaseValues ? imageRepository?.endsWith('/canvas-boot') === true : imageRepository === undefined || imageRepository.endsWith('/canvas-boot'),
    usesImmutableBackendImageTag: imageTag !== 'latest',
    keepsCanvasEngineServiceAccount: requireBaseValues ? serviceAccountName === 'canvas-engine' : serviceAccountName === undefined || serviceAccountName === 'canvas-engine',
    keepsCanvasEngineRuntimeSecret: requireBaseValues ? secretName === 'canvas-engine-runtime' : secretName === undefined || secretName === 'canvas-engine-runtime'
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

function readKubectlCreateSecretName(content) {
  const match = content.match(/\bkubectl\b[^\n]*\bcreate\s+secret\s+generic\s+([A-Za-z0-9._-]+)/)
  return match?.[1]
}

function readReleaseRunbookImageRepositories(content) {
  return [...content.matchAll(/\bCANVAS_IMAGE_NAME=(?:"([^"\n]+)"|'([^'\n]+)'|([^\s\\\n]+))/g)]
    .map((match) => match[1] ?? match[2] ?? match[3])
}

function documentsStableServiceAccountPrerequisite(content) {
  return /service\s*account/i.test(content) &&
    /\bcanvas-engine\b/.test(content) &&
    /\bRBAC\b/i.test(content)
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

function buildBlockers(oldCounts, currentCounts, compatibilityTests, routeGapSummary, bootCutover, runCommandCutover, packagedRuntimeCutover, frontendToolchain, productionPreflightCutover, activeRuntimeRunbookCutover, serviceNameCompatibility) {
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
	    if (runtimeFile.file === 'docs/architecture/evidence/runbooks/release-deployment.md') {
	      if (runtimeFile.appliesStaticCanvasEngineManifests) {
	        blockers.push('release deployment runbook applies static canvas-engine manifests instead of Helm service-name policy')
	      }
		      if (!runtimeFile.usesHelmReleasePath) {
		        blockers.push('release deployment runbook does not use Helm release path for backend service-name policy')
		      }
		      if (!runtimeFile.releaseRunbookImageRepository) {
		        blockers.push('release deployment runbook does not set backend image repository')
		      } else if (!runtimeFile.matchesHelmImageRepository) {
		        blockers.push(`release deployment runbook uses image repositories ${runtimeFile.mismatchedReleaseRunbookImageRepositories.join(', ')} but Helm expects ${serviceNameCompatibility.helmValues.imageRepository}`)
		      }
		      if (!runtimeFile.releaseRunbookSecretName) {
		        blockers.push('release deployment runbook does not create backend runtime secret')
		      } else if (!runtimeFile.matchesHelmRuntimeSecret) {
		        blockers.push(`release deployment runbook creates secret ${runtimeFile.releaseRunbookSecretName} but Helm expects ${serviceNameCompatibility.helmValues.secretName}`)
		      }
		      if (!runtimeFile.documentsStableServiceAccountPrerequisite) {
		        blockers.push('release deployment runbook does not document stable canvas-engine ServiceAccount/RBAC prerequisite')
		      }
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
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.hasReleaseDryRunGate) {
      blockers.push(`CI workflow does not run release dry-run gates: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.hasProductionFlinkDeploymentPreflightGate) {
      blockers.push(`CI workflow does not run production Flink deployment preflight: ${runtimeFile.file}`)
    }
    for (const jobName of runtimeFile.requiredGateJobsAllowFailure ?? []) {
      blockers.push(`CI required gate job can continue after failure: ${runtimeFile.file} ${jobName}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.finalBuildDependsOnRequiredGates) {
      blockers.push(`CI final image build does not depend on required guardrail gates: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.finalBuildDependsOnBackendValidationGates) {
      blockers.push(`CI final image build does not depend on backend validation gates: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && !runtimeFile.finalBuildDependsOnFrontendValidationGates) {
      blockers.push(`CI final image build does not depend on frontend validation gates: ${runtimeFile.file}`)
    }
    if ((runtimeFile.file === '.github/workflows/ci.yml' || runtimeFile.file === '.github/workflows/canvas-ci.yml') && runtimeFile.finalBuildCanRunAfterFailedRequiredGates) {
      blockers.push(`CI final image build can run when required gates fail: ${runtimeFile.file}`)
    }
  }
  if (!frontendToolchain.packageJson.present) {
    blockers.push(`frontend package manifest is missing: ${frontendPackagePath}`)
  } else if (!frontendToolchain.packageJson.usesRequiredNodeEngine) {
    blockers.push(`frontend package.json must declare node engine ${requiredFrontendNodeEngine}`)
  }
  if (!frontendToolchain.packageLock.present) {
    blockers.push(`frontend package lock is missing: ${frontendPackageLockPath}`)
  } else if (!frontendToolchain.packageLock.usesRequiredNodeEngine) {
    blockers.push(`frontend package-lock must declare node engine ${requiredFrontendNodeEngine}`)
  }
  for (const workflowFile of frontendToolchain.workflowFiles) {
    if (!workflowFile.present) {
      continue
    }
    if (!workflowFile.usesRequiredNodeVersion) {
      blockers.push(`CI workflow must pin frontend-compatible Node ${requiredCiNodeVersion}: ${workflowFile.file}`)
    }
  }
  for (const documentationFile of frontendToolchain.documentationFiles) {
    if (!documentationFile.present) {
      blockers.push(`frontend Node version doc is missing: ${documentationFile.file}`)
      continue
    }
    if (!documentationFile.documentsRequiredNodeVersion) {
      blockers.push(`frontend Node version doc must mention ${requiredFrontendNodeDocText}: ${documentationFile.file}`)
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
  }
  for (const helmValuesFile of serviceNameCompatibility.helmValueFiles ?? []) {
    if (!helmValuesFile.present) {
      blockers.push(`service-name compatibility Helm values file is missing: ${helmValuesFile.file}`)
      continue
    }
    if (!helmValuesFile.usesCanvasBootImageRepository) {
      blockers.push(`Helm backend image repository is not cut over to canvas-boot: ${helmValuesFile.file}`)
    }
    if (!helmValuesFile.usesImmutableBackendImageTag) {
      blockers.push(`Helm backend image tag must not be latest: ${helmValuesFile.file}`)
    }
    if (!helmValuesFile.keepsCanvasEngineBackendName) {
      blockers.push(`Helm backend.name must remain canvas-engine until service/DNS compatibility cutover: ${helmValuesFile.file}`)
    }
    if (!helmValuesFile.keepsCanvasEngineServiceAccount) {
      blockers.push(`Helm serviceAccountName must remain canvas-engine until RBAC compatibility cutover: ${helmValuesFile.file}`)
    }
    if (!helmValuesFile.keepsCanvasEngineRuntimeSecret) {
      blockers.push(`Helm runtime secret name must remain canvas-engine-runtime until secret compatibility cutover: ${helmValuesFile.file}`)
    }
  }
  if (!serviceNameCompatibility.policy.present) {
    blockers.push(`service-name compatibility policy runbook is missing: ${serviceNameCompatibility.policy.path}`)
  } else {
    if (!serviceNameCompatibility.policy.documentsCanvasBootRuntimeImage) {
      blockers.push(`service-name compatibility policy does not document canvas-boot runtime image: ${serviceNameCompatibility.policy.path}`)
    }
    if (!serviceNameCompatibility.policy.documentsStableCanvasEngineServiceNames) {
      blockers.push(`service-name compatibility policy does not document stable canvas-engine service/secret names: ${serviceNameCompatibility.policy.path}`)
    }
    if (!serviceNameCompatibility.policy.requiresDnsCompatibilityBeforeRename) {
      blockers.push(`service-name compatibility policy does not require DNS compatibility before rename: ${serviceNameCompatibility.policy.path}`)
    }
    if (!serviceNameCompatibility.policy.rejectsMechanicalRename) {
      blockers.push(`service-name compatibility policy does not reject mechanical service-name renames: ${serviceNameCompatibility.policy.path}`)
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
