# Architecture

## Overview

The backend is a layered Spring Boot application. The React frontend submits public
form data over HTTP; the backend validates, transforms, and persists it into MySQL. A
scheduled job exports new records to Google Sheets for reporting — it is a **separate**
concern and is never part of the customer submission path.

```
                     React Frontend
                          |
                          | HTTP POST (application/json)
                          ↓
                 Spring Boot REST API  (port 8080)
                          |
             ┌────────────┼────────────┐
             ↓            ↓            ↓
          Controller    Service      Validation
                          |
                          ↓
                    JPA Repository
                          |
                          ↓
                       MySQL
```

Daily reporting pipeline (separate from customer submissions):

```
MySQL --> 12:00 PM Scheduled Job --> Google Sheets API --> Google Sheet (single 'LoanApplications' tab)
```

**MySQL is the source of truth.** Google Sheets is only a reporting/export destination.
A customer submission must succeed even if Google Sheets is unavailable.


---

## Layers and responsibilities

| Layer | Responsibility | Example classes |
|---|---|---|
| **Controller** | Thin HTTP layer: binds the JSON body, triggers `@Valid`, returns `ResponseEntity<ApiResponse>`. No business logic. | `LoanApplicationController` |
| **Service** | Business rules: DTO → entity mapping, enum resolution, String → BigDecimal money conversion, saving via repository. | `LoanApplicationService` |
| **Repository** | Spring Data JPA interface. Database access only. | `LoanApplicationRepository` |
| **Entity** | JPA persistence model matching the database contract. | `LoanApplication` |
| **Request DTO** | The contract the frontend submits. Differently shaped from the entity on purpose. | `LoanApplicationRequest` |
| **Exception handler** | Converts every failure into the same `{ok, message}` envelope with no internals leaked. | `GlobalExceptionHandler` |
| **Enums / config** | Domain constants and cross-cutting configuration (CORS). | `LoanType`, `CorsConfig` |

### Vertical-slice flow (per form)

```
HTTP POST
   ↓
@Valid on the Request DTO     — rejects format errors (HTTP 400)
   ↓
Service: build entity          — resolve enums, convert String money to BigDecimal
   ↓
Service: business validation   — unknown loan type / employment type → HTTP 422
   ↓
Repository.save()             — persist (HTTP 201)
```

---

## Key design decisions

### 1. DTOs are not entities
The frontend sends `"amount": "500000"` (a String); the database stores
`loan_amount DECIMAL(14,2)`. These are different responsibilities:

- **Request DTO** = frontend contract
- **Entity** = database contract

The service deliberately converts `String → BigDecimal` (using `new BigDecimal()` with
validation) rather than casting. Money is never stored as `double`.

### 2. Server-side enum resolution
The frontend sends lowercase, hyphenated values such as `"home-bt"` and
`"self-employed"`. Java enum naming is `HOME_BT` / `SELF_EMPLOYED`, which Jackson cannot
map automatically. Each enum therefore carries its wire `value` and exposes a static
`fromValue(...)` lookup. Request DTO fields stay `String`; only the DB stores canonical
values.

### 3. Server-owned fields
- `status` defaults to `LeadStatus.NEW` on every entity.
- `createdAt` is stamped by an entity `@PrePersist` hook using `LocalDateTime.now()`.
- Neither field can be set from the client, preventing timestamp/status tampering.
- `status` is persisted with `@Enumerated(EnumType.STRING)` — never as an ordinal number.

### 4. Consistent HTTP semantics
| Condition | Status |
|---|---|
| Bean Validation failure (bad mobile, false consent, missing field) | 400 |
| Invalid business value (unknown loan type, bad money) | 422 |
| Unexpected server/database error | 500 (generic message) |

### 5. Global, leak-free error handling
`GlobalExceptionHandler` (`@RestControllerAdvice`) maps:
- `MethodArgumentNotValidException` → 400 with the first field-constraint message
- `HttpMessageNotReadableException` → 400 `Invalid request payload.`
- `IllegalArgumentException` → 422 with its message (e.g. `Invalid loan type.`)
- `Exception` → 500 with a generic message

Stack traces, SQL/Hibernate output, and database credentials never reach the frontend.

