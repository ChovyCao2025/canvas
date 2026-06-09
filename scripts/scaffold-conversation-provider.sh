#!/usr/bin/env bash
# Scaffolds a new conversation provider adapter and focused verification hooks.
#
# Generated files follow the existing adapter contract layout so new providers can
# be added with consistent DTOs, tests, and dry-run guidance.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_DIR="$ROOT_DIR/backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation"
FIXTURE_DIR="$ROOT_DIR/backend/canvas-engine/src/test/resources/conversation/adapter-contracts"

ADAPTER_KEY=""
PROVIDER=""
INTERACTIVE_ID_FIELD="actionId"
INTERACTIVE_TEXT_FIELD="actionLabel"
ATTRIBUTES=()
DRY_RUN=false
FORCE=false
VERIFY=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/scaffold-conversation-provider.sh --adapter-key KEY --provider PROVIDER [options]

Generates a constructor-only provider conversation adapter, payload record, and TEXT/INTERACTIVE
adapter-contract fixtures that are intended to pass scripts/verify-conversation-focus.sh --adapter-contract-only.

Options:
  --adapter-key KEY             Uppercase/snake adapter key, for example EMAIL or LINE_DM
  --provider PROVIDER           Raw provider value to put in generated fixtures, for example line
  --attribute FIELD             Extra provider-specific text field to include in payload and text fixture
  --interactive-id-field FIELD  Interactive id field name; default actionId
  --interactive-text-field FIELD Interactive display text field name; default actionLabel
  --force                       Overwrite generated files if they already exist
  --verify                      Run adapter-contract-only verification after generation
  --dry-run                     Print the files that would be generated
  --help, -h                    Show this help
EOF
}

print_next_steps() {
  echo "next verification:"
  echo "  scripts/verify-conversation-focus.sh --adapter-fixture-lint-only"
  echo "  scripts/verify-conversation-focus.sh --adapter-contract-only"
}

print_verify_dry_run() {
  echo "would run verification:"
  echo "  scripts/verify-conversation-focus.sh --adapter-contract-only"
}

run_adapter_contract_verification() {
  echo "Running adapter-contract-only verification:"
  echo "  scripts/verify-conversation-focus.sh --adapter-contract-only"
  "$ROOT_DIR/scripts/verify-conversation-focus.sh" --adapter-contract-only
}

repo_relative_path() {
  local path="$1"
  printf '%s\n' "${path#$ROOT_DIR/}"
}

print_generated_files() {
  printf '  %s\n' \
    "$(repo_relative_path "$adapter_file")" \
    "$(repo_relative_path "$payload_file")" \
    "$(repo_relative_path "$text_fixture")" \
    "$(repo_relative_path "$interactive_fixture")"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --adapter-key)
      ADAPTER_KEY="${2:-}"
      shift 2
      ;;
    --provider)
      PROVIDER="${2:-}"
      shift 2
      ;;
    --attribute)
      ATTRIBUTES+=("${2:-}")
      shift 2
      ;;
    --interactive-id-field)
      INTERACTIVE_ID_FIELD="${2:-}"
      shift 2
      ;;
    --interactive-text-field)
      INTERACTIVE_TEXT_FIELD="${2:-}"
      shift 2
      ;;
    --force)
      FORCE=true
      shift
      ;;
    --verify)
      VERIFY=true
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

[[ -n "$ADAPTER_KEY" ]] || fail "--adapter-key is required"
[[ -n "$PROVIDER" ]] || fail "--provider is required"
[[ "$ADAPTER_KEY" =~ ^[A-Z][A-Z0-9_]*$ ]] || fail "--adapter-key must be uppercase snake case"
[[ "$ADAPTER_KEY" != *_ ]] || fail "--adapter-key must not end with underscore"
[[ "$ADAPTER_KEY" != *__* ]] || fail "--adapter-key must not contain consecutive underscores"
[[ "$PROVIDER" =~ ^[A-Za-z0-9_.:-]+$ ]] || fail "--provider must contain only letters, numbers, dot, colon, underscore, or dash"

