#!/usr/bin/env node

import { readdir, readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const oldWebPath = 'backend/canvas-engine/src/main/java/org/chovy/canvas/web'
const canvasWebSourcePath = 'backend/canvas-web/src/main/java'
const compatibilityTestPath = 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'

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
  const blockers = buildBlockers(oldCounts, currentCounts, compatibilityTests)

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
    routeGapSummary: buildRouteGapSummary(oldCounts.controllers, currentCounts.controllers),
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
  const oldGroups = groupControllersByRoute(oldControllers, topLevelStats)
  const currentGroups = groupControllersByRoute(currentControllers, topLevelStats)
  const candidateGroups = [...oldGroups.keys()].filter((group) => {
    const oldGroup = oldGroups.get(group)
    const currentGroup = currentGroups.get(group) ?? emptyRouteGroup(group)
    return oldGroup.controllerFiles.size > currentGroup.controllerFiles.size ||
      oldGroup.endpointCount > currentGroup.endpointCount
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

function buildBlockers(oldCounts, currentCounts, compatibilityTests) {
  const blockers = []
  if (!oldCounts.present) {
    blockers.push(`old canvas-engine web source path is missing: ${oldWebPath}`)
  }
  if (!currentCounts.present) {
    blockers.push(`canvas-web source path is missing: ${canvasWebSourcePath}`)
  }
  if (currentCounts.controllerCount < oldCounts.controllerCount) {
    blockers.push(`canvas-web controller count ${currentCounts.controllerCount} is below old canvas-engine web controller count ${oldCounts.controllerCount}`)
  }
  if (currentCounts.endpointCount < oldCounts.endpointCount) {
    blockers.push(`canvas-web endpoint count ${currentCounts.endpointCount} is below old canvas-engine web endpoint count ${oldCounts.endpointCount}`)
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
