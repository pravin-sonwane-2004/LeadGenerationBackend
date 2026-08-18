# BTech Loan_Wala — Backend API

Spring Boot backend for the BTech Loan_Wala React application. It receives public
lead-capture form submissions, validates incoming JSON, persists the data into MySQL,
and returns a consistent response envelope to the frontend.

Built with **Java 17+**, **Spring Boot 4.1**, **Spring Web (MVC)**, **Spring Data JPA /
Hibernate**, **MySQL**, and **Bean Validation**.

---

## Quick start

### Prerequisites
- **JDK 17+** (project targets Java 26, but any 17+ compatible toolchain works)
- **Maven 3.6+**
- **MySQL 8** running locally on `localhost:3306`

### 1. Create the database (optional)
The datasource uses `createDatabaseIfNotExist=true`, so the schema is created
automatically on first start. If you prefer to create it manually:

```sql
CREATE DATABASE IF NOT EXISTS lead_generation;
```

### 2. Configure credentials
Credentials are read from environment variables with sensible local defaults, so no
secrets live in source code.

| Env var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/lead_generation?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC` | JDBC connection string |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `0000` | MySQL password |
| `DB_DDL_AUTO` | `update` | Hibernate schema strategy (`validate` in prod) |
| `SERVER_PORT` | `8080` | HTTP port |

Windows PowerShell:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
```

### 3. Run

```bash
# From the project root
mvn spring-boot:run
```

or build and run the executable jar:

```bash
mvn -DskipTests package
java -jar target\lead-generation.jar
```

The application starts on **`http://localhost:8080`** and connects to MySQL. Hibernate
creates the four tables automatically in development only (`ddl-auto=update`) — never
rely on that in production.

