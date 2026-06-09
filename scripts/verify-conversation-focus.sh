#!/usr/bin/env bash
# Runs the focused conversation/SCRM verification suite.
#
# The script selects backend domain/controller tests and frontend presentation tests
# that cover provider adapters, routing, work items, AI replies, and operator UI.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
ENGINE_DIR="$BACKEND_DIR/canvas-engine"
FRONTEND_DIR="$ROOT_DIR/frontend"
ADAPTER_CONTRACT_TARGET_DIR="$ENGINE_DIR/target/test-classes/conversation/adapter-contracts"
FOCUSED_BACKEND_WORK_DIR="${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine"
FOCUSED_BACKEND_SOURCES_FILE="$FOCUSED_BACKEND_WORK_DIR/sources.txt"
BACKEND_TEST_CLASSPATH_FILE="$FOCUSED_BACKEND_WORK_DIR/test-classpath.txt"
BACKEND_LOMBOK_TARGET_CLASSES_STAMP="$FOCUSED_BACKEND_WORK_DIR/lombok-target-classes.stamp"
BACKEND_LOMBOK_TARGET_CLASSES_SOURCES_FILE="$FOCUSED_BACKEND_WORK_DIR/lombok-target-sources.txt"
BACKEND_LOMBOK_TARGET_CLASSES_ALL_SOURCES_FILE="$FOCUSED_BACKEND_WORK_DIR/main-java-target-sources.txt"
BACKEND_MAIN_RESOURCE_DIR="$ENGINE_DIR/src/main/resources"
BACKEND_TEST_RESOURCE_DIR="$ENGINE_DIR/src/test/resources"
ADAPTER_CONTRACT_FIXTURE_DIR="$BACKEND_TEST_RESOURCE_DIR/conversation/adapter-contracts"
BACKEND_MAIN_RESOURCE_TARGET_DIR="$ENGINE_DIR/target/classes"
BACKEND_TEST_RESOURCE_TARGET_DIR="$ENGINE_DIR/target/test-classes"
BACKEND_MAIN_RESOURCE_SYNC_ROOTS=(
  "application-prod.yml"
  "application-staging.yml"
  "application.yml"
  "db"
  "infrastructure"
  "logback-spring.xml"
  "mapper"
  "scripts"
)
BACKEND_TEST_RESOURCE_SYNC_ROOTS=(
  "conversation"
)
BACKEND_LOMBOK_CACHE_WATCH_MAIN_SOURCE_ROOTS=(
  "auth"
  "common"
  "config"
  "controller"
  "domain/conversation"
  "domain/demo"
  "engine/channel"
  "engine/context"
  "engine/delivery"
  "engine/handler"
  "engine/handlers"
  "engine/policy"
  "engine/trigger"
  "engine/wait"
  "infrastructure/mq"
  "web/ConversationController.java"
  "web/ConversationPrivateDomainController.java"
  "web/ConversationProviderWebhookController.java"
  "web/ConversationWorkspaceController.java"
  "web/DemoSandboxController.java"
  "web/PublicConversationWebhookController.java"
)
BACKEND_LOMBOK_TARGET_CLASS_SENTINELS=(
  "org/chovy/canvas/config/JwtAuthFilter.class"
  "org/chovy/canvas/dal/dataobject/MessageSendRecordDO.class"
  "org/chovy/canvas/engine/context/ExecutionContext.class"
  "org/chovy/canvas/engine/delivery/DeliveryOutboxDO.class"
  "org/chovy/canvas/engine/delivery/DeliveryOutboxDO\$DeliveryOutboxDOBuilder.class"
  "org/chovy/canvas/engine/delivery/DeliveryOutboxService.class"
  "org/chovy/canvas/engine/delivery/DeliveryReceiptLog.class"
  "org/chovy/canvas/engine/delivery/DeliveryReceiptLog\$DeliveryReceiptLogBuilder.class"
  "org/chovy/canvas/engine/delivery/ReachDeliveryService.class"
  "org/chovy/canvas/engine/trigger/CanvasExecutionService.class"
)
BACKEND_CLASSPATH_INPUTS=(
  "$BACKEND_DIR/pom.xml"
  "$ENGINE_DIR/pom.xml"
  "$BACKEND_DIR/canvas-cache-sdk/pom.xml"
)
PROVIDER_ADAPTER_TARGET_PACKAGE_DIR="$ENGINE_DIR/target/test-classes/org/chovy/canvas/domain/conversation"
PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR="$ENGINE_DIR/target/classes/org/chovy/canvas/domain/conversation"
PROVIDER_ADAPTER_TARGET_PACKAGE_DIRS=(
  "$PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR"
  "$PROVIDER_ADAPTER_TARGET_PACKAGE_DIR"
)
CANVAS_MAIN_TARGET_PACKAGE_DIR="$ENGINE_DIR/target/classes/org/chovy/canvas"
PROVIDER_SCAFFOLD_SCRIPT="$ROOT_DIR/scripts/scaffold-conversation-provider.sh"
PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY="VERIFY_SCAFFOLD"
PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER="verify_provider"
PROVIDER_SCAFFOLD_PROBE_ADAPTER_KEY="ZZZ_VERIFY_PROBE"
PROVIDER_SCAFFOLD_PROBE_PROVIDER="zzz_verify_probe"
LOMBOK_PROCESSOR='lombok.launch.AnnotationProcessorHider$AnnotationProcessor'

BACKEND_SELECTOR=""
BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES=()
BACKEND_CONVERSATION_API_MAIN_SOURCES=()
BACKEND_RUNTIME_MAIN_SOURCES=()
FRONTEND_CONVERSATION_LOGIC_TESTS=(
  "src/services/conversationApi.test.ts"
  "src/pages/conversations/conversationPresentation.test.ts"
  "src/services/demoSandboxApi.test.ts"
  "src/pages/demo-sandbox/demoSandbox.test.ts"
)
FRONTEND_AUTHORING_TESTS=(
  "src/components/node-panel/nodeLibrary.test.ts"
  "src/pages/canvas-editor/insertNode.test.ts"
)
FRONTEND_TESTS=(
  "${FRONTEND_CONVERSATION_LOGIC_TESTS[@]}"
  "${FRONTEND_AUTHORING_TESTS[@]}"
)
FOCUSED_BACKEND_TEST_SOURCES=(
  "canvas-engine/src/test/java/org/chovy/canvas/testsupport/MigrationTestSupport.java"
  "canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/web/ConversationControllerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/web/PublicConversationWebhookControllerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnectorTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistryTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceConnectorDispatchTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarnessTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupportTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationSessionSchemaTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationIngressServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyAdapterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/RcsConversationReplyAdapterTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractSupport.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractMatrixTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalogTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityServiceTest.java"
)
ADAPTER_CONTRACT_BACKEND_TEST_SOURCES=(
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractSupport.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractMatrixTest.java"
)
PROVIDER_ADAPTER_SUPPORT_MAIN_SOURCES=(
  "canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterContext.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressReq.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalog.java"
  "canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupport.java"
)
PROVIDER_ADAPTER_MAIN_SOURCES=()
LOMBOK_MAIN_SOURCES=()
BACKEND_MAIN_JAVA_SOURCES=()
FOCUSED_BACKEND_COMPILE_SOURCES=()
PROVIDER_SCAFFOLD_PROBE_REPO_FILES=(
  "$ENGINE_DIR/src/main/java/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyAdapter.java"
  "$ENGINE_DIR/src/main/java/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyPayload.java"
  "$ENGINE_DIR/src/test/resources/conversation/adapter-contracts/zzz-verify-probe-text.json"
  "$ENGINE_DIR/src/test/resources/conversation/adapter-contracts/zzz-verify-probe-interactive.json"
)
PROVIDER_SCAFFOLD_PROBE_TARGET_FILES=(
  "$ENGINE_DIR/target/classes/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyAdapter.class"
  "$ENGINE_DIR/target/classes/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyPayload.class"
  "$ENGINE_DIR/target/test-classes/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyAdapter.class"
  "$ENGINE_DIR/target/test-classes/org/chovy/canvas/domain/conversation/ZzzVerifyProbeConversationReplyPayload.class"
  "$ENGINE_DIR/target/test-classes/conversation/adapter-contracts/zzz-verify-probe-text.json"
  "$ENGINE_DIR/target/test-classes/conversation/adapter-contracts/zzz-verify-probe-interactive.json"
)

RUN_BACKEND=true
RUN_FRONTEND=true
RUN_FRONTEND_BUILD=false
RUN_ADAPTER_CONTRACT_ONLY=false
RUN_ADAPTER_SOURCE_COMPILE_ONLY=false
RUN_BACKEND_SOURCE_COMPILE_ONLY=false
RUN_ADAPTER_FIXTURE_LINT_ONLY=false
RUN_ADAPTER_FIXTURE_CONTRACT_ONLY=false
RUN_SCAFFOLD_PREFLIGHT_ONLY=false
RUN_SCAFFOLD_CONTRACT_PROBE=false
RUN_BACKEND_DOMAIN_ONLY=false
RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY=false
RUN_BACKEND_CONVERSATION_SERVICES_ONLY=false
RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY=false
RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY=false
RUN_BACKEND_WHATSAPP_ONLY=false
RUN_BACKEND_API_ONLY=false
RUN_BACKEND_RUNTIME_ONLY=false
RUN_BACKEND_SCHEMA_ONLY=false
RUN_FRONTEND_LOGIC_ONLY=false
RUN_FRONTEND_CONVERSATION_ONLY=false
RUN_FRONTEND_AUTHORING_ONLY=false
DRY_RUN=false
REFRESH_BACKEND_LOMBOK_TARGET_CLASSES=false
NODE_BIN=""
NPM_BIN=""

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/verify-conversation-focus.sh [options]

Runs the focused P2-080 conversation verification gate:
  - backend conversation schema, ingress, WAIT, provider webhook, adapter matrix, outbox connector tests
  - frontend conversation API, presentation, demo sandbox, node palette, and insert-node tests

