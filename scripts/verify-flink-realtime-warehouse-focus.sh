#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
ENGINE_DIR="${BACKEND_DIR}/canvas-engine"

if [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$({ /usr/libexec/java_home -v 21; } 2>/dev/null || true)"
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "ERROR: JAVA_HOME is not set and Java 21 could not be discovered" >&2
  exit 1
fi

cd "${BACKEND_DIR}"
mvn -pl canvas-flink-jobs package

cd "${ROOT_DIR}"
bash -n scripts/verify-flink-realtime-warehouse-live.sh

cd "${ROOT_DIR}"
scripts/verify-enterprise-olap-focus.sh

cd "${BACKEND_DIR}"
mvn -pl canvas-engine -DincludeScope=test "-Dmdep.outputFile=${ENGINE_DIR}/target/test-classpath.txt" dependency:build-classpath

mkdir -p "${ENGINE_DIR}/target/classes" "${ENGINE_DIR}/target/test-classes"
TEST_CP="$(cat "${ENGINE_DIR}/target/test-classpath.txt"):${ENGINE_DIR}/target/classes:${ENGINE_DIR}/target/test-classes"
"${JAVA_HOME}/bin/javac" --release 21 \
  -cp "${TEST_CP}" \
  -sourcepath canvas-engine/src/main/java \
  -d "${ENGINE_DIR}/target/classes" \
  canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyntheticDataPathProbeRunDO.java \
  canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyntheticDataPathProbeRunMapper.java \
  canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeService.java \
  canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java \
  canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java \
  canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessService.java \
  canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeController.java \
  canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeCutoverReadinessController.java
"${JAVA_HOME}/bin/javac" --release 21 \
  -cp "${TEST_CP}" \
  -sourcepath canvas-engine/src/main/java:canvas-engine/src/test/java \
  -d "${ENGINE_DIR}/target/test-classes" \
  canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeSchemaTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeSourceModeTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateSourceModeTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessServiceTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest.java \
  canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeCutoverReadinessControllerTest.java
mvn -pl canvas-engine \
  -Dtest=CdpWarehouseSyntheticDataPathProbeSchemaTest,CdpWarehouseSyntheticDataPathProbeSourceModeTest,CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest,CdpWarehouseE2eCertificationGateSourceModeTest,CdpWarehouseRealtimeCutoverReadinessServiceTest,CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest,CdpWarehouseRealtimeCutoverReadinessControllerTest \
  -DfailIfNoTests=false \
  test

cd "${ROOT_DIR}"
git diff --check
