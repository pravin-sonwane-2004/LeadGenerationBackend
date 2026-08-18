# Deployment Guide

Production deployment steps for the **BTech Loan_Wala** lead-generation backend
(`com.btechloanwala:LeadGeneration:1.0.0`).

---

## 1. Requirements

- **JDK 26** (or a 17+ compatible toolchain)
- **Maven 3.6+** (only needed to build — not on the runtime host)
- **MySQL 8** with a dedicated, least-privilege database user
- **Google service-account key** with Editor access to the export spreadsheet (optional
  if you do not use the Google Sheets export)

---

## 2. Build

```bash
mvn clean package
```

Artifact: **`target/lead-generation.jar`** — a self-contained executable Spring Boot jar.

Run the test suite before shipping:

```bash
mvn test
```

---

## 3. Runtime configuration

All secrets and environment-specific values come from environment variables. Never
commit credentials.

| Env var | Default (dev) | Required in prod | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | *(none)* | **yes** — `prod` | Activates production profile |
| `DB_URL` | `jdbc:mysql://localhost:3306/lead_generation?...` | **yes** | JDBC URL (use TLS in prod) |
| `DB_USERNAME` | `root` | **yes** | MySQL user |
| `DB_PASSWORD` | `0000` | **yes** | MySQL password |
| `DB_DDL_AUTO` | `update` (dev) / `validate` (prod) | no | Hibernate schema strategy |
| `SERVER_PORT` | `8080` | no | HTTP port |
| `DB_POOL_MAX` / `DB_POOL_MIN` | 10 / 2 | no | Hikari pool sizing |
| `GOOGLE_SHEETS_CREDENTIALS` | `./credentials/service-account.json` | if using export | Service-account JSON key path |
| `GOOGLE_SHEETS_SPREADSHEET_ID` | *(local id)* | if using export | Target spreadsheet id |
| `GOOGLE_SHEETS_TAB` | `LoanApplications` | if using export | Tab that receives all exported leads (unified 14-column layout) |
| `JPA_SHOW_SQL` | `false` | no | Debug SQL logging |

### Run

```bash
java -jar target/lead-generation.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url="$DB_URL" \
  --spring.datasource.username="$DB_USERNAME" \
  --spring.datasource.password="$DB_PASSWORD"
```

(Prefer exporting `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` as real environment variables
over passing them on the command line, so they do not show up in `ps` output.)

---

## 4. Database schema management

- **Development:** `spring.jpa.hibernate.ddl-auto=update` lets Hibernate create the
  schema automatically.
- **Production (`prod` profile):** `ddl-auto` defaults to `validate`, so the app fails
  fast if entities drift from the schema. Hibernate never alters tables in production.

Recommended production practice: manage the schema with **Flyway** (or your chosen
migration tool) and keep `ddl-auto=validate`. The four tables are
`loan_applications`, `eligibility_checks`, `contact_messages`, `callback_requests` —
each with an `exported BOOLEAN NOT NULL DEFAULT FALSE` column. See
[`docs/ARCHITECTURE.md`](ARCHITECTURE.md) for the exact columns.

---

## 5. Google Sheets export (optional)

1. Create a service account in Google Cloud and download its JSON key.
2. Share the target spreadsheet with the service account email (**Editor**).
3. Enable the Google Sheets API for the project.
4. Set `GOOGLE_SHEETS_CREDENTIALS` to the key path and `GOOGLE_SHEETS_SPREADSHEET_ID` to
   the spreadsheet id. Set `GOOGLE_SHEETS_TAB` only if the receiving tab is not named
   `LoanApplications`.
5. The scheduler runs at **12:00 PM Asia/Kolkata**; test it with `POST /api/export`.

See [`docs/GOOGLE_SHEETS_EXPORT.md`](GOOGLE_SHEETS_EXPORT.md) for the full export spec.
---

## 6. Production hardening checklist

- [ ] **CORS** — `CorsConfig` currently allows only `http://localhost:5173`. Replace
      with the real frontend origin(s) before going live.
- [ ] **`POST /api/export`** is a development-only trigger and is unauthenticated. Do
      **not** expose it on a public network as-is — gate it behind auth or restrict the
      route.
- [ ] **TLS/HTTPS** — terminate TLS at the load balancer/proxy; do not run plain HTTP in
      front of the internet.
- [ ] **Secrets** — MySQL password and the Google service-account key must come from a
      secret manager / environment, never from source code.
- [ ] **Backups** — MySQL is the source of truth. Google Sheets is only a reporting copy
      and must never be treated as durable storage.
- [ ] **Logs** — SQL debug logging is off in prod; review application logs periodically
      for rejected submissions.
- [ ] *(Optional)* Add `spring-boot-starter-actuator` and expose only `health` and `info`
      for load-balancer health checks.
- [ ] *(Optional)* Add Flyway migrations so schema changes are versioned.

---

## 7. Health check / smoke test

After startup, confirm the endpoints respond (no health actuator by default — use a
simple submission or the login-less form endpoints):

```bash
curl -X POST http://localhost:8080/api/apply-now \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Smoke Test","mobile":"7276063476","loanType":"personal","amount":"10000","employment":"salaried","income":"50000","city":"Pune","consent":true}'
```

Expect `201 Created` with `{"ok": true, ...}`. If the Google Sheets export is enabled:

```bash
curl -X POST http://localhost:8080/api/export
```

Expect `200 OK` and a summary message.