Options:
  --backend-only           Run only backend verification
  --backend-source-compile-only Run backend focused javac preflight without Surefire
  --backend-domain-only    Run only backend domain/conversation verification
  --backend-conversation-adapters-only Run only backend conversation adapter verification
  --backend-conversation-services-only Run only backend domain/conversation service verification
  --backend-conversation-controllers-only Run only backend conversation controller verification
  --backend-conversation-webhooks-only Run only backend conversation webhook verification
  --backend-whatsapp-only Run only backend WhatsApp conversation verification
  --backend-api-only       Run only backend conversation API/controller verification
  --backend-runtime-only   Run only backend WAIT/delivery/channel runtime verification
  --backend-schema-only    Run only backend conversation schema/migration verification
  --adapter-contract-only  Run only the provider adapter contract matrix and fixture gates
  --adapter-source-compile-only Run only provider adapter focused javac preflight
  --adapter-fixture-lint-only Run only fast JSON fixture syntax/metadata checks
  --adapter-fixture-contract-only Run adapter fixture contracts using fresh compiled classes
  --scaffold-preflight-only Run only provider scaffold syntax/output/negative-input checks
  --scaffold-contract-probe Generate a disposable scaffold provider and prove adapter contracts
  --frontend-only          Run only frontend verification
  --frontend-logic-only    Run only frontend API/presentation helper tests
  --frontend-conversation-only Run frontend conversation/demo tests without authoring tests
  --frontend-authoring-only Run only frontend node palette and insert-node authoring tests
  --skip-frontend          Run backend verification and skip frontend verification
  --with-frontend-build    Also run npm run build after focused frontend tests
  --dry-run                Print resolved commands without running them
  --help, -h               Show this help
EOF
}

provider_adapter_main_sources() {
  local conversation_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas/domain/conversation"
  local conversation_source_prefix="canvas-engine/src/main/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_source_dir" ]] || fail "conversation source directory is missing: $conversation_source_dir"
  {
    for source in "${PROVIDER_ADAPTER_SUPPORT_MAIN_SOURCES[@]}"; do
      printf '%s\n' "$source"
    done
    while IFS= read -r source; do
      printf '%s/%s\n' "$conversation_source_prefix" "$(basename "$source")"
    done < <(find "$conversation_source_dir" -maxdepth 1 -type f \
      \( -name '*ConversationReplyAdapter.java' -o -name '*ConversationReplyPayload.java' \) | sort)
  } | awk '!seen[$0]++'
}

lombok_main_sources() {
  local main_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas"
  local source

  [[ -d "$main_source_dir" ]] || fail "main source directory is missing: $main_source_dir"
  while IFS= read -r source; do
    if grep -Eq '^(import|import static) lombok\.|lombok\.' "$source"; then
      printf '%s\n' "${source#$BACKEND_DIR/}"
    fi
  done < <(find "$main_source_dir" -type f -name '*.java' | sort)
}

backend_main_java_sources() {
  local main_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas"
  local root source import_line imported relative dal_source index
  local sources=()

  [[ -d "$main_source_dir" ]] || fail "main source directory is missing: $main_source_dir"
  for root in "${BACKEND_LOMBOK_CACHE_WATCH_MAIN_SOURCE_ROOTS[@]}"; do
    [[ -e "$main_source_dir/$root" ]] || continue
    if [[ -d "$main_source_dir/$root" ]]; then
      while IFS= read -r source; do
        sources+=("${source#$BACKEND_DIR/}")
      done < <(find "$main_source_dir/$root" -type f -name '*.java' | sort)
    elif [[ "$main_source_dir/$root" == *.java ]]; then
      sources+=("${main_source_dir#$BACKEND_DIR/}/$root")
    fi
  done

  index=0
  while [[ "$index" -lt "${#sources[@]}" ]]; do
    source="${sources[$index]}"
    if [[ -f "$BACKEND_DIR/$source" ]]; then
      while IFS= read -r import_line; do
        imported="${import_line#import }"
        imported="${imported%;}"
        relative="${imported#org.chovy.canvas.}"
        relative="${relative//.//}"
        dal_source="canvas-engine/src/main/java/org/chovy/canvas/$relative.java"
        [[ -f "$BACKEND_DIR/$dal_source" ]] && sources+=("$dal_source")
      done < <(grep -E '^import org\.chovy\.canvas\.dal\.(dataobject|mapper)\.[A-Za-z0-9_]+;' "$BACKEND_DIR/$source" || true)
    fi
    index=$((index + 1))
  done

  printf '%s\n' "${sources[@]}" | awk '!seen[$0]++'
}

conversation_focused_test_sources() {
  local conversation_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/conversation"
  local web_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/web"
  local source

  {
    for source in "${FOCUSED_BACKEND_TEST_SOURCES[@]}"; do
      printf '%s\n' "$source"
    done
    if [[ -d "$conversation_test_dir" ]]; then
      while IFS= read -r source; do
        printf '%s\n' "${source#$BACKEND_DIR/}"
      done < <(find "$conversation_test_dir" -maxdepth 1 -type f -name '*Test.java' | sort)
    fi
    if [[ -d "$web_test_dir" ]]; then
      while IFS= read -r source; do
        printf '%s\n' "${source#$BACKEND_DIR/}"
      done < <(find "$web_test_dir" -maxdepth 1 -type f -name '*Conversation*Test.java' | sort)
    fi
  } | awk '!seen[$0]++'
}

conversation_domain_main_sources() {
  local conversation_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_source_dir" ]] || fail "conversation source directory is missing: $conversation_source_dir"
  while IFS= read -r source; do
    printf '%s\n' "${source#$BACKEND_DIR/}"
  done < <(find "$conversation_source_dir" -maxdepth 1 -type f -name '*.java' | sort)
}

conversation_domain_test_sources() {
  local conversation_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_test_dir" ]] || fail "conversation test directory is missing: $conversation_test_dir"
  while IFS= read -r source; do
    printf '%s\n' "${source#$BACKEND_DIR/}"
  done < <(find "$conversation_test_dir" -maxdepth 1 -type f -name '*Test.java' | sort)
}

conversation_adapter_test_sources() {
  local conversation_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_test_dir" ]] || fail "conversation test directory is missing: $conversation_test_dir"
  {
    printf '%s\n' \
      "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarnessTest.java" \
      "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalogTest.java" \
      "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupportTest.java" \
      "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractSupport.java" \
      "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractMatrixTest.java"
    while IFS= read -r source; do
      printf '%s\n' "${source#$BACKEND_DIR/}"
    done < <(find "$conversation_test_dir" -maxdepth 1 -type f -name '*ConversationReplyAdapterTest.java' | sort)
  } | awk '!seen[$0]++'
}

conversation_service_test_sources() {
  local conversation_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_test_dir" ]] || fail "conversation test directory is missing: $conversation_test_dir"
  while IFS= read -r source; do
    printf '%s\n' "${source#$BACKEND_DIR/}"
  done < <(find "$conversation_test_dir" -maxdepth 1 -type f -name '*ServiceTest.java' | sort)
}

conversation_api_main_sources() {
  local web_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas/web"
  local source

  {
    conversation_domain_main_sources
    printf '%s\n' \
      "canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java" \
      "canvas-engine/src/main/java/org/chovy/canvas/config/InternalApiAuthFilter.java" \
      "canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java"
    [[ -d "$web_source_dir" ]] || fail "web source directory is missing: $web_source_dir"
    while IFS= read -r source; do
      printf '%s\n' "${source#$BACKEND_DIR/}"
    done < <(find "$web_source_dir" -maxdepth 1 -type f \
      \( -name '*Conversation*Controller.java' -o -name 'DemoSandboxController.java' \) | sort)
  } | awk '!seen[$0]++'
}

conversation_api_test_sources() {
  local web_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/web"
  local source

  {
    printf '%s\n' \
      "canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java" \
      "canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java"
    [[ -d "$web_test_dir" ]] || fail "web test directory is missing: $web_test_dir"
    while IFS= read -r source; do
      printf '%s\n' "${source#$BACKEND_DIR/}"
    done < <(find "$web_test_dir" -maxdepth 1 -type f -name '*Conversation*Test.java' | sort)
  } | awk '!seen[$0]++'
}

conversation_controller_main_sources() {
  local web_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas/web"
  local source

  {
    conversation_domain_main_sources
    printf '%s\n' \
      "canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java"
    [[ -d "$web_source_dir" ]] || fail "web source directory is missing: $web_source_dir"
    while IFS= read -r source; do
      printf '%s\n' "${source#$BACKEND_DIR/}"
    done < <(find "$web_source_dir" -maxdepth 1 -type f \
      \( -name '*Conversation*Controller.java' -o -name 'DemoSandboxController.java' \) | sort)
  } | awk '!seen[$0]++'
}

conversation_controller_test_sources() {
  local web_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/web"
  local source

  {
    printf '%s\n' \
      "canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java"
    [[ -d "$web_test_dir" ]] || fail "web test directory is missing: $web_test_dir"
    while IFS= read -r source; do
      printf '%s\n' "${source#$BACKEND_DIR/}"
    done < <(find "$web_test_dir" -maxdepth 1 -type f -name '*Conversation*Test.java' | sort)
  } | awk '!seen[$0]++'
}

conversation_webhook_main_sources() {
  {
    conversation_domain_main_sources
    printf '%s\n' \
      "canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java" \
      "canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java"
  } | awk '!seen[$0]++'
}

conversation_webhook_test_sources() {
  printf '%s\n' \
    "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityServiceTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/web/PublicConversationWebhookControllerTest.java"
}

backend_whatsapp_main_sources() {
  {
    conversation_webhook_main_sources
    printf '%s\n' \
      "canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java" \
      "canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiClient.java" \
      "canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnector.java"
  } | awk '!seen[$0]++'
}

backend_whatsapp_test_sources() {
  printf '%s\n' \
    "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapterTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityServiceTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/web/PublicConversationWebhookControllerTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnectorTest.java"
}

