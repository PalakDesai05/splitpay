# SplitPay — Group Expense Splitter with Wallet Settlement

Java 21 · Spring Boot 3 · MySQL 8 · JWT · Docker

## Run
```bash
docker compose up --build        # API on http://localhost:8080, MySQL on the compose network
./demo.sh                        # optional end-to-end walkthrough (needs curl + jq)
```
Without Docker: start MySQL, then `mvn spring-boot:run` (env: `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET` ≥ 32 chars). Tests: `mvn test`.

## API  (all except `/api/auth/*` need `Authorization: Bearer <token>`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` `{name,email,password}` | create user + wallet, returns JWT |
| POST | `/api/auth/login` `{email,password}` | returns JWT |
| GET / POST | `/api/wallet`, `/api/wallet/deposit` `{amount}` | balance / top-up (simulated) |
| POST / GET | `/api/groups` `{name,memberEmails[]}` · `/api/groups` · `/api/groups/{id}` | create / list mine / details |
| POST | `/api/groups/{id}/members` `{email}` | add member |
| POST / GET | `/api/groups/{id}/expenses` | add / list. Body: `{description,amount,paidBy?,participants?[],exactShares?{userId:amt}}` — equal split by default, leftover cents distributed |
| GET | `/api/groups/{id}/balances` | net per member (+ is owed, − owes) |
| GET | `/api/groups/{id}/settle-up` | minimal payments to clear the group |
| POST / GET | `/api/groups/{id}/settlements` | pay from wallet (header `Idempotency-Key` required) / history. Body `{toUser,amount}` |

## Design notes
- **Minimal settlements** — `DebtSimplifier`: two max-heaps (creditors, debtors) in integer cents; repeatedly match the largest creditor with the largest debtor. ≤ n−1 transfers, O(n log n).
- **Wallet transfers** — one DB transaction; both wallet rows locked `FOR UPDATE` in id order (no deadlocks); insufficient funds → 422 and full rollback.
- **Idempotency** — `(from_user, idempotency_key)` is unique. The settlement row is inserted *first* (claims the key) inside the same transaction as the money movement, under a per-group row lock. Retrying with the same key returns the original result without paying again; reusing a key for a different request → 422.
- **Overpayment guard** — a payment can't exceed what the payer owes or what the payee is owed.
- **Money** — `BigDecimal` (scale 2) + `DECIMAL(12,2)`; never floating point.
