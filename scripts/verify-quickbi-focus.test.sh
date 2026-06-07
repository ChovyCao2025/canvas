#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_SCRIPT="$ROOT_DIR/scripts/verify-quickbi-focus.sh"
NODE_BIN="${NODE_BIN:-/opt/homebrew/bin/node}"

if [[ ! -x "$NODE_BIN" ]]; then
  NODE_BIN="$(command -v node)"
fi

DRY_RUN_OUTPUT="$("$VERIFY_SCRIPT" --backend-only --dry-run)"

DRY_RUN_OUTPUT="$DRY_RUN_OUTPUT" "$NODE_BIN" -e '
  const output = process.env.DRY_RUN_OUTPUT;
  const compileLine = output.split("\n").find((line) => line.startsWith("backend-compile:")) || "";
  if (!compileLine.includes("-Dmaven.compiler.useIncrementalCompilation=false")) {
    throw new Error("backend compile dry-run must disable Maven incremental compilation");
  }
  if (!compileLine.includes("-Dmaven.compiler.forceJavacCompilerUse=true")) {
    throw new Error("backend compile dry-run must force javac compiler use");
  }
'

echo "quickbi focus verification script test passed"