backend_runtime_main_sources() {
  local main_source_dir="$ENGINE_DIR/src/main/java/org/chovy/canvas"
  local root source
  local roots=(
    "common/MapFieldKeys.java"
    "common/PageResult.java"
    "common/enums/NodeType.java"
    "common/enums/TriggerType.java"
    "dal/dataobject/CanvasWaitSubscriptionDO.java"
    "dal/dataobject/MessageSendRecordDO.java"
    "dal/mapper/CanvasWaitSubscriptionMapper.java"
    "dal/mapper/MessageSendRecordMapper.java"
    "engine/channel/ChannelConnector.java"
    "engine/channel/ChannelConnectorRegistry.java"
    "engine/channel/DisabledChannelConnector.java"
    "engine/channel/WhatsAppCloudApiClient.java"
    "engine/channel/WhatsAppCloudApiConnector.java"
    "engine/context/ExecutionContext.java"
    "engine/delivery/DeliveryOutboxDO.java"
    "engine/delivery/DeliveryOutboxService.java"
    "engine/delivery/DeliveryReceiptLog.java"
    "engine/delivery/DeliveryReceiptRequest.java"
    "engine/delivery/ReachDeliveryService.java"
    "engine/handler/NodeHandler.java"
    "engine/handler/NodeHandlerType.java"
    "engine/handler/NodeOutcome.java"
    "engine/handler/NodeResult.java"
    "engine/handlers/WaitHandler.java"
    "engine/wait/WaitResumeService.java"
    "engine/wait/WaitSubscriptionService.java"
    "infrastructure/http/ExternalHttpClient.java"
    "infrastructure/mq/DeliveryOutboxConsumer.java"
    "infrastructure/reactor/TrackedReactiveTaskRegistry.java"
  )

  for root in "${roots[@]}"; do
    [[ -e "$main_source_dir/$root" ]] || fail "backend runtime source is missing: $main_source_dir/$root"
    if [[ -d "$main_source_dir/$root" ]]; then
      while IFS= read -r source; do
        printf '%s\n' "${source#$BACKEND_DIR/}"
      done < <(find "$main_source_dir/$root" -type f -name '*.java' | sort)
    else
      printf '%s/%s\n' "${main_source_dir#$BACKEND_DIR/}" "$root"
    fi
  done | awk '!seen[$0]++'
}

backend_runtime_test_sources() {
  printf '%s\n' \
    "canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnectorTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistryTest.java" \
    "canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceConnectorDispatchTest.java"
}

conversation_schema_test_sources() {
  local conversation_test_dir="$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/conversation"
  local source

  [[ -d "$conversation_test_dir" ]] || fail "conversation test directory is missing: $conversation_test_dir"
  while IFS= read -r source; do
    printf '%s\n' "${source#$BACKEND_DIR/}"
  done < <(find "$conversation_test_dir" -maxdepth 1 -type f -name '*SchemaTest.java' | sort)
}

backend_selector_from_sources() {
  local source class_name
  local test_classes=()

  for source in "$@"; do
    class_name="$(basename "$source" .java)"
    if [[ "$class_name" == *Test ]]; then
      test_classes+=("$class_name")
    fi
  done

  [[ "${#test_classes[@]}" -gt 0 ]] || fail "backend focused verification has no test classes"
  local IFS=,
  printf '%s' "${test_classes[*]}"
}

assert_backend_sources_exist() {
  local source
  local missing=()

  for source in "$@"; do
    if [[ ! -f "$BACKEND_DIR/$source" ]]; then
      missing+=("$source")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: backend focused source is missing: %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

frontend_discovered_service_tests() {
  local source

  while IFS= read -r source; do
    printf '%s\n' "${source#$FRONTEND_DIR/}"
  done < <(find "$FRONTEND_DIR/src/services" -maxdepth 1 -type f \
    \( -name 'conversation*.test.ts' -o -name 'conversation*.test.tsx' \
    -o -name 'demoSandbox*.test.ts' -o -name 'demoSandbox*.test.tsx' \) | sort)
}

frontend_discovered_page_tests() {
  local include_tsx="$1"
  local source

  if [[ "$include_tsx" == "true" ]]; then
    while IFS= read -r source; do
      printf '%s\n' "${source#$FRONTEND_DIR/}"
    done < <(find "$FRONTEND_DIR/src/pages/conversations" "$FRONTEND_DIR/src/pages/demo-sandbox" \
      -maxdepth 1 -type f \( -name '*.test.ts' -o -name '*.test.tsx' \) 2>/dev/null | sort)
  else
    while IFS= read -r source; do
      printf '%s\n' "${source#$FRONTEND_DIR/}"
    done < <(find "$FRONTEND_DIR/src/pages/conversations" "$FRONTEND_DIR/src/pages/demo-sandbox" \
      -maxdepth 1 -type f -name '*.test.ts' 2>/dev/null | sort)
  fi
}

frontend_conversation_logic_tests() {
  local source

  {
    for source in "${FRONTEND_CONVERSATION_LOGIC_TESTS[@]}"; do
      printf '%s\n' "$source"
    done
    frontend_discovered_service_tests
    frontend_discovered_page_tests false
  } | awk '!seen[$0]++'
}

frontend_conversation_tests() {
  local source

  {
    for source in "${FRONTEND_CONVERSATION_LOGIC_TESTS[@]}"; do
      printf '%s\n' "$source"
    done
    frontend_discovered_service_tests
    frontend_discovered_page_tests true
  } | awk '!seen[$0]++'
}

frontend_authoring_tests() {
  local source

  for source in "${FRONTEND_AUTHORING_TESTS[@]}"; do
    printf '%s\n' "$source"
  done
}

frontend_focused_tests() {
  local source

  {
    for source in "${FRONTEND_TESTS[@]}"; do
      printf '%s\n' "$source"
    done
    frontend_discovered_service_tests
    frontend_discovered_page_tests true
  } | awk '!seen[$0]++'
}

assert_frontend_tests_exist() {
  local source
  local missing=()

  for source in "$@"; do
    if [[ ! -f "$FRONTEND_DIR/$source" ]]; then
      missing+=("$source")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: frontend focused test is missing: %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

assert_provider_scaffold_available() {
  [[ -x "$PROVIDER_SCAFFOLD_SCRIPT" ]] || fail "conversation provider scaffold is missing or not executable: $PROVIDER_SCAFFOLD_SCRIPT"
}

repo_relative_path() {
  local path="$1"
  printf '%s\n' "${path#$ROOT_DIR/}"
}

assert_text_contains() {
  local text="$1"
  local expected="$2"
  local label="$3"

  [[ "$text" == *"$expected"* ]] || fail "$label must contain: $expected"
}

assert_text_not_contains() {
  local text="$1"
  local unexpected="$2"
  local label="$3"

  [[ "$text" != *"$unexpected"* ]] || fail "$label must not contain: $unexpected"
}

assert_command_fails_with() {
  local expected="$1"
  shift
  local output status

  set +e
  output="$("$@" 2>&1)"
  status=$?
  set -e

  [[ "$status" -ne 0 ]] || fail "expected command to fail: $*"
  assert_text_contains "$output" "$expected" "failed command output for $*"
}

verify_provider_scaffold_preflight() {
  local scaffold_output scaffold_verify_output

  assert_provider_scaffold_available
  bash -n "$PROVIDER_SCAFFOLD_SCRIPT"
  scaffold_output="$("$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER" \
    --dry-run)"

  assert_text_contains "$scaffold_output" \
    "backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/VerifyScaffoldConversationReplyAdapter.java" \
    "provider scaffold dry-run output"
  assert_text_contains "$scaffold_output" \
    "backend/canvas-engine/src/test/resources/conversation/adapter-contracts/verify-scaffold-text.json" \
    "provider scaffold dry-run output"
  assert_text_contains "$scaffold_output" \
    "scripts/verify-conversation-focus.sh --adapter-contract-only" \
    "provider scaffold dry-run output"
  assert_text_contains "$scaffold_output" \
    "scripts/verify-conversation-focus.sh --adapter-fixture-lint-only" \
    "provider scaffold dry-run output"
  assert_text_not_contains "$scaffold_output" "$ROOT_DIR/" "provider scaffold dry-run output"

  scaffold_verify_output="$("$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER" \
    --verify \
    --dry-run)"
  assert_text_contains "$scaffold_verify_output" \
    "would run verification:" \
    "provider scaffold verify dry-run output"
  assert_text_contains "$scaffold_verify_output" \
    "scripts/verify-conversation-focus.sh --adapter-contract-only" \
    "provider scaffold verify dry-run output"
  assert_text_not_contains "$scaffold_verify_output" "$ROOT_DIR/" "provider scaffold verify dry-run output"

  assert_command_fails_with "--adapter-key must not contain consecutive underscores" \
    "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD__KEY --provider bad --dry-run
  assert_command_fails_with "--adapter-key must not end with underscore" \
    "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_ --provider bad --dry-run
  assert_command_fails_with "--attribute must not reuse a common payload field: text" \
    "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_FIELD --provider bad --attribute text --dry-run
  assert_command_fails_with "--attribute must not be a Java keyword: class" \
    "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_KEYWORD --provider bad --attribute class --dry-run
  assert_command_fails_with "--attribute must be lower camel case: ThreadId" \
    "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_FIELD_NAME --provider bad --attribute ThreadId --dry-run
}

assert_provider_scaffold_probe_repo_files_absent() {
  local file

  for file in "${PROVIDER_SCAFFOLD_PROBE_REPO_FILES[@]}"; do
    [[ ! -e "$file" ]] || fail "provider scaffold probe file already exists: $(repo_relative_path "$file")"
  done
}

cleanup_provider_scaffold_probe() {
  local file

  for file in "${PROVIDER_SCAFFOLD_PROBE_REPO_FILES[@]}" "${PROVIDER_SCAFFOLD_PROBE_TARGET_FILES[@]}"; do
    rm -f "$file"
  done
}

verify_provider_scaffold_contract_probe() {
  local file status

  assert_provider_scaffold_available
  assert_provider_scaffold_probe_repo_files_absent
  for file in "${PROVIDER_SCAFFOLD_PROBE_TARGET_FILES[@]}"; do
    rm -f "$file"
  done

  set +e
  "$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_PROBE_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_PROBE_PROVIDER" \
    --verify
  status=$?
  set -e

  cleanup_provider_scaffold_probe
  return "$status"
}

verify_adapter_fixture_lint() {
  local node_bin

  node_bin="$(command -v node || true)"
  [[ -n "$node_bin" ]] || fail "node is required for adapter fixture lint"
  "$node_bin" - "$ADAPTER_CONTRACT_FIXTURE_DIR" <<'NODE'
const fs = require('fs')
const path = require('path')

const fixtureDir = process.argv[2]
const supportedMessageTypes = new Set(['TEXT', 'IMAGE', 'INTERACTIVE', 'UNKNOWN'])
const commonPayloadFields = new Set([
  'canvasId',
  'versionId',
  'executionId',
  'userId',
  'provider',
  'externalMessageId',
  'eventId',
  'text',
  'intent',
  'attributes',
  'occurredAt'
])
let failures = 0

function fail(message) {
  failures += 1
  console.error(`ERROR: ${message}`)
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function textValue(value) {
  return value === null || value === undefined ? '' : String(value)
}

function assertNonBlankString(value, label) {
  if (typeof value !== 'string' || value.trim() === '') {
    fail(`${label} must be a nonblank string`)
  }
}

function equalJson(left, right) {
  return JSON.stringify(left) === JSON.stringify(right)
}

if (!fs.existsSync(fixtureDir)) {
  fail(`adapter fixture directory is missing: ${fixtureDir}`)
} else {
  const indexPath = path.join(fixtureDir, 'index.json')
  if (fs.existsSync(indexPath)) {
    fail('adapter fixture index is retired: conversation/adapter-contracts/index.json')
  }

  const files = fs.readdirSync(fixtureDir)
    .filter(file => file.endsWith('.json'))
    .sort()

  if (files.length === 0) {
    fail(`adapter fixture directory has no JSON fixtures: ${fixtureDir}`)
  }

  const names = new Set()
  for (const file of files) {
    const fixturePath = path.join(fixtureDir, file)
    const resourceName = `conversation/adapter-contracts/${file}`
    let fixture

    try {
      fixture = JSON.parse(fs.readFileSync(fixturePath, 'utf8'))
    } catch (error) {
      fail(`${resourceName} must be valid JSON: ${error.message}`)
      continue
    }

    assertNonBlankString(fixture.name, `${resourceName} name`)
    if (typeof fixture.name === 'string') {
      if (names.has(fixture.name)) {
        fail(`${resourceName} name must be unique: ${fixture.name}`)
      }
      names.add(fixture.name)
    }

    assertNonBlankString(fixture.adapterKey, `${resourceName} adapterKey`)
    assertNonBlankString(fixture.expectedChannel, `${resourceName} expectedChannel`)
    assertNonBlankString(fixture.expectedProvider, `${resourceName} expectedProvider`)
    assertNonBlankString(fixture.expectedMessageType, `${resourceName} expectedMessageType`)
    assertNonBlankString(fixture.expectedText, `${resourceName} expectedText`)
    assertNonBlankString(fixture.expectedExternalMessageId, `${resourceName} expectedExternalMessageId`)
    assertNonBlankString(fixture.expectedEventId, `${resourceName} expectedEventId`)
    assertNonBlankString(fixture.missingPayloadMessage, `${resourceName} missingPayloadMessage`)

    if (!isObject(fixture.rawPayload)) {
      fail(`${resourceName} rawPayload must be an object`)
      continue
    }
    if (!isObject(fixture.expectedAttributes)) {
      fail(`${resourceName} expectedAttributes must be an object`)
      continue
    }

    const adapterKey = textValue(fixture.adapterKey)
    const expectedChannel = textValue(fixture.expectedChannel)
    const expectedProvider = textValue(fixture.expectedProvider)
    const expectedMessageType = textValue(fixture.expectedMessageType)
    const rawPayload = fixture.rawPayload
    const expectedAttributes = fixture.expectedAttributes

    if (expectedChannel !== expectedChannel.trim() || expectedChannel !== expectedChannel.toUpperCase()) {
      fail(`${resourceName} expectedChannel must be uppercase trimmed`)
    }
    if (expectedProvider !== expectedProvider.trim() || expectedProvider !== expectedProvider.toUpperCase()) {
      fail(`${resourceName} expectedProvider must be uppercase trimmed`)
    }
    if (expectedMessageType !== expectedMessageType.trim() || expectedMessageType !== expectedMessageType.toUpperCase()) {
      fail(`${resourceName} expectedMessageType must be uppercase trimmed`)
    }
    if (!supportedMessageTypes.has(expectedMessageType)) {
      fail(`${resourceName} expectedMessageType must be one of ${Array.from(supportedMessageTypes).join(', ')}`)
    }
    if (adapterKey !== expectedChannel.toLowerCase()) {
      fail(`${resourceName} adapterKey must be the lowercase alias of expectedChannel`)
    }

    const expectedPrefix = adapterKey.replace(/_/g, '-')
    if (!file.startsWith(`${expectedPrefix}-`)) {
      fail(`${resourceName} file name must start with ${expectedPrefix}-`)
    }

    for (const key of ['userId', 'provider', 'externalMessageId', 'eventId']) {
      if (!Object.prototype.hasOwnProperty.call(rawPayload, key)) {
        fail(`${resourceName} rawPayload.${key} is required`)
      } else if (textValue(rawPayload[key]).trim() === '') {
        fail(`${resourceName} rawPayload.${key} must be usable text`)
      }
    }

    if (expectedAttributes.adapter !== expectedChannel) {
      fail(`${resourceName} expectedAttributes.adapter must equal expectedChannel`)
    }
    if (textValue(rawPayload.provider).toUpperCase() !== expectedProvider) {
      fail(`${resourceName} expectedProvider must bind to rawPayload.provider`)
    }
    if (textValue(rawPayload.externalMessageId) !== textValue(fixture.expectedExternalMessageId)) {
      fail(`${resourceName} expectedExternalMessageId must bind to rawPayload.externalMessageId`)
    }
    if (textValue(rawPayload.eventId) !== textValue(fixture.expectedEventId)) {
      fail(`${resourceName} expectedEventId must bind to rawPayload.eventId`)
    }

    if (expectedMessageType === 'TEXT' && textValue(rawPayload.text) !== textValue(fixture.expectedText)) {
      fail(`${resourceName} expectedText must bind to rawPayload.text for TEXT fixtures`)
    }
    if (expectedMessageType === 'INTERACTIVE') {
      const topLevelValues = Object.values(rawPayload).map(textValue)
      const attributeValues = isObject(rawPayload.attributes)
        ? Object.values(rawPayload.attributes).map(textValue)
        : []
      if (![...topLevelValues, ...attributeValues].includes(textValue(fixture.expectedText))) {
        fail(`${resourceName} expectedText must bind to a raw interactive display value`)
      }
    }

    for (const [key, value] of Object.entries(expectedAttributes)) {
      if (key === 'adapter') {
        continue
      }
      const rawValue = Object.prototype.hasOwnProperty.call(rawPayload, key)
        ? rawPayload[key]
        : isObject(rawPayload.attributes) && Object.prototype.hasOwnProperty.call(rawPayload.attributes, key)
          ? rawPayload.attributes[key]
          : undefined
      if (rawValue === undefined || !equalJson(rawValue, value)) {
        fail(`${resourceName} expectedAttributes.${key} must bind to rawPayload.${key} or rawPayload.attributes.${key}`)
      }
    }

    for (const key of Object.keys(rawPayload)) {
      if (!commonPayloadFields.has(key) && !Object.prototype.hasOwnProperty.call(expectedAttributes, key)) {
        fail(`${resourceName} provider-specific rawPayload.${key} must appear in expectedAttributes`)
      }
    }
  }
}

if (failures > 0) {
  process.exit(1)
}
NODE
}

clean_provider_adapter_classes() {
  local package_dir

  for package_dir in "${PROVIDER_ADAPTER_TARGET_PACKAGE_DIRS[@]}"; do
    [[ -d "$package_dir" ]] || continue
    find "$package_dir" -type f \
      \( -name '*ConversationReplyAdapter.class' -o -name '*ConversationReplyPayload.class' \) \
      -delete
  done
}

clean_provider_adapter_main_classes() {
  [[ -d "$PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR" ]] || return 0
  find "$PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR" -type f \
    \( -name '*ConversationReplyAdapter.class' -o -name '*ConversationReplyPayload.class' \) \
    -delete
}

prune_stale_provider_adapter_test_classes() {
  local class_file simple_name source_file

  [[ -d "$PROVIDER_ADAPTER_TARGET_PACKAGE_DIR" ]] || return 0
  while IFS= read -r class_file; do
    simple_name="$(basename "$class_file" .class)"
    source_file="$ENGINE_DIR/src/main/java/org/chovy/canvas/domain/conversation/$simple_name.java"
    if [[ ! -f "$source_file" ]]; then
      rm -f "$class_file"
    fi
  done < <(find "$PROVIDER_ADAPTER_TARGET_PACKAGE_DIR" -maxdepth 1 -type f \
    \( -name '*ConversationReplyAdapter.class' -o -name '*ConversationReplyPayload.class' \) | sort)
}

target_test_class_for_source() {
  local source="$1"
  local relative
  local package_name
  local class_name

  case "$source" in
    canvas-engine/src/main/java/*.java)
      relative="${source#canvas-engine/src/main/java/}"
      ;;
    canvas-engine/src/test/java/*.java)
      relative="${source#canvas-engine/src/test/java/}"
      ;;
    *)
      fail "cannot map backend source to target test class: $source"
      ;;
  esac
  package_name="$(awk '/^package / {gsub(/;$/, "", $2); print $2; exit}' "$BACKEND_DIR/$source")"
  if [[ -n "$package_name" ]]; then
    class_name="$(basename "$source")"
    relative="${package_name//.//}/$class_name"
  fi
  printf '%s/%s.class\n' "$BACKEND_TEST_RESOURCE_TARGET_DIR" "${relative%.java}"
}

assert_backend_selected_tests_compiled() {
  local source class_file
  local missing=()

  for source in "$@"; do
    [[ "$(basename "$source" .java)" == *Test ]] || continue
    class_file="$(target_test_class_for_source "$source")"
    if [[ ! -f "$class_file" ]]; then
      missing+=("$source -> ${class_file#$ENGINE_DIR/}")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: backend focused test class was not compiled: %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

target_main_class_for_source() {
  local source="$1"
  local relative
  local package_name
  local class_name

  case "$source" in
    canvas-engine/src/main/java/*.java)
      relative="${source#canvas-engine/src/main/java/}"
      ;;
    *)
      fail "cannot map non-main backend source to target main class: $source"
      ;;
  esac
  package_name="$(awk '/^package / {gsub(/;$/, "", $2); print $2; exit}' "$BACKEND_DIR/$source")"
  if [[ -n "$package_name" ]]; then
    class_name="$(basename "$source")"
    relative="${package_name//.//}/$class_name"
  fi
  printf '%s/%s.class\n' "$BACKEND_MAIN_RESOURCE_TARGET_DIR" "${relative%.java}"
}

class_file_is_fresh_for_source() {
  local class_file="$1"
  local source="$2"
  local input

  [[ -f "$class_file" ]] || return 1
  [[ "$class_file" -nt "$BACKEND_DIR/$source" ]] || return 1
  for input in "${BACKEND_CLASSPATH_INPUTS[@]}"; do
    [[ ! -f "$input" || "$class_file" -nt "$input" ]] || return 1
  done
}

adapter_fixture_contract_class_cache_is_valid() {
  local source class_file main_class_file source_file_name

  for source in "${FOCUSED_BACKEND_COMPILE_SOURCES[@]}"; do
    class_file="$(target_test_class_for_source "$source")"
    source_file_name="$(basename "$source")"
    if [[ "$source" == canvas-engine/src/main/java/* \
      && "$source_file_name" != *ConversationReplyAdapter.java \
      && "$source_file_name" != *ConversationReplyPayload.java ]]; then
      main_class_file="$(target_main_class_for_source "$source")"
      class_file_is_fresh_for_source "$class_file" "$source" \
        || class_file_is_fresh_for_source "$main_class_file" "$source" \
        || return 1
    else
      class_file_is_fresh_for_source "$class_file" "$source" || return 1
    fi
  done
}

clean_canvas_main_classes() {
  [[ -d "$CANVAS_MAIN_TARGET_PACKAGE_DIR" ]] || return 0
  find "$CANVAS_MAIN_TARGET_PACKAGE_DIR" -type f -name '*.class' -delete
}

write_focused_backend_sources_file() {
  mkdir -p "$(dirname "$FOCUSED_BACKEND_SOURCES_FILE")"
  printf '%s\n' "${FOCUSED_BACKEND_COMPILE_SOURCES[@]}" > "$FOCUSED_BACKEND_SOURCES_FILE"
}

dedupe_focused_backend_compile_sources() {
  local source
  local deduped=()

  while IFS= read -r source; do
    [[ -n "$source" ]] || continue
    deduped+=("$source")
  done < <(printf '%s\n' "${FOCUSED_BACKEND_COMPILE_SOURCES[@]}" | awk '!seen[$0]++')

  FOCUSED_BACKEND_COMPILE_SOURCES=("${deduped[@]}")
}

copy_resource_tree() {
  local source_dir="$1"
  local target_dir="$2"
  local source relative target_parent

  [[ -d "$source_dir" ]] || return 0
  mkdir -p "$target_dir"
  while IFS= read -r -d '' source; do
    relative="${source#$source_dir/}"
    target_parent="$(dirname "$target_dir/$relative")"
    mkdir -p "$target_parent"
    cp -p "$source" "$target_dir/$relative"
  done < <(find "$source_dir" -type f -print0)
}

assert_backend_main_resource_roots_supported() {
  local resource root supported supported_root

  [[ -d "$BACKEND_MAIN_RESOURCE_DIR" ]] || return 0
  while IFS= read -r resource; do
    root="$(basename "$resource")"
    supported_root=false
    for supported in "${BACKEND_MAIN_RESOURCE_SYNC_ROOTS[@]}"; do
      if [[ "$root" == "$supported" ]]; then
        supported_root=true
        break
      fi
    done
    [[ "$supported_root" == "true" ]] || fail "backend main resource root needs verifier sync review: $root"
  done < <(find "$BACKEND_MAIN_RESOURCE_DIR" -mindepth 1 -maxdepth 1 | sort)
}

sync_backend_main_resources() {
  local root

  assert_backend_main_resource_roots_supported
  mkdir -p "$BACKEND_MAIN_RESOURCE_TARGET_DIR"
  for root in "${BACKEND_MAIN_RESOURCE_SYNC_ROOTS[@]}"; do
    rm -rf "$BACKEND_MAIN_RESOURCE_TARGET_DIR/$root"
  done
  copy_resource_tree "$BACKEND_MAIN_RESOURCE_DIR" "$BACKEND_MAIN_RESOURCE_TARGET_DIR"
}

sync_backend_test_resources() {
  local root

  assert_backend_test_resource_roots_supported
  mkdir -p "$BACKEND_TEST_RESOURCE_TARGET_DIR"
  for root in "${BACKEND_TEST_RESOURCE_SYNC_ROOTS[@]}"; do
    rm -rf "$BACKEND_TEST_RESOURCE_TARGET_DIR/$root"
  done
  copy_resource_tree "$BACKEND_TEST_RESOURCE_DIR" "$BACKEND_TEST_RESOURCE_TARGET_DIR"
}

assert_backend_test_resource_roots_supported() {
  local resource root supported supported_root

  [[ -d "$BACKEND_TEST_RESOURCE_DIR" ]] || return 0
  while IFS= read -r resource; do
    root="$(basename "$resource")"
    supported_root=false
    for supported in "${BACKEND_TEST_RESOURCE_SYNC_ROOTS[@]}"; do
      if [[ "$root" == "$supported" ]]; then
        supported_root=true
        break
      fi
    done
    [[ "$supported_root" == "true" ]] || fail "backend test resource root needs verifier sync review: $root"
  done < <(find "$BACKEND_TEST_RESOURCE_DIR" -mindepth 1 -maxdepth 1 | sort)
}

clean_canvas_test_classes() {
  [[ -d "$BACKEND_TEST_RESOURCE_TARGET_DIR/org/chovy/canvas" ]] || return 0
  find "$BACKEND_TEST_RESOURCE_TARGET_DIR/org/chovy/canvas" -type f -name '*.class' -delete
}

backend_test_classpath_is_fresh() {
  local input

  [[ -s "$BACKEND_TEST_CLASSPATH_FILE" ]] || return 1
  for input in "${BACKEND_CLASSPATH_INPUTS[@]}"; do
    [[ ! -f "$input" || "$BACKEND_TEST_CLASSPATH_FILE" -nt "$input" ]] || return 1
  done
}

backend_test_classpath_entries_exist() {
  local classpath entry old_ifs

  [[ -s "$BACKEND_TEST_CLASSPATH_FILE" ]] || return 1
  classpath="$(cat "$BACKEND_TEST_CLASSPATH_FILE")"
  [[ -n "$classpath" ]] || return 1

  old_ifs="$IFS"
  IFS=:
  for entry in $classpath; do
    if [[ ! -e "$entry" ]]; then
      IFS="$old_ifs"
      return 1
    fi
  done
  IFS="$old_ifs"
}

backend_test_classpath_cache_is_valid() {
  backend_test_classpath_is_fresh && backend_test_classpath_entries_exist
}

refresh_backend_test_classpath() {
  if backend_test_classpath_cache_is_valid; then
    echo "Using cached backend test classpath: $BACKEND_TEST_CLASSPATH_FILE"
    return 0
  fi
  mkdir -p "$(dirname "$BACKEND_TEST_CLASSPATH_FILE")"
  "${BACKEND_CLASSPATH_CMD[@]}"
}

backend_lombok_target_classes_have_classes() {
  local sentinel

  [[ -d "$BACKEND_TEST_RESOURCE_TARGET_DIR/org/chovy/canvas" ]] || return 1
  [[ -n "$(find "$BACKEND_TEST_RESOURCE_TARGET_DIR/org/chovy/canvas" -type f -name '*.class' -print -quit)" ]] || return 1
  for sentinel in "${BACKEND_LOMBOK_TARGET_CLASS_SENTINELS[@]}"; do
    [[ -f "$BACKEND_TEST_RESOURCE_TARGET_DIR/$sentinel" ]] || return 1
  done
}

backend_lombok_target_classes_sources_match() {
  local cached current

  [[ -f "$BACKEND_LOMBOK_TARGET_CLASSES_ALL_SOURCES_FILE" ]] || return 1
  cached="$(cat "$BACKEND_LOMBOK_TARGET_CLASSES_ALL_SOURCES_FILE")"
  current="$(printf '%s\n' "${BACKEND_MAIN_JAVA_SOURCES[@]}")"
  [[ "$cached" == "$current" ]]
}

backend_lombok_target_classes_are_fresh() {
  local input source

  [[ -f "$BACKEND_LOMBOK_TARGET_CLASSES_STAMP" ]] || return 1
  backend_lombok_target_classes_have_classes || return 1
  backend_lombok_target_classes_sources_match || return 1
  [[ "$BACKEND_LOMBOK_TARGET_CLASSES_STAMP" -nt "$BACKEND_TEST_CLASSPATH_FILE" ]] || return 1
  for input in "${BACKEND_CLASSPATH_INPUTS[@]}"; do
    [[ ! -f "$input" || "$BACKEND_LOMBOK_TARGET_CLASSES_STAMP" -nt "$input" ]] || return 1
  done
  for source in "${BACKEND_MAIN_JAVA_SOURCES[@]}"; do
    [[ "$BACKEND_LOMBOK_TARGET_CLASSES_STAMP" -nt "$BACKEND_DIR/$source" ]] || return 1
  done
}

backend_lombok_target_classes_cache_is_valid() {
  backend_lombok_target_classes_are_fresh
}

backend_focused_compile_uses_lombok_processor() {
  [[ "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
    && "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
    && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
    && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
    && "$RUN_BACKEND_API_ONLY" != "true" \
    && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]
}

write_backend_lombok_target_classes_cache() {
  [[ "$REFRESH_BACKEND_LOMBOK_TARGET_CLASSES" == "true" ]] || return 0
  mkdir -p "$FOCUSED_BACKEND_WORK_DIR"
  printf '%s\n' "${LOMBOK_MAIN_SOURCES[@]}" > "$BACKEND_LOMBOK_TARGET_CLASSES_SOURCES_FILE"
  printf '%s\n' "${BACKEND_MAIN_JAVA_SOURCES[@]}" > "$BACKEND_LOMBOK_TARGET_CLASSES_ALL_SOURCES_FILE"
  touch "$BACKEND_LOMBOK_TARGET_CLASSES_STAMP"
}

restore_backend_lombok_target_classes_cache_outputs() {
  [[ "$REFRESH_BACKEND_LOMBOK_TARGET_CLASSES" == "true" ]] || return 0
  local restore_sources_file="$FOCUSED_BACKEND_WORK_DIR/lombok-target-restore-sources.txt"

  mkdir -p "$FOCUSED_BACKEND_WORK_DIR"
  {
    printf '%s\n' "${BACKEND_MAIN_JAVA_SOURCES[@]}"
    printf '%s\n' "${LOMBOK_MAIN_SOURCES[@]}"
  } | awk '!seen[$0]++' > "$restore_sources_file"

  "$JAVA_HOME/bin/javac" \
    --release \
    21 \
    -cp \
    "$TEST_CP" \
    -processorpath \
    "$TEST_CP" \
    -processor \
    "$LOMBOK_PROCESSOR" \
    -implicit:class \
    -sourcepath \
    "canvas-engine/src/main/java:canvas-engine/src/test/java" \
    -d \
    "$ENGINE_DIR/target/test-classes" \
    "@$restore_sources_file"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backend-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      shift
      ;;
    --backend-source-compile-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_SOURCE_COMPILE_ONLY=true
      shift
      ;;
    --backend-domain-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_DOMAIN_ONLY=true
      shift
      ;;
    --backend-conversation-adapters-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY=true
      shift
      ;;
    --backend-conversation-services-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_CONVERSATION_SERVICES_ONLY=true
      shift
      ;;
    --backend-conversation-controllers-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY=true
      shift
      ;;
    --backend-conversation-webhooks-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY=true
      shift
      ;;
    --backend-whatsapp-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_WHATSAPP_ONLY=true
      shift
      ;;
    --backend-api-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_API_ONLY=true
      shift
      ;;
    --backend-runtime-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_RUNTIME_ONLY=true
      shift
      ;;
    --backend-schema-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_BACKEND_SCHEMA_ONLY=true
      shift
      ;;
    --adapter-contract-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_ADAPTER_CONTRACT_ONLY=true
      shift
      ;;
    --adapter-source-compile-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_ADAPTER_SOURCE_COMPILE_ONLY=true
      shift
      ;;
    --adapter-fixture-lint-only)
      RUN_BACKEND=false
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_ADAPTER_FIXTURE_LINT_ONLY=true
      shift
      ;;
    --adapter-fixture-contract-only)
      RUN_BACKEND=false
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_ADAPTER_FIXTURE_CONTRACT_ONLY=true
      shift
      ;;
    --scaffold-preflight-only)
      RUN_BACKEND=false
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_SCAFFOLD_PREFLIGHT_ONLY=true
      shift
      ;;
    --scaffold-contract-probe)
      RUN_BACKEND=false
      RUN_FRONTEND=false
      RUN_FRONTEND_BUILD=false
      RUN_SCAFFOLD_CONTRACT_PROBE=true
      shift
      ;;
    --frontend-only)
      RUN_BACKEND=false
      RUN_FRONTEND=true
      shift
      ;;
    --frontend-logic-only)
      RUN_BACKEND=false
      RUN_FRONTEND=true
      RUN_FRONTEND_LOGIC_ONLY=true
      RUN_FRONTEND_CONVERSATION_ONLY=false
      RUN_FRONTEND_AUTHORING_ONLY=false
      shift
      ;;
    --frontend-conversation-only)
      RUN_BACKEND=false
      RUN_FRONTEND=true
      RUN_FRONTEND_LOGIC_ONLY=false
      RUN_FRONTEND_CONVERSATION_ONLY=true
      RUN_FRONTEND_AUTHORING_ONLY=false
      shift
      ;;
    --frontend-authoring-only)
      RUN_BACKEND=false
      RUN_FRONTEND=true
      RUN_FRONTEND_LOGIC_ONLY=false
      RUN_FRONTEND_CONVERSATION_ONLY=false
      RUN_FRONTEND_AUTHORING_ONLY=true
      shift
      ;;
    --skip-frontend)
      RUN_FRONTEND=false
      shift
      ;;
    --with-frontend-build)
      RUN_FRONTEND_BUILD=true
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

if [[ "$RUN_ADAPTER_CONTRACT_ONLY" == "true" || "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" == "true" || "$RUN_ADAPTER_FIXTURE_CONTRACT_ONLY" == "true" ]]; then
  FOCUSED_BACKEND_TEST_SOURCES=("${ADAPTER_CONTRACT_BACKEND_TEST_SOURCES[@]}")
fi

java_spec_for() {
  "$1" -XshowSettings:properties -version 2>&1 |
    awk -F'= ' '/java.specification.version/ {print $2; exit}'
}

is_java_21_or_newer() {
  case "$1" in
    21|2[2-9]|[3-9][0-9])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

node_version_for() {
  "$1" -v 2>/dev/null | sed 's/^v//'
}

is_frontend_node_supported() {
  local version="$1"
  local major minor
  major="${version%%.*}"
  version="${version#*.}"
  minor="${version%%.*}"
  case "$major" in
    20)
      [[ "$minor" =~ ^[0-9]+$ && "$minor" -ge 19 ]]
      ;;
    22)
      [[ "$minor" =~ ^[0-9]+$ && "$minor" -ge 12 ]]
      ;;
    2[4-9]|[3-9][0-9])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

try_java_home() {
  local candidate="$1"
  if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
    local candidate_spec
    candidate_spec="$(java_spec_for "$candidate/bin/java")"
    if is_java_21_or_newer "$candidate_spec"; then
      JAVA_HOME="$candidate"
      export JAVA_HOME
      return 0
    fi
  fi
  return 1
}

resolve_java_21() {
  local java_spec=""
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    java_spec="$(java_spec_for "$JAVA_HOME/bin/java")"
    if is_java_21_or_newer "$java_spec"; then
      return 0
    fi
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local candidate_java_home
    candidate_java_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    try_java_home "$candidate_java_home" && return 0
  fi

  if [[ -d "$HOME/Library/Java/JavaVirtualMachines" ]]; then
    local candidate
    while IFS= read -r candidate; do
      try_java_home "$candidate" && return 0
    done < <(find "$HOME/Library/Java/JavaVirtualMachines" -path "*/Contents/Home" -type d 2>/dev/null | sort)
  fi

  if command -v java >/dev/null 2>&1; then
    java_spec="$(java_spec_for "$(command -v java)")"
    if is_java_21_or_newer "$java_spec"; then
      return 0
    fi
  fi

  fail "Java 21+ is required for conversation verification; set JAVA_HOME to a Java 21+ JDK"
}

try_frontend_node() {
  local candidate="$1"
  if [[ -n "$candidate" && -x "$candidate" ]]; then
    local candidate_version
    candidate_version="$(node_version_for "$candidate")"
    if is_frontend_node_supported "$candidate_version"; then
      NODE_BIN="$candidate"
      PATH="$(dirname "$candidate"):$PATH"
      export PATH
      NPM_BIN="$(command -v npm || true)"
      [[ -n "$NPM_BIN" ]] || fail "npm is required next to supported Node: $candidate"
      return 0
    fi
  fi
  return 1
}

resolve_frontend_node() {
  try_frontend_node "$(command -v node || true)" && return 0

  local candidate
  for candidate in /opt/homebrew/bin/node /usr/local/bin/node; do
    try_frontend_node "$candidate" && return 0
  done

  for search_root in "$HOME/.nvm/versions/node" /opt/homebrew/Cellar/node /usr/local/Cellar/node "$HOME/Library"; do
    [[ -d "$search_root" ]] || continue
    while IFS= read -r candidate; do
      try_frontend_node "$candidate" && return 0
    done < <(find "$search_root" -path "*/bin/node" -type f 2>/dev/null | sort -r)
  done

  fail "frontend dependencies require Node ^20.19.0, >=22.12.0, or >=24.0.0; current node is $(node -v 2>/dev/null || echo missing)"
}

quote_cmd() {
  printf ' %q' "$@"
  printf '\n'
}

print_provider_scaffold_preflight_dry_run() {
  printf 'provider-scaffold-syntax:'
  quote_cmd bash -n "$PROVIDER_SCAFFOLD_SCRIPT"
  printf 'provider-scaffold-dry-run:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER" \
    --dry-run
  printf 'provider-scaffold-output-check:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER" \
    --dry-run
  printf 'provider-scaffold-verify-dry-run:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_DRY_RUN_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_DRY_RUN_PROVIDER" \
    --verify \
    --dry-run
  printf 'provider-scaffold-invalid-adapter-key:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD__KEY --provider bad --dry-run
  printf 'provider-scaffold-trailing-underscore-key:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_ --provider bad --dry-run
  printf 'provider-scaffold-common-field-rejection:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_FIELD --provider bad --attribute text --dry-run
  printf 'provider-scaffold-java-keyword-rejection:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_KEYWORD --provider bad --attribute class --dry-run
  printf 'provider-scaffold-lower-camel-field-rejection:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" --adapter-key BAD_FIELD_NAME --provider bad --attribute ThreadId --dry-run
}

print_provider_scaffold_contract_probe_dry_run() {
  printf 'provider-scaffold-contract-probe:'
  quote_cmd "$PROVIDER_SCAFFOLD_SCRIPT" \
    --adapter-key "$PROVIDER_SCAFFOLD_PROBE_ADAPTER_KEY" \
    --provider "$PROVIDER_SCAFFOLD_PROBE_PROVIDER" \
    --verify
  printf 'provider-scaffold-contract-probe-cleanup:'
  quote_cmd "$0" "<remove disposable scaffold probe source, fixtures, and target output>"
}

print_adapter_fixture_lint_dry_run() {
  printf 'adapter-fixture-lint:'
  quote_cmd node "<validate JSON fixtures under $ADAPTER_CONTRACT_FIXTURE_DIR>"
}

print_adapter_fixture_contract_dry_run() {
  print_adapter_fixture_lint_dry_run
  if adapter_fixture_contract_class_cache_is_valid; then
    echo "backend-focused-class-cache: hit $BACKEND_TEST_RESOURCE_TARGET_DIR"
  else
    echo "backend-focused-class-cache: stale; run scripts/verify-conversation-focus.sh --adapter-contract-only"
  fi
  printf 'backend-clean-provider-main-classes:'
  quote_cmd find "$PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR" -type f \
    "(" -name "*ConversationReplyAdapter.class" -o -name "*ConversationReplyPayload.class" ")" -delete
  printf 'backend-prune-stale-provider-test-classes:'
  quote_cmd "$0" "<remove provider adapter/payload test classes whose source no longer exists>"
  printf 'backend-test-resources-sync:'
  quote_cmd "$0" "<sync reviewed roots from $BACKEND_TEST_RESOURCE_DIR -> $BACKEND_TEST_RESOURCE_TARGET_DIR>"
  printf 'backend: cd backend &&'
  quote_cmd "${BACKEND_CMD[@]}"
}

BACKEND_CMD=()
BACKEND_CLASSPATH_CMD=(mvn -pl canvas-engine -DincludeScope=test "-Dmdep.outputFile=$BACKEND_TEST_CLASSPATH_FILE" dependency:build-classpath)
FRONTEND_CMD=(npm run test -- --run "${FRONTEND_TESTS[@]}")
FRONTEND_BUILD_CMD=(npm run build)

[[ "$RUN_BACKEND" == "true" || "$RUN_FRONTEND" == "true" || "$RUN_ADAPTER_FIXTURE_LINT_ONLY" == "true" || "$RUN_ADAPTER_FIXTURE_CONTRACT_ONLY" == "true" || "$RUN_SCAFFOLD_PREFLIGHT_ONLY" == "true" || "$RUN_SCAFFOLD_CONTRACT_PROBE" == "true" ]] || fail "nothing to verify"

if [[ "$RUN_SCAFFOLD_PREFLIGHT_ONLY" == "true" ]]; then
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "conversation scaffold preflight dry-run passed"
    print_provider_scaffold_preflight_dry_run
    exit 0
  fi

  verify_provider_scaffold_preflight
  echo "conversation scaffold preflight passed"
  exit 0
fi

if [[ "$RUN_ADAPTER_FIXTURE_LINT_ONLY" == "true" ]]; then
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "conversation adapter fixture lint dry-run passed"
    print_adapter_fixture_lint_dry_run
    exit 0
  fi

  verify_adapter_fixture_lint
  echo "conversation adapter fixture lint passed"
  exit 0
fi

if [[ "$RUN_ADAPTER_FIXTURE_CONTRACT_ONLY" == "true" ]]; then
  [[ -d "$BACKEND_DIR" ]] || fail "backend directory is missing: $BACKEND_DIR"
  resolve_java_21
  export PATH="$JAVA_HOME/bin:$PATH"
  while IFS= read -r source; do
    PROVIDER_ADAPTER_MAIN_SOURCES+=("$source")
  done < <(provider_adapter_main_sources)
  FOCUSED_BACKEND_COMPILE_SOURCES+=("${PROVIDER_ADAPTER_MAIN_SOURCES[@]}")
  FOCUSED_BACKEND_COMPILE_SOURCES+=("${FOCUSED_BACKEND_TEST_SOURCES[@]}")
  assert_backend_sources_exist "${FOCUSED_BACKEND_COMPILE_SOURCES[@]}"
  BACKEND_SELECTOR="$(backend_selector_from_sources "${FOCUSED_BACKEND_TEST_SOURCES[@]}")"
  BACKEND_CMD=(
    mvn
    -pl
    canvas-engine
    surefire:test
    "-Dtest=$BACKEND_SELECTOR"
    -DfailIfNoTests=true
    -Dsurefire.failIfNoSpecifiedTests=true
  )

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "conversation adapter fixture contract dry-run passed"
    print_adapter_fixture_contract_dry_run
    exit 0
  fi

  verify_adapter_fixture_lint
  clean_provider_adapter_main_classes
  prune_stale_provider_adapter_test_classes
  if ! adapter_fixture_contract_class_cache_is_valid; then
    fail "adapter fixture contract class cache is stale; run scripts/verify-conversation-focus.sh --adapter-contract-only"
  fi
  (
    cd "$BACKEND_DIR"
    sync_backend_test_resources
    "${BACKEND_CMD[@]}"
  )
  echo "conversation adapter fixture contract passed"
  exit 0
fi

if [[ "$RUN_SCAFFOLD_CONTRACT_PROBE" == "true" ]]; then
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "conversation scaffold contract probe dry-run passed"
    print_provider_scaffold_contract_probe_dry_run
    exit 0
  fi

  verify_provider_scaffold_contract_probe
  echo "conversation scaffold contract probe passed"
  exit 0
fi

if [[ "$RUN_BACKEND" == "true" ]]; then
  [[ -d "$BACKEND_DIR" ]] || fail "backend directory is missing: $BACKEND_DIR"
  assert_provider_scaffold_available
  assert_backend_main_resource_roots_supported
  resolve_java_21
  export PATH="$JAVA_HOME/bin:$PATH"
  if [[ "$RUN_BACKEND_DOMAIN_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES+=("$source")
    done < <(conversation_domain_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_domain_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES+=("$source")
    done < <(conversation_domain_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_adapter_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES+=("$source")
    done < <(conversation_domain_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_service_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_DOMAIN_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_API_MAIN_SOURCES+=("$source")
    done < <(conversation_controller_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_controller_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_API_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_API_MAIN_SOURCES+=("$source")
    done < <(conversation_webhook_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_webhook_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_API_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_WHATSAPP_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_RUNTIME_MAIN_SOURCES+=("$source")
    done < <(backend_whatsapp_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(backend_whatsapp_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_RUNTIME_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_API_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_CONVERSATION_API_MAIN_SOURCES+=("$source")
    done < <(conversation_api_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_api_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_CONVERSATION_API_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_RUNTIME_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      BACKEND_RUNTIME_MAIN_SOURCES+=("$source")
    done < <(backend_runtime_main_sources)
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(backend_runtime_test_sources)
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_RUNTIME_MAIN_SOURCES[@]}")
  elif [[ "$RUN_BACKEND_SCHEMA_ONLY" == "true" ]]; then
    FOCUSED_BACKEND_TEST_SOURCES=()
    while IFS= read -r source; do
      FOCUSED_BACKEND_TEST_SOURCES+=("$source")
    done < <(conversation_schema_test_sources)
  else
    while IFS= read -r source; do
      PROVIDER_ADAPTER_MAIN_SOURCES+=("$source")
    done < <(provider_adapter_main_sources)
  fi
  if [[ "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
    && "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
    && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
    && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
    && "$RUN_BACKEND_API_ONLY" != "true" \
    && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
    && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
    while IFS= read -r source; do
      BACKEND_MAIN_JAVA_SOURCES+=("$source")
    done < <(backend_main_java_sources)
    discovered_test_sources=()
    while IFS= read -r source; do
      discovered_test_sources+=("$source")
    done < <(conversation_focused_test_sources)
    FOCUSED_BACKEND_TEST_SOURCES=("${discovered_test_sources[@]}")
    if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" == "true" ]]; then
      REFRESH_BACKEND_LOMBOK_TARGET_CLASSES=false
      FOCUSED_BACKEND_COMPILE_SOURCES+=("${BACKEND_MAIN_JAVA_SOURCES[@]}")
    elif backend_lombok_target_classes_cache_is_valid; then
      REFRESH_BACKEND_LOMBOK_TARGET_CLASSES=false
    else
      REFRESH_BACKEND_LOMBOK_TARGET_CLASSES=true
      while IFS= read -r source; do
        LOMBOK_MAIN_SOURCES+=("$source")
      done < <(lombok_main_sources)
      assert_backend_sources_exist "${LOMBOK_MAIN_SOURCES[@]}"
      FOCUSED_BACKEND_COMPILE_SOURCES+=("${LOMBOK_MAIN_SOURCES[@]}")
    fi
  fi
  if [[ "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
    && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
    && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
    && "$RUN_BACKEND_API_ONLY" != "true" \
    && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
    && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
    FOCUSED_BACKEND_COMPILE_SOURCES+=("${PROVIDER_ADAPTER_MAIN_SOURCES[@]}")
  fi
  FOCUSED_BACKEND_COMPILE_SOURCES+=("${FOCUSED_BACKEND_TEST_SOURCES[@]}")
  dedupe_focused_backend_compile_sources
  assert_backend_sources_exist "${FOCUSED_BACKEND_COMPILE_SOURCES[@]}"
  BACKEND_SELECTOR="$(backend_selector_from_sources "${FOCUSED_BACKEND_TEST_SOURCES[@]}")"
  BACKEND_CMD=(
    mvn
    -pl
    canvas-engine
    surefire:test
    "-Dtest=$BACKEND_SELECTOR"
    -DfailIfNoTests=true
    -Dsurefire.failIfNoSpecifiedTests=true
  )
fi

if [[ "$RUN_FRONTEND" == "true" ]]; then
  [[ -d "$FRONTEND_DIR" ]] || fail "frontend directory is missing: $FRONTEND_DIR"
  resolve_frontend_node
  discovered_frontend_tests=()
  if [[ "$RUN_FRONTEND_LOGIC_ONLY" == "true" ]]; then
    while IFS= read -r source; do
      discovered_frontend_tests+=("$source")
    done < <(frontend_conversation_logic_tests)
  elif [[ "$RUN_FRONTEND_CONVERSATION_ONLY" == "true" ]]; then
    while IFS= read -r source; do
      discovered_frontend_tests+=("$source")
    done < <(frontend_conversation_tests)
  elif [[ "$RUN_FRONTEND_AUTHORING_ONLY" == "true" ]]; then
    while IFS= read -r source; do
      discovered_frontend_tests+=("$source")
    done < <(frontend_authoring_tests)
  else
    while IFS= read -r source; do
      discovered_frontend_tests+=("$source")
    done < <(frontend_focused_tests)
  fi
  FRONTEND_TESTS=("${discovered_frontend_tests[@]}")
  assert_frontend_tests_exist "${FRONTEND_TESTS[@]}"
  FRONTEND_CMD=("$NPM_BIN" run test -- --run "${FRONTEND_TESTS[@]}")
  FRONTEND_BUILD_CMD=("$NPM_BIN" run build)
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "conversation focus verification dry-run passed"
  if [[ "$RUN_BACKEND" == "true" ]]; then
    echo "JAVA_HOME: $JAVA_HOME"
    echo "java_specification_version: $(java_spec_for "$JAVA_HOME/bin/java")"
    print_provider_scaffold_preflight_dry_run
    print_adapter_fixture_lint_dry_run
    if [[ "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
      && "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
      && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
      && "$RUN_BACKEND_API_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" ]]; then
      printf 'backend-main-resources-sync:'
      quote_cmd "$0" "<sync $BACKEND_MAIN_RESOURCE_DIR -> $BACKEND_MAIN_RESOURCE_TARGET_DIR>"
    fi
    if backend_test_classpath_cache_is_valid; then
      echo "backend-classpath-cache: hit $BACKEND_TEST_CLASSPATH_FILE"
    else
      printf 'backend-classpath-refresh: cd backend &&'
      quote_cmd "${BACKEND_CLASSPATH_CMD[@]}"
    fi
    if [[ "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
      && "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
      && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
      && "$RUN_BACKEND_API_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
      && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
      printf 'backend-clean-main-classes:'
      quote_cmd find "$CANVAS_MAIN_TARGET_PACKAGE_DIR" -type f -name "*.class" -delete
    fi
    if [[ "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
      && "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
      && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
      && "$RUN_BACKEND_API_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
      && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
      if [[ "$REFRESH_BACKEND_LOMBOK_TARGET_CLASSES" == "true" ]]; then
        echo "backend-lombok-target-classes-cache: refresh $BACKEND_TEST_RESOURCE_TARGET_DIR"
        echo "backend-lombok-target-classes-refresh-count: ${#LOMBOK_MAIN_SOURCES[@]}"
        echo "backend-lombok-target-classes-main-source-count: ${#BACKEND_MAIN_JAVA_SOURCES[@]}"
        printf 'backend-clean-test-classes:'
        quote_cmd find "$BACKEND_TEST_RESOURCE_TARGET_DIR/org/chovy/canvas" -type f -name "*.class" -delete
        printf 'backend-lombok-target-classes-restore: cd backend && javac --release 21 -cp <test-classpath> -processorpath <test-classpath> -processor %q -implicit:class -sourcepath canvas-engine/src/main/java:canvas-engine/src/test/java -d canvas-engine/target/test-classes' "$LOMBOK_PROCESSOR"
        quote_cmd "@$FOCUSED_BACKEND_WORK_DIR/lombok-target-restore-sources.txt"
      else
        echo "backend-lombok-target-classes-cache: hit $BACKEND_TEST_RESOURCE_TARGET_DIR"
      fi
    fi
    if [[ "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
      && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
      && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
      && "$RUN_BACKEND_API_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
      && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
      printf 'backend-test-resources-sync:'
      quote_cmd "$0" "<sync reviewed roots from $BACKEND_TEST_RESOURCE_DIR -> $BACKEND_TEST_RESOURCE_TARGET_DIR>"
      printf 'backend-test-resources-copy:'
      quote_cmd "$0" "<copy $BACKEND_TEST_RESOURCE_DIR -> $BACKEND_TEST_RESOURCE_TARGET_DIR>"
    fi
    if [[ "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
      && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
      printf 'backend-clean-provider-main-classes:'
      quote_cmd find "$PROVIDER_ADAPTER_MAIN_TARGET_PACKAGE_DIR" -type f \
        "(" -name "*ConversationReplyAdapter.class" -o -name "*ConversationReplyPayload.class" ")" -delete
      printf 'backend-clean-provider-test-classes:'
      quote_cmd find "$PROVIDER_ADAPTER_TARGET_PACKAGE_DIR" -type f \
        "(" -name "*ConversationReplyAdapter.class" -o -name "*ConversationReplyPayload.class" ")" -delete
    fi
    echo "backend-focused-source-count: ${#FOCUSED_BACKEND_COMPILE_SOURCES[@]}"
    echo "backend-focused-sources-file: $FOCUSED_BACKEND_SOURCES_FILE"
    if backend_focused_compile_uses_lombok_processor; then
      printf 'backend-focused-compile: cd backend && javac --release 21 -cp <test-classpath> -processorpath <test-classpath> -processor %q' "$LOMBOK_PROCESSOR"
      if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" == "true" ]]; then
        printf ' -implicit:none'
      else
        printf ' -implicit:class'
      fi
      printf ' -sourcepath canvas-engine/src/main/java:canvas-engine/src/test/java -d canvas-engine/target/test-classes'
    else
      printf 'backend-focused-compile: cd backend && javac --release 21 -cp <test-classpath> -proc:none -implicit:none -sourcepath canvas-engine/src/main/java:canvas-engine/src/test/java -d canvas-engine/target/test-classes'
    fi
    quote_cmd "@$FOCUSED_BACKEND_SOURCES_FILE"
    if [[ "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" == "true" ]]; then
      echo "backend-surefire: skipped for adapter source compile preflight"
    elif [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" == "true" ]]; then
      echo "backend-surefire: skipped for backend source compile preflight"
    else
      printf 'backend: cd backend &&'
      quote_cmd "${BACKEND_CMD[@]}"
    fi
  fi
  if [[ "$RUN_FRONTEND" == "true" ]]; then
    echo "NODE_BIN: $NODE_BIN"
    echo "node_version: $(node_version_for "$NODE_BIN")"
    echo "NPM_BIN: $NPM_BIN"
    printf 'frontend: cd frontend &&'
    quote_cmd "${FRONTEND_CMD[@]}"
    if [[ "$RUN_FRONTEND_BUILD" == "true" ]]; then
      printf 'frontend-build: cd frontend &&'
      quote_cmd "${FRONTEND_BUILD_CMD[@]}"
    fi
  fi
  exit 0
fi

if [[ "$RUN_BACKEND" == "true" ]]; then
  echo "Running conversation backend verification with java.specification.version=$(java_spec_for "$JAVA_HOME/bin/java")"
  verify_provider_scaffold_preflight
  verify_adapter_fixture_lint
  (
    cd "$BACKEND_DIR"
    refresh_backend_test_classpath
    if [[ "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" != "true" ]]; then
      if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
        && "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
        && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
        && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
        && "$RUN_BACKEND_API_ONLY" != "true" \
        && "$RUN_BACKEND_RUNTIME_ONLY" != "true" ]]; then
        sync_backend_main_resources
      fi
      if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
        && "$RUN_ADAPTER_CONTRACT_ONLY" != "true" \
        && "$RUN_BACKEND_DOMAIN_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_ADAPTERS_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
        && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
        && "$RUN_BACKEND_API_ONLY" != "true" \
        && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
        && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
        clean_canvas_main_classes
      fi
      if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" != "true" \
        && "$RUN_BACKEND_API_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_CONTROLLERS_ONLY" != "true" \
        && "$RUN_BACKEND_CONVERSATION_WEBHOOKS_ONLY" != "true" \
        && "$RUN_BACKEND_WHATSAPP_ONLY" != "true" \
        && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
        && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
        sync_backend_test_resources
      fi
      if [[ "$REFRESH_BACKEND_LOMBOK_TARGET_CLASSES" == "true" ]]; then
        clean_canvas_test_classes
      fi
    fi
    if [[ "$RUN_BACKEND_CONVERSATION_SERVICES_ONLY" != "true" \
      && "$RUN_BACKEND_RUNTIME_ONLY" != "true" \
      && "$RUN_BACKEND_SCHEMA_ONLY" != "true" ]]; then
      clean_provider_adapter_classes
    fi
    TEST_CP="$(cat "$BACKEND_TEST_CLASSPATH_FILE"):$ENGINE_DIR/target/classes:$ENGINE_DIR/target/test-classes"
    write_focused_backend_sources_file
    BACKEND_JAVAC_ARGS=(
      --release
      21
      -cp
      "$TEST_CP"
    )
    if backend_focused_compile_uses_lombok_processor; then
      BACKEND_JAVAC_ARGS+=(
        -processorpath
        "$TEST_CP"
        -processor
        "$LOMBOK_PROCESSOR"
      )
      if [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" == "true" ]]; then
        BACKEND_JAVAC_ARGS+=(-implicit:none)
      else
        BACKEND_JAVAC_ARGS+=(-implicit:class)
      fi
    else
      BACKEND_JAVAC_ARGS+=(
        -proc:none
        -implicit:none
      )
    fi
    BACKEND_JAVAC_ARGS+=(
      -sourcepath
      "canvas-engine/src/main/java:canvas-engine/src/test/java"
      -d
      "$ENGINE_DIR/target/test-classes"
      "@$FOCUSED_BACKEND_SOURCES_FILE"
    )
    "$JAVA_HOME/bin/javac" \
      "${BACKEND_JAVAC_ARGS[@]}"
    if [[ "$RUN_ADAPTER_SOURCE_COMPILE_ONLY" == "true" ]]; then
      echo "conversation adapter source compile passed"
    elif [[ "$RUN_BACKEND_SOURCE_COMPILE_ONLY" == "true" ]]; then
      echo "conversation backend source compile passed"
    else
      assert_backend_selected_tests_compiled "${FOCUSED_BACKEND_TEST_SOURCES[@]}"
      "${BACKEND_CMD[@]}"
      restore_backend_lombok_target_classes_cache_outputs
      write_backend_lombok_target_classes_cache
    fi
  )
fi

if [[ "$RUN_FRONTEND" == "true" ]]; then
  echo "Running conversation frontend verification"
  (
    cd "$FRONTEND_DIR"
    "${FRONTEND_CMD[@]}"
    if [[ "$RUN_FRONTEND_BUILD" == "true" ]]; then
      "${FRONTEND_BUILD_CMD[@]}"
    fi
  )
fi
