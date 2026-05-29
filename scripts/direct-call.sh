#!/bin/bash
# Canvas 直调触发脚本（免鉴权，内网调用）
# 默认画布 ID=11

HOST="${CANVAS_HOST:-localhost:8080}"
CANVAS_ID="${1:-11}"
BODY='{"inputParams":{},"idempotencyKey":"direct-'${CANVAS_ID}'-'$(date +%s)'"}'

echo "=== 直调触发画布 #${CANVAS_ID} ==="
echo "Host: http://${HOST}"
echo "Body: ${BODY}"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "http://${HOST}/canvas/execute/direct/${CANVAS_ID}" \
  -H "Content-Type: application/json" \
  -d "${BODY}"