#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
ENGINE_DIR="${BACKEND_DIR}/canvas-engine"

TEST_SELECTOR="ApplicationYamlTest,CdpWarehouseEnterpriseOlapReadinessServiceTest,CdpWarehouseProductionReadinessProofServiceTest,CdpWarehouseDorisPrometheusMetricsParserTest,CdpWarehouseEnterpriseOlapEvidenceServiceTest,CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest,CdpWarehouseEnterpriseOlapEvidenceSchedulerTest,CdpWarehouseEnterpriseOlapEvidenceControllerTest,HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest"
FOCUSED_MAIN_SOURCES=(
  "canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceDO.java"
  "canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.java"
  "canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceMapper.java"
  "canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.java"
  "canvas-engine/src/main/java/org/chovy/canvas/common/tenant/RoleNames.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParser.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapDorisEvidenceClient.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessService.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionService.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceScheduler.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java"
  "canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceController.java"
)
FOCUSED_TEST_SOURCES=(
  "canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationYamlTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParserTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceSchedulerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceControllerTest.java"
)

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

if [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$({ /usr/libexec/java_home -v 21; } 2>/dev/null || true)"
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  fail "JAVA_HOME is not set and Java 21 could not be discovered"
fi

cd "${BACKEND_DIR}"

if ! mvn -pl canvas-engine clean -DskipTests compile; then
  [[ -d "${ENGINE_DIR}/target/classes" ]] || fail "main compile failed and ${ENGINE_DIR}/target/classes is missing"
  echo "Main compile failed outside the enterprise OLAP focus slice; falling back to focused javac compile" >&2
fi
mvn -pl canvas-engine -DincludeScope=test "-Dmdep.outputFile=${ENGINE_DIR}/target/test-classpath.txt" dependency:build-classpath

mkdir -p "${ENGINE_DIR}/target/test-classes"
TEST_CP="$(cat "${ENGINE_DIR}/target/test-classpath.txt"):${ENGINE_DIR}/target/classes:${ENGINE_DIR}/target/test-classes"
"${JAVA_HOME}/bin/javac" --release 21 -cp "${TEST_CP}" -d "${ENGINE_DIR}/target/classes" "${FOCUSED_MAIN_SOURCES[@]}"
"${JAVA_HOME}/bin/javac" --release 21 -cp "${TEST_CP}" -d "${ENGINE_DIR}/target/test-classes" "${FOCUSED_TEST_SOURCES[@]}"

mvn -pl canvas-engine "-Dtest=${TEST_SELECTOR}" -DfailIfNoTests=false surefire:test