### 6. CORS scoped to the dev origin
`CorsConfig` allows only `http://localhost:5173` (the React dev server). It does **not**
use `allowedOrigins("*")`. Swap in the production domain when deployed.

### 7. Google Sheets export is a decoupled, retry-safe reporting job
A scheduled job (12:00 PM Asia/Kolkata) exports unexported rows to Google Sheets —
**never** on the customer submission path. Each entity has an `exported` boolean; the
job only flips it to `true` after the Sheets append succeeds, so failures are retried
next run. See [`docs/GOOGLE_SHEETS_EXPORT.md`](GOOGLE_SHEETS_EXPORT.md) for the full spec.

---

## Package structure

```
com.btechloanwala.LeadGeneration
├── config            CorsConfig, SchedulingConfig (@EnableScheduling)
├── controller        LoanApplicationController, EligibilityController, ContactController,
│                     CallbackController, ExportController (dev manual trigger)
├── dto/request       LoanApplicationRequest, EligibilityRequest, ContactRequest, CallbackRequestDTO
├── dto/response      ApiResponse
├── entity            LoanApplication, EligibilityCheck, ContactMessage, CallbackRequest
├── enums             LoanType, EmploymentType, LeadStatus
├── exception         GlobalExceptionHandler
├── repository        *Repository (one per entity)
├── scheduler         DailyExportScheduler (@Scheduled 12:00 PM Asia/Kolkata)
└── service
    ├── *Service + dto→entity logic (one per form)
    └── export        LeadExportService, GoogleSheetsClient, ExportSummary
```

Each form follows the same naming pattern: `Entity`, `XxxRepository`, `XxxRequest`,
`XxxService`, `XxxController`.

---

## Database schema

| Table | Created by | Notable columns |
|---|---|---|
| `loan_applications` | `LoanApplication` | `loan_amount DECIMAL(14,2)`, `monthly_income DECIMAL(14,2)`, `consent BIT`, `status enum`, `created_at DATETIME(6)`, `exported BOOLEAN NOT NULL DEFAULT FALSE` |
| `eligibility_checks` | `EligibilityCheck` | same conventions, no consent, `exported BOOLEAN NOT NULL DEFAULT FALSE` |
| `contact_messages` | `ContactMessage` | `message TEXT`, `subject VARCHAR(255)`, `exported BOOLEAN NOT NULL DEFAULT FALSE` |
| `callback_requests` | `CallbackRequest` | contact details + `loan_type`, `exported BOOLEAN NOT NULL DEFAULT FALSE` |

> `ddl-auto=update` (in `application.properties`) is acceptable for local development
> only. Production should use proper migrations such as **Flyway**. The `exported`
> column is added with `DEFAULT FALSE` so existing rows are back-filled without manual
> DDL.

---

## Security boundaries

- The four public `POST` form endpoints accept submissions with no login.
- `POST /api/export` is a **development-only** manual trigger for the export job.
  It is currently unauthenticated for convenience; gate it behind auth / expose only in
  a dev profile before deploying to a public environment.
- There are **no** public endpoints such as `GET /api/leads`, `GET /api/applications`,
  or `GET /api/customers`. Employee-facing data access requires a separate access-control
  decision and is intentionally not implemented in Version 1.
- Spring Security / JWT are **not** added yet.

---

## Google Sheets export

Implemented as a decoupled, retry-safe reporting job. See
[`docs/GOOGLE_SHEETS_EXPORT.md`](GOOGLE_SHEETS_EXPORT.md) for the full specification,
row layouts, and the append/ordering guarantees.

```
Google Spreadsheet ("BTech Loan_Wala Leads")
│
└── LoanApplications   ← every exported lead, tagged by Type column
     (Loan Application | Eligibility Check | Contact Message | Callback Request)
```

All four tables map into the same `LoanApplications` tab (one 14-column layout). `exported = false`
rows are appended below existing data every day at 12:00 PM Asia/Kolkata, and only flipped to
`true` after the Sheets append
succeeds. Google Sheets is **never** part of the customer submission transaction.

Known limitation (Version 1): a crash between a successful append and the
`exported = true` save can re-append rows on the next run. A future
`google_sheet_exports` export-log / idempotency table addresses this; distributed
transactions are intentionally out of scope for V1.

