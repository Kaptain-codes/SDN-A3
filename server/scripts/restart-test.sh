#!/usr/bin/env bash
# Restart test: start server, sync a plan, kill the server, start it again, check the plan is still there.
# Run from anywhere after: mvn package
set -euo pipefail
cd "$(dirname "$0")/.."
BASE=http://localhost:${STEMMATE_PORT:-8080}
PLAN=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)
ITEM=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)

wait_for_server() {
  for _ in $(seq 1 20); do curl -sf "$BASE/health" >/dev/null && return 0; sleep 0.5; done
  echo "server did not start"; exit 1
}

echo "[1] start server";  java -jar target/stemmate-server.jar > restart-test.log 2>&1 & PID=$!; wait_for_server
echo "[2] sync plan $PLAN"
curl -s -X POST "$BASE/sync?deviceId=restart-test" \
  --data-binary "id=$ITEM&action=SAVE_PLAN&planId=$PLAN&title=Restart%20test&safety=Teacher%20checks%20wiring&updatedAt=$(date +%s)000"
echo "[3] kill server";  kill -9 "$PID"; sleep 1
echo "[4] start again";  java -jar target/stemmate-server.jar >> restart-test.log 2>&1 & PID=$!; wait_for_server
echo "[5] read it back"
if curl -s "$BASE/plans?deviceId=restart-test" | grep -q "$PLAN"; then RESULT=PASS; else RESULT=FAIL; fi
kill "$PID"
echo "$RESULT: plan $( [ $RESULT = PASS ] && echo survived || echo was lost after ) a hard restart"
[ $RESULT = PASS ]