is_java_identifier() {
  [[ "$1" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]
}

is_lower_camel_field() {
  [[ "$1" =~ ^[a-z][A-Za-z0-9]*$ ]]
}

is_java_keyword() {
  case "$1" in
    abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null|_|record|sealed|permits)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_common_payload_field() {
  case "$1" in
    canvasId|versionId|executionId|userId|provider|externalMessageId|eventId|text|intent|attributes|occurredAt)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

validate_provider_field() {
  local option="$1"
  local field="$2"

  is_java_identifier "$field" || fail "$option must be a Java identifier: $field"
  is_lower_camel_field "$field" || fail "$option must be lower camel case: $field"
  ! is_java_keyword "$field" || fail "$option must not be a Java keyword: $field"
  ! is_common_payload_field "$field" || fail "$option must not reuse a common payload field: $field"
}

assert_unique_attributes() {
  local outer_index inner_index field other

  for ((outer_index = 0; outer_index < ${#ATTRIBUTES[@]}; outer_index++)); do
    field="${ATTRIBUTES[$outer_index]}"
    for ((inner_index = outer_index + 1; inner_index < ${#ATTRIBUTES[@]}; inner_index++)); do
      other="${ATTRIBUTES[$inner_index]}"
      [[ "$field" != "$other" ]] || fail "--attribute must not be repeated: $field"
    done
  done
}

validate_provider_field "--interactive-id-field" "$INTERACTIVE_ID_FIELD"
validate_provider_field "--interactive-text-field" "$INTERACTIVE_TEXT_FIELD"
[[ "$INTERACTIVE_ID_FIELD" != "$INTERACTIVE_TEXT_FIELD" ]] || fail "--interactive-id-field and --interactive-text-field must be different"
if [[ "${#ATTRIBUTES[@]}" -gt 0 ]]; then
  for attribute in "${ATTRIBUTES[@]}"; do
    validate_provider_field "--attribute" "$attribute"
  done
fi
assert_unique_attributes

lower_key="$(printf '%s' "$ADAPTER_KEY" | tr '[:upper:]' '[:lower:]')"
kebab_key="${lower_key//_/-}"
missing_payload_message="${lower_key//_/ } conversation reply payload is required"
expected_provider="$(printf '%s' "$PROVIDER" | tr '[:lower:]' '[:upper:]')"

to_pascal() {
  local value="$1"
  local token
  local result=""
  IFS='_' read -ra parts <<< "$value"
  for token in "${parts[@]}"; do
    result+="$(printf '%s' "${token:0:1}" | tr '[:lower:]' '[:upper:]')"
    result+="$(printf '%s' "${token:1}" | tr '[:upper:]' '[:lower:]')"
  done
  printf '%s' "$result"
}

class_prefix="$(to_pascal "$ADAPTER_KEY")"
adapter_class="${class_prefix}ConversationReplyAdapter"
payload_class="${class_prefix}ConversationReplyPayload"
adapter_file="$MAIN_DIR/$adapter_class.java"
payload_file="$MAIN_DIR/$payload_class.java"
text_fixture="$FIXTURE_DIR/$kebab_key-text.json"
interactive_fixture="$FIXTURE_DIR/$kebab_key-interactive.json"

unique_provider_fields() {
  local field
  {
    if [[ "${#ATTRIBUTES[@]}" -gt 0 ]]; then
      for field in "${ATTRIBUTES[@]}"; do
        printf '%s\n' "$field"
      done
    fi
    printf '%s\n' "$INTERACTIVE_ID_FIELD" "$INTERACTIVE_TEXT_FIELD"
  } | awk '!seen[$0]++'
}

provider_fields=()
while IFS= read -r field; do
  provider_fields+=("$field")
done < <(unique_provider_fields)

for file in "$adapter_file" "$payload_file" "$text_fixture" "$interactive_fixture"; do
  if [[ -e "$file" && "$FORCE" != "true" ]]; then
    fail "refusing to overwrite existing file without --force: $file"
  fi
done

if [[ "$DRY_RUN" == "true" ]]; then
  echo "conversation provider scaffold dry-run passed"
  echo "adapter_key: $ADAPTER_KEY"
  echo "provider: $PROVIDER"
  echo "files:"
  print_generated_files
  if [[ "$VERIFY" == "true" ]]; then
    print_verify_dry_run
  else
    print_next_steps
  fi
  exit 0
fi

mkdir -p "$MAIN_DIR" "$FIXTURE_DIR"

payload_provider_fields=""
for field in "${provider_fields[@]}"; do
  payload_provider_fields+="        String $field,"$'\n'
done

adapter_provider_attributes=""
for index in "${!provider_fields[@]}"; do
  field="${provider_fields[$index]}"
  suffix=","
  if [[ "$index" -eq "$((${#provider_fields[@]} - 1))" ]]; then
    suffix=""
  fi
  adapter_provider_attributes+="                        providerAttribute(\"$field\", $payload_class::$field)$suffix"$'\n'
done

text_raw_attribute_lines=""
text_expected_attribute_lines=""
if [[ "${#ATTRIBUTES[@]}" -gt 0 ]]; then
  for attribute in "${ATTRIBUTES[@]}"; do
    text_raw_attribute_lines+="    \"$attribute\": \"$attribute-fixture\","$'\n'
    text_expected_attribute_lines+="    \"$attribute\": \"$attribute-fixture\","$'\n'
  done
fi

cat > "$payload_file" <<EOF
package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record $payload_class(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String provider,
${payload_provider_fields}        String externalMessageId,
        String eventId,
        String text,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
EOF

cat > "$adapter_file" <<EOF
package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class $adapter_class
        extends AbstractProviderConversationReplyAdapter<$payload_class> {

    public $adapter_class() {
        super(
                "$ADAPTER_KEY",
                $payload_class.class,
                "$missing_payload_message",
                List.of(
${adapter_provider_attributes}                ),
                $payload_class::$INTERACTIVE_TEXT_FIELD,
                List.of(
                        $payload_class::$INTERACTIVE_ID_FIELD,
                        $payload_class::$INTERACTIVE_TEXT_FIELD));
    }
}
EOF

cat > "$text_fixture" <<EOF
{
  "name": "$lower_key text fixture",
  "adapterKey": "$lower_key",
  "rawPayload": {
    "canvasId": 700,
    "versionId": 800,
    "executionId": "$kebab_key-exec-1",
    "userId": "$lower_key:user-fixture",
    "provider": "$PROVIDER",
${text_raw_attribute_lines}    "externalMessageId": "$kebab_key-text-fixture",
    "eventId": "$kebab_key-text-event-fixture",
    "text": "hello",
    "intent": "GREETING",
    "occurredAt": "2026-06-02T09:00:00"
  },
  "expectedChannel": "$ADAPTER_KEY",
  "expectedProvider": "$expected_provider",
  "expectedMessageType": "TEXT",
  "expectedText": "hello",
  "expectedExternalMessageId": "$kebab_key-text-fixture",
  "expectedEventId": "$kebab_key-text-event-fixture",
  "expectedAttributes": {
${text_expected_attribute_lines}    "adapter": "$ADAPTER_KEY"
  },
  "missingPayloadMessage": "$missing_payload_message"
}
EOF

cat > "$interactive_fixture" <<EOF
{
  "name": "$lower_key interactive fixture",
  "adapterKey": "$lower_key",
  "rawPayload": {
    "userId": "$lower_key:user-fixture",
    "provider": "$PROVIDER",
    "externalMessageId": "$kebab_key-interactive-fixture",
    "eventId": "$kebab_key-interactive-event-fixture",
    "$INTERACTIVE_ID_FIELD": "book-demo",
    "$INTERACTIVE_TEXT_FIELD": "Book a demo",
    "intent": "BOOK_DEMO"
  },
  "expectedChannel": "$ADAPTER_KEY",
  "expectedProvider": "$expected_provider",
  "expectedMessageType": "INTERACTIVE",
  "expectedText": "Book a demo",
  "expectedExternalMessageId": "$kebab_key-interactive-fixture",
  "expectedEventId": "$kebab_key-interactive-event-fixture",
  "expectedAttributes": {
    "adapter": "$ADAPTER_KEY",
    "$INTERACTIVE_ID_FIELD": "book-demo",
    "$INTERACTIVE_TEXT_FIELD": "Book a demo"
  },
  "missingPayloadMessage": "$missing_payload_message"
}
EOF

echo "Generated conversation provider scaffold:"
print_generated_files
if [[ "$VERIFY" == "true" ]]; then
  run_adapter_contract_verification
else
  print_next_steps
fi
