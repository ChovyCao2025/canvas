#!/bin/bash
# Canvas 7 事件上报触发脚本
# 画布: 示例：事件触发 API 调用 (id=7)
# 事件码: ORDER_COMPLETE

SECRET="${CANVAS_EVENT_REPORT_SECRET:-canvas-event-report-secret-2026!!}"
HOST="${CANVAS_HOST:-localhost:8080}"

BODY='{"eventCode":"ORDER_COMPLETE","userId":"user_016","attributes":{"orderId":"ORD-20260528-001","amount":99.9,"orderTime":"2026-05-28T10:30:00"}}'

TIMESTAMP=$(( $(date +%s) * 1000 ))
SIGNATURE="sha256=$(printf '%s\n%s' "${TIMESTAMP}" "${BODY}" | openssl dgst -sha256 -hmac "${SECRET}" | awk '{print $NF}')"

echo "=== 事件上报 ==="
echo "Secret:   ${SECRET}"
echo "Timestamp: ${TIMESTAMP}"
echo "Signature: ${SIGNATURE}"
echo "Body:     ${BODY}"
echo ""

curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "http://${HOST}/canvas/events/report" \
  -H "Content-Type: application/json" \
  -H "X-Canvas-Timestamp: ${TIMESTAMP}" \
  -H "X-Canvas-Signature: ${SIGNATURE}" \
  -d "${BODY}"