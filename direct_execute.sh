#!/bin/bash

# Usage: ./direct_execute.sh <canvas_id> [user_id] [count]
#   canvas_id  - 画布ID (必填)
#   user_id    - 用户ID前缀，默认 "user_"
#   count      - 执行次数，默认 1

CANVAS_ID=${1:?"Usage: $0 <canvas_id> [user_id_prefix] [count]"}
USER_PREFIX=${2:-"user_"}
COUNT=${3:-1}
BASE_URL="http://localhost:8080/canvas/execute/direct"

for i in $(seq 1 "$COUNT"); do
  USER_ID="${USER_PREFIX}$(printf '%03d' $i)"
  IDEMPOTENCY_KEY="direct-${CANVAS_ID}-${USER_ID}-$(openssl rand -hex 4)"

  echo "[$i/$COUNT] canvas=$CANVAS_ID user=$USER_ID key=$IDEMPOTENCY_KEY"

  RESPONSE=$(curl -s -X POST "${BASE_URL}/${CANVAS_ID}" \
    -H "Content-Type: application/json" \
    -d "{
      \"inputParams\": {
        \"userId\": \"${USER_ID}\"
      },
      \"idempotencyKey\": \"${IDEMPOTENCY_KEY}\"
    }")

  echo "  -> $RESPONSE"
  echo ""
done
