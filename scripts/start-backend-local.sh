#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
CLASSIFIER="$ROOT_DIR/scripts/classify-flyway-validate-failure.sh"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-canvas-mysql}"
DEFAULT_DB_NAME="canvas_db"
FRESH_DB_NAME="canvas_boot_local"
DB_NAME="$DEFAULT_DB_NAME"
DB_NAME_SET=0
FRESH_DB=0
DRY_RUN=0
SERVER_PORT=""
CANVAS_JWT_SECRET_VALUE="${CANVAS_JWT_SECRET:-local-dev-jwt-secret-at-least-32-bytes}"

usage() {
  cat <<'EOF'
Start the local canvas-boot backend with the current runtime entrypoint.

Options:
  --fresh-db             Recreate a disposable local database before startup.
  --db-name <name>       Override the database name. Defaults to canvas_db.
                         When --fresh-db is set and no name is provided, uses
                         canvas_boot_local.
  --server-port <port>   Override the backend HTTP port.
  --dry-run              Print the commands without executing them.
  -h, --help             Show this help.
EOF
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --fresh-db)
      FRESH_DB=1
      shift
      ;;
    --db-name)
      [[ $# -ge 2 ]] || fail "--db-name requires a value"
      DB_NAME="$2"
      DB_NAME_SET=1
      shift 2
      ;;
    --server-port)
      [[ $# -ge 2 ]] || fail "--server-port requires a value"
      SERVER_PORT="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

if [[ $FRESH_DB -eq 1 && $DB_NAME_SET -eq 0 ]]; then
  DB_NAME="$FRESH_DB_NAME"
fi

[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || fail "database name must match ^[A-Za-z0-9_]+$"
[[ -z "$SERVER_PORT" || "$SERVER_PORT" =~ ^[0-9]+$ ]] || fail "server port must be numeric"

DATASOURCE_URL="jdbc:mysql://localhost:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
SPRING_BOOT_ARGUMENTS=""
if [[ -n "$SERVER_PORT" ]]; then
  SPRING_BOOT_ARGUMENTS="--server.port=${SERVER_PORT}"
fi

if [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  export JAVA_HOME
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if command -v java >/dev/null 2>&1; then
  java_version_output="$(java -version 2>&1 | head -n 1)"
  [[ "$java_version_output" == *" 21."* || "$java_version_output" == *"\"21"* ]] \
    || fail "Java 21 is required, but current runtime is: $java_version_output"
else
  fail "java command not found"
fi

print_plan() {
  echo "Target database: $DB_NAME"
  echo "Datasource URL: $DATASOURCE_URL"
  if [[ $FRESH_DB -eq 1 ]]; then
    echo "SQL: DROP DATABASE IF EXISTS \`${DB_NAME}\`; CREATE DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  else
    echo "SQL: CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  fi
  echo "Command:"
  if [[ -n "$SPRING_BOOT_ARGUMENTS" ]]; then
    echo "cd backend && mvn -pl canvas-boot -am -DskipTests install && CANVAS_JWT_SECRET=$CANVAS_JWT_SECRET_VALUE SPRING_DATASOURCE_URL=$DATASOURCE_URL mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.arguments=$SPRING_BOOT_ARGUMENTS"
  else
    echo "cd backend && mvn -pl canvas-boot -am -DskipTests install && CANVAS_JWT_SECRET=$CANVAS_JWT_SECRET_VALUE SPRING_DATASOURCE_URL=$DATASOURCE_URL mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run"
  fi
}

ensure_database() {
  local sql
  if [[ $FRESH_DB -eq 1 ]]; then
    sql="DROP DATABASE IF EXISTS \`${DB_NAME}\`; CREATE DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  else
    sql="CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  fi
  docker exec "$MYSQL_CONTAINER" mysql -uroot -proot -e "$sql"
}

run_backend() {
  local classification_file
  local log_file
  local build_status
  local status
  local -a cmd

  log_file="$(mktemp "${TMPDIR:-/tmp}/canvas-boot-start.XXXXXX")"
  classification_file="$(mktemp "${TMPDIR:-/tmp}/canvas-boot-classify.XXXXXX")"
  cmd=(mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run)
  if [[ -n "$SPRING_BOOT_ARGUMENTS" ]]; then
    cmd+=("-Dspring-boot.run.arguments=$SPRING_BOOT_ARGUMENTS")
  fi

  echo "Backend log: $log_file"
  (
    cd "$BACKEND_DIR"
    mvn -pl canvas-boot -am -DskipTests install
  )
  build_status=$?
  if [[ $build_status -ne 0 ]]; then
    return "$build_status"
  fi

  set +e
  (
    cd "$BACKEND_DIR"
    CANVAS_JWT_SECRET="$CANVAS_JWT_SECRET_VALUE" \
    SPRING_DATASOURCE_URL="$DATASOURCE_URL" \
    "${cmd[@]}"
  ) 2>&1 | tee "$log_file"
  status=${PIPESTATUS[0]}
  set -e

  if [[ $status -ne 0 && -x "$CLASSIFIER" ]]; then
    echo
    if "$CLASSIFIER" --input-file "$log_file" >"$classification_file"; then
      cat "$classification_file"
    else
      cat "$classification_file"
    fi
    if grep -Fq "schema-history checksum drift" "$classification_file" \
      && [[ $FRESH_DB -eq 0 && "$DB_NAME" == "$DEFAULT_DB_NAME" ]]; then
      echo "Hint: rerun with a disposable database to bypass local Flyway checksum drift:" >&2
      echo "  bash scripts/start-backend-local.sh --fresh-db" >&2
    fi
  fi

  return "$status"
}

print_plan
if [[ $DRY_RUN -eq 1 ]]; then
  exit 0
fi

ensure_database
run_backend
