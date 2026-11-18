#!/usr/bin/env bash
# Scripted console session that parks vehicles, checks reports,
# exits, restarts, and confirms the data was saved to disk.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! command -v "${JAVA_HOME:+$JAVA_HOME/bin/javac}" >/dev/null 2>&1; then
  if command -v javac >/dev/null 2>&1; then
    JAVA_BIN=java
  fi
fi

OUT="$ROOT/out"
DATA="$ROOT/data/scripted-session.dat"
rm -f "$DATA"

echo "========== SESSION 1: park three vehicles, reject bad input, unpark one =========="
# 1 park bike, 1 park car, invalid type, invalid plate, duplicate car,
# 1 park truck, 3 availability, 5 parked list, 4 ticket details,
# 2 unpark bike by registration, 6 revenue today, 7 exit
SESSION1_OUTPUT=$(mktemp)
# shellcheck disable=SC2086
printf '%s\n' \
  "1" "1" "MH12BK1001" \
  "1" "2" "mh12cr2002" \
  "1" "9" \
  "1" "2" "??????" \
  "1" "2" "MH12CR2002" \
  "1" "3" "MH12TR3003" \
  "3" \
  "5" \
  "4" "TKT-$(date +%Y%m%d)-0001" \
  "2" "MH12BK1001" \
  "6" "" \
  "7" | $JAVA_BIN -cp "$OUT" com.mansi.parkinglot.ParkingLotApp --data "$DATA" --config 2,2,2,1 | tee "$SESSION1_OUTPUT"

echo
echo "========== SESSION 2: restart and confirm remaining vehicles are still parked =========="
SESSION2_OUTPUT=$(mktemp)
printf '%s\n' \
  "5" \
  "3" \
  "2" "MH12CR2002" \
  "2" "MH12TR3003" \
  "6" "" \
  "7" | $JAVA_BIN -cp "$OUT" com.mansi.parkinglot.ParkingLotApp --data "$DATA" --config 2,2,2,1 | tee "$SESSION2_OUTPUT"

fail=0
assert_contains() {
  local file="$1" needle="$2" label="$3"
  if grep -F -q -- "$needle" "$file"; then
    echo "  PASS  $label"
  else
    echo "  FAIL  $label  (missing: $needle)"
    fail=1
  fi
}

echo
echo "========== Checks =========="
assert_contains "$SESSION1_OUTPUT" "Vehicle parked successfully." "session 1 parked a vehicle"
assert_contains "$SESSION1_OUTPUT" "MH12BK1001" "bike registration appears"
assert_contains "$SESSION1_OUTPUT" "MH12CR2002" "car registration is normalised to uppercase"
assert_contains "$SESSION1_OUTPUT" "already parked" "duplicate car is rejected"
assert_contains "$SESSION1_OUTPUT" "Invalid registration" "bad plate is rejected"
assert_contains "$SESSION1_OUTPUT" "Vehicle unparked successfully" "bike was unparked"
assert_contains "$SESSION2_OUTPUT" "Loaded saved data" "session 2 reloaded the data file"
assert_contains "$SESSION2_OUTPUT" "MH12CR2002" "car is still parked after restart"
assert_contains "$SESSION2_OUTPUT" "MH12TR3003" "truck is still parked after restart"
assert_contains "$SESSION2_OUTPUT" "Vehicle unparked successfully" "remaining vehicles were unparked"

# The bike should no longer be in the parked list after restart
if grep -F -q "MH12BK1001" "$SESSION2_OUTPUT"; then
  # It might appear only if still parked; after session 1 it should be gone from option 5.
  # Option 5 is printed first in session 2. Check just that block roughly:
  if awk '/View parked vehicles|Ticket/{}/Total parked/{exit} {print}' "$SESSION2_OUTPUT" | grep -F -q "MH12BK1001"; then
    echo "  FAIL  bike should not still be parked after restart"
    fail=1
  else
    echo "  PASS  bike was not still parked after restart"
  fi
else
  echo "  PASS  bike was not still parked after restart"
fi

rm -f "$SESSION1_OUTPUT" "$SESSION2_OUTPUT"

if [[ "$fail" -ne 0 ]]; then
  echo "Scripted session FAILED"
  exit 1
fi
echo "Scripted session PASSED"