> **Production?** Activate the `prod` profile (`--spring.profiles.active=prod`) and follow
> [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

---

## Public API endpoints

The four form endpoints accept `application/json` and are **public** — customers must be
able to submit forms without logging in. `POST /api/export` is a development-only manual
trigger for the Google Sheets export job.

| Method | URL | Purpose |
|---|---|---|
| `POST` | `/api/apply-now` | Submit a loan application |
| `POST` | `/api/eligibility` | Submit an eligibility check |
| `POST` | `/api/contact` | Submit a contact message |
| `POST` | `/api/callback` | Request a callback |
| `POST` | `/api/export` | *(dev)* Run the Google Sheets export now |

Full request/response examples, validation rules, and error handling are documented in
[`docs/API.md`](docs/API.md).

---

## Response contract

Every endpoint returns the same envelope:

```json
{
  "ok": true,
  "message": "Application submitted successfully."
}
```

- **201 Created** — record persisted successfully, `"ok": true`
- **400 Bad Request** — Bean Validation failed (bad mobile, false consent, malformed body)
- **422 Unprocessable Entity** — well-formed request but an invalid business value
  (unknown loan type, invalid employment type, non-numeric money)
- **500 Internal Server Error** — unexpected error (generic message, no internals leaked)

---

## Project layout

```
src/main/java/com/btechloanwala/LeadGeneration/
├── LeadGenerationApplication.java   Entry point
├── config/                          CorsConfig (dev CORS), SchedulingConfig (@EnableScheduling)
├── controller/                      REST controllers (thin HTTP layer) + ExportController (dev trigger)
├── dto/
│   ├── request/                     Request payloads (frontend contract)
│   └── response/ApiResponse.java    Shared {ok, message} envelope
├── entity/                          JPA entities (database contract)
├── enums/                           LoanType, EmploymentType, LeadStatus
├── exception/GlobalExceptionHandler Central error handling (no stack traces leak)
├── repository/                      Spring Data repositories (DB access)
├── scheduler/DailyExportScheduler   @Scheduled 12:00 PM Asia/Kolkata export trigger
└── service/
    ├── *Service.java                Business logic, DTO→entity mapping, money conversion
    └── export/                      LeadExportService, GoogleSheetsClient, ExportSummary
```

Each form is a complete vertical slice:

```
Controller → Service → Repository → MySQL
     ↓
  [@Valid DTO]  →  [business validation]  →  [persist]
```

---

## Database

Hibernate maps each entity to a table (`ddl-auto=update` for local development — do **not**
rely on it in production).

| Table | Entity | Notes |
|---|---|---|
| `loan_applications` | `LoanApplication` | money as `DECIMAL(14,2)`, requires consent |
| `eligibility_checks` | `EligibilityCheck` | no consent field |
| `contact_messages` | `ContactMessage` | subject optional, message required |
| `callback_requests` | `CallbackRequest` | contact details + loan type only |

Every table also has an `exported BOOLEAN NOT NULL DEFAULT FALSE` column used by the
daily Google Sheets export job (see below).

Common conventions:
- `status` defaults to **`NEW`** (`LeadStatus` stored as a String, never an ordinal).
- `created_at` is generated by the server via `@PrePersist` — never accepted from the client.
- `exported` defaults to **`false`** — server-owned, set to `true` only by the export job.

---

## Interesting design decisions

- **DTOs are not entities.** The frontend sends `"amount": "500000"` (String); the DB
  stores `loan_amount DECIMAL(14,2)`. The service layer converts `String → BigDecimal`
  and validates numeric input — never blindly cast.
- **Server-side enum resolution.** The frontend sends `"home-bt"` and `"self-employed"`,
  which do not match Java enum naming (`HOME_BT`, `SELF_EMPLOYED`). Request fields stay
  Strings and are resolved via `LoanType.fromValue()` / `EmploymentType.fromValue()`.
- **Spring Boot 4.1 starter name.** Use `spring-boot-starter-webmvc` — Boot 4 split the
  legacy `spring-boot-starter-web` into `webmvc` / `webflux`.

---

## Daily Google Sheets export

A scheduled job runs every day at **12:00 PM Asia/Kolkata** and appends new records to a
single tab (`LoanApplications`) of the "BTech Loan_Wala Leads" spreadsheet. All four
record types share the same 14-column layout — `Timestamp`, `Type`, `Full Name`,
`Mobile`, `Email`, `Loan Type`, `Loan Amount`, `Employment Type`, `Monthly Income`,
`City`, `Subject`, `Message`, `Consent`, `Status` — and the `Type` column identifies the
form a row came from. New records start `exported = false`; the job only flips them to
`true` **after** the Sheets append succeeds, so failures are retried the next day.
Google Sheets is never part of the customer submission path.

To trigger it manually for testing, call the dev endpoint:

```
POST /api/export
```

Required configuration (env-var overridable, with sensible defaults — no secrets in
source code):

| Env var | Default | Purpose |
|---|---|---|
| `GOOGLE_SHEETS_CREDENTIALS` | `./credentials/service-account.json` | Service-account JSON key |
| `GOOGLE_SHEETS_SPREADSHEET_ID` | `1KwAI9b8Kvgx_t2V7Tpc4PZJjxkhnGZTZ84a0MMTEsdQ` | Target spreadsheet id |
| `GOOGLE_SHEETS_TAB` | `LoanApplications` | Tab receiving all exported leads (unified 14-column layout) |

Full specification (ordering, append-only behavior, row layouts, limitations): see
[`docs/GOOGLE_SHEETS_EXPORT.md`](docs/GOOGLE_SHEETS_EXPORT.md).

---

## Roadmap (later phases)

The following are intentionally **not** part of this initial version:

- **Internal lead retrieval APIs** (`GET /leads`, etc.) and access control — deliberately
  not exposed publicly until the employee-access model is decided.
- A more robust export-log / idempotency table (`google_sheet_exports`) to replace the
  simple boolean, eliminating the theoretical duplicate-append edge case.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details.
#   L e a d G e n e r a t i o n B a c k e n d  
 