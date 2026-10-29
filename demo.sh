#!/usr/bin/env bash
# End-to-end demo against a running API. Needs: curl, jq
BASE=${BASE:-http://localhost:8080/api}; S=$RANDOM
api(){ local a=(-s -X "$1" "$BASE$2" -H 'Content-Type: application/json'); [ -n "$3" ] && a+=(-H "Authorization: Bearer $3"); [ -n "$5" ] && a+=(-H "Idempotency-Key: $5"); [ -n "$4" ] && a+=(-d "$4"); curl "${a[@]}"; echo; }
reg(){ api POST /auth/register "" "{\"name\":\"$1\",\"email\":\"$1$S@demo.com\",\"password\":\"secret123\"}"; }
RA=$(reg alice); RB=$(reg bob); RC=$(reg carol)
TA=$(jq -r .token <<<"$RA"); TB=$(jq -r .token <<<"$RB"); IA=$(jq -r .userId <<<"$RA"); IB=$(jq -r .userId <<<"$RB")
G=$(api POST /groups "$TA" "{\"name\":\"Goa Trip\",\"memberEmails\":[\"bob$S@demo.com\",\"carol$S@demo.com\"]}" | jq -r .id)
echo "== Alice pays 90 (equal split)";  api POST /groups/$G/expenses "$TA" '{"description":"Hotel","amount":90}'
echo "== Bob pays 30 (equal split)";    api POST /groups/$G/expenses "$TB" '{"description":"Taxi","amount":30}'
echo "== Balances (+ is owed, - owes)"; api GET /groups/$G/balances "$TA"
echo "== Minimal settle-up plan";       api GET /groups/$G/settle-up "$TA"
echo "== Bob tops up wallet 50";        api POST /wallet/deposit "$TB" '{"amount":50}'
K="key-$RANDOM-$RANDOM"
echo "== Bob pays Alice 10 twice with SAME Idempotency-Key (charged once)"
for i in 1 2; do api POST /groups/$G/settlements "$TB" "{\"toUser\":$IA,\"amount\":10}" "$K"; done
echo "== Bob wallet (expect 40.00)";    api GET /wallet "$TB"
echo "== Remaining plan";               api GET /groups/$G/settle-up "$TA"
