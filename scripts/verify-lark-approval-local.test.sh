#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-lark-approval-local.sh"

require_pattern() {
  local pattern="$1"
  local message="$2"
  if ! grep -Eq -- "$pattern" "$SCRIPT"; then
    echo "Missing local Lark approval verifier behavior: $message" >&2
    exit 1
  fi
}

require_pattern 'mvn -f "\$MODULE_DIR/pom.xml" test -Dtest="\$TESTS"' 'normal focused Maven test lifecycle'
require_pattern 'DEFAULT_JAVA_HOME="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home"' 'repository Java 21 default'
require_pattern 'CANVAS_JAVA_HOME' 'explicit Java override'
require_pattern 'export PATH="\$JAVA_HOME/bin:\$PATH"' 'Maven sees selected JDK first'
require_pattern 'ALLOW_TARGETED_TEST_COMPILE' 'explicit fallback opt-in'
require_pattern 'dependency:build-classpath' 'test classpath generation for targeted compile'
require_pattern 'domain/approval' 'targeted approval main source compilation'
require_pattern 'web/ApprovalController.java' 'targeted approval controller compilation'
require_pattern '-sourcepath "\$MODULE_DIR/src/main/java"' 'main sourcepath for targeted compilation'
require_pattern '-sourcepath "\$MODULE_DIR/src/test/java:\$MODULE_DIR/src/main/java"' 'test sourcepath for targeted compilation'
require_pattern '\$JAVA_HOME/bin/javac' 'targeted approval test compilation'
require_pattern 'surefire:test -Dtest="\$TESTS"' 'focused surefire execution after targeted compile'
require_pattern 'ApprovalLarkUserIdentityResolverTest,LarkApprovalProviderTest,HttpLarkApprovalClientTest,ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,CanvasPublishApprovalServiceTest' 'approval test set'

echo "verify-lark-approval-local tests passed"
