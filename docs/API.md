# API Reference

Base URL: `http://localhost:8080`

All endpoints require `Content-Type: application/json`. All endpoints are **public**
(no authentication). Every response uses the shared envelope:

```json
{ "ok": true, "message": "..." }
```

---

## HTTP status conventions

| Status | Meaning |
|---|---|
| `201 Created` | Record persisted successfully (`"ok": true`) |
| `400 Bad Request` | Bean Validation failed — bad format or missing field, false consent, or malformed JSON body |
| `422 Unprocessable Entity` | Well-formed JSON but an invalid business value (unknown loan type / employment type / non-numeric money) |
| `500 Internal Server Error` | Unexpected server/database error — generic message, no stack traces or internals exposed |

---

## Enumerated values

### Loan types
```
personal | home | business | lap | car | home-bt | education | project-funding
```

### Employment types
```
salaried | self-employed | business
```

### Lead status (server-managed, not submitted)
```
NEW | CONTACTED | CLOSED
```

---

# 1. POST /api/apply-now

Submit a loan application.

### Request body

```json
{
  "fullName": "Amit Sharma",
  "mobile": "7276063476",
  "email": "amit@mail.com",
  "loanType": "personal",
  "amount": "500000",
  "employment": "salaried",
  "income": "90000",
  "city": "Pune",
  "message": "Need personal loan",
  "consent": true
}
```

### Field rules

| Field | Required | Validation |
|---|---|---|
| `fullName` | ✅ | non-blank, ≤ 120 chars |
| `mobile` | ✅ | exactly 10 digits (`^[0-9]{10}$`) |
| `email` | ❌ | valid email if provided, ≤ 120 chars |
| `loanType` | ✅ | one of the loan types above |
| `amount` | ✅ | numeric string, non-negative |
| `employment` | ✅ | one of the employment types above |
| `income` | ✅ | numeric string, non-negative |
| `city` | ✅ | non-blank, ≤ 100 chars |
| `message` | ❌ | free text |
| `consent` | ✅ | must be `true` |

### Success — 201

```json
{
  "ok": true,
  "message": "Application submitted successfully."
}
```

### Error examples

Invalid mobile — **400**:
```json
{ "ok": false, "message": "Mobile number must contain 10 digits." }
```

Invalid loan type — **422**:
```json
{ "ok": false, "message": "Invalid loan type." }
```

Invalid employment type — **422**:
```json
{ "ok": false, "message": "Invalid employment type." }
```

Consent `false` — **400** (no row is persisted):
```json
{ "ok": false, "message": "Consent is required to submit the application." }
```

Non-numeric amount — **422**:
```json
{ "ok": false, "message": "Invalid loan amount." }
```

---

# 2. POST /api/eligibility

Submit an eligibility check. There is **no consent** field on this form.

### Request body

```json
{
  "fullName": "Riya Verma",
  "mobile": "8989898989",
  "email": "riya@mail.com",
  "loanType": "home-bt",
  "employment": "self-employed",
  "income": "120000",
  "amount": "2500000",
  "city": "Mumbai"
}
```

### Field rules

| Field | Required | Validation |
|---|---|---|
| `fullName` | ✅ | non-blank, ≤ 120 chars |
| `mobile` | ✅ | exactly 10 digits |
| `email` | ❌ | valid email if provided |
| `loanType` | ✅ | one of the loan types above |
| `employment` | ✅ | one of the employment types above |
| `income` | ✅ | numeric string, non-negative |
| `amount` | ✅ | numeric string, non-negative |
| `city` | ✅ | non-blank, ≤ 100 chars |

### Success — 201

```json
{
  "ok": true,
  "message": "Eligibility check submitted successfully."
}
```

---

# 3. POST /api/contact

Submit a contact message.

### Request body

```json
{
  "fullName": "Priya Patel",
  "mobile": "9876543210",
  "email": "priya@mail.com",
  "subject": "Query about home loan",
  "message": "Please share latest rates."
}
```

### Field rules

| Field | Required | Validation |
|---|---|---|
| `fullName` | ✅ | non-blank, ≤ 120 chars |
| `mobile` | ✅ | exactly 10 digits |
| `email` | ❌ | valid email if provided, ≤ 120 chars |
| `subject` | ❌ | ≤ 255 chars |
| `message` | ✅ | non-blank, ≤ 5000 chars |

### Success — 201

```json
{
  "ok": true,
  "message": "Message received successfully."
}
```

---

# 4. POST /api/callback

Request a callback.

### Request body

```json
{
  "fullName": "Kabir Khan",
  "mobile": "9988776655",
  "loanType": "business"
}
```

### Field rules

| Field | Required | Validation |
|---|---|---|
| `fullName` | ✅ | non-blank, ≤ 120 chars |
| `mobile` | ✅ | exactly 10 digits |
| `loanType` | ✅ | one of the loan types above |

### Success — 201

```json
{
  "ok": true,
  "message": "Callback requested successfully."
}
```

---

# 5. POST /api/export (development only)

Manually trigger the daily Google Sheets export job. It executes the **exact same**
`LeadExportService` used by the 12:00 PM Asia/Kolkata scheduler — no duplicated logic.
Use it to test exports without waiting for the scheduled run.

No request body required.

### Success — 200

```json
{
  "ok": true,
  "message": "Google Sheets export complete. Exported: Loan Application=2, Eligibility Check=1. No new records to export."
}
```

The message reports how many records were exported per lead type and which types failed
(failed records stay `exported = false` and are retried next run). See
[`GOOGLE_SHEETS_EXPORT.md`](GOOGLE_SHEETS_EXPORT.md) for the full spec.

---

## Generic error handling

Any unexpected server/database failure returns a safe generic message — internal class
names, SQL/Hibernate output, and credentials are never exposed.

```json
{
  "ok": false,
  "message": "Unable to process your request. Please try again."
}
```

A malformed or unreadable JSON body returns:

```json
{
  "ok": false,
  "message": "Invalid request payload."
}
```

---

## Testing with curl

```bash
curl -X POST http://localhost:8080/api/apply-now \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Amit Sharma","mobile":"7276063476","loanType":"personal","amount":"500000","employment":"salaried","income":"90000","city":"Pune","consent":true}'
```

Reusable sample payloads (valid and invalid) are available in the `docs/` folder
(`t*.json`, `e*.json`, `c*.json`, `k*.json`).
