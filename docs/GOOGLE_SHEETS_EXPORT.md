# Daily Google Sheets Export

The backend automatically exports newly created records to Google Sheets every day at
**12:00 PM Asia/Kolkata time**.

```java
@Scheduled(cron = "0 0 12 * * *", zone = "Asia/Kolkata")
```

Scheduling is enabled globally via `@EnableScheduling` (see
`config/SchedulingConfig.java`). The job runs in `scheduler/DailyExportScheduler.java`
and delegates to `service/export/LeadExportService.java`.

---

## How it works

Every day at 12:00 PM:

```text
MySQL
   ↓
Find records where exported = false
   ↓
Send those records to Google Sheets
   ↓
Google Sheets APPENDS them as NEW ROWS
   ↓
Confirm Google Sheets API succeeded
   ↓
Update those records: exported = true
```

### The `exported` boolean

Every exportable record carries:

```text
exported BOOLEAN NOT NULL DEFAULT FALSE
```

- New records are created with `exported = false` (server-owned, never read from the
  client).
- The query `WHERE exported = false` selects exactly the rows still waiting to be sent.
- After Google Sheets reports success, the job flips `exported = true`.

Example:

```text
ID | Name  | exported
1  | Rahul | true
2  | Amit  | true
3  | Priya | false
4  | Ravi  | false
```

The next export selects only records 3 and 4. After a successful append:

```text
ID | Name  | exported
1  | Rahul | true
2  | Amit  | true
3  | Priya | true
4  | Ravi  | true
```

---

## Append-only guarantee

The Google Sheets operation **always appends** new rows underneath existing data; it
never overwrites rows during a normal export. The client uses the Sheets API
`values.append` request with:

- `ValueInputOption = USER_ENTERED`
- `InsertDataOption = INSERT_ROWS`

Example sheet before export:

```text
ID | Name
1  | Rahul
2  | Amit
```

New database records `3 → Priya` and `4 → Ravi`:

```text
ID | Name
1  | Rahul
2  | Amit
3  | Priya
4  | Ravi
```

Existing rows 1 and 2 remain unchanged.

---

## Critical ordering

The backend follows this order for every table:

```text
1. Query unexported records
2. Send them to Google Sheets
3. Wait for a successful Google Sheets API response
4. ONLY after success, update exported = true
```

It never does:

```text
1. Find records
2. exported = true
3. Send to Google Sheets
```

If Google Sheets fails, the records stay `exported = false` and are retried on the next
scheduled run. A failure on one table does **not** block the other tables.

---

## No new data

If there are no records with `exported = false`, the scheduler logs something like:

```text
No new records to export for 'Loan Application'.
```

and finishes without touching Google Sheets for that type.

---

## Multiple tables

The same principle applies to all four forms. Each table tracks its own export state:

```text
loan_applications   ── exported BOOLEAN NOT NULL DEFAULT FALSE
eligibility_checks  ── exported BOOLEAN NOT NULL DEFAULT FALSE
contact_messages    ── exported BOOLEAN NOT NULL DEFAULT FALSE
callback_requests   ── exported BOOLEAN NOT NULL DEFAULT FALSE
```
---

## Spreadsheet structure

One Google spreadsheet ("BTech Loan_Wala Leads") with one lead tab (`LoanApplications`)
that all four record types are appended to. Every row uses the same 14-column layout and
the **Type** column says which form the lead came from, so no data is lost regardless of
which form submitted it:

| Column | Loan Application | Eligibility Check | Contact Message | Callback Request |
|---|---|---|---|---|
| 1 `Timestamp` | created at | created at | created at | created at |
| 2 `Type` | `Loan Application` | `Eligibility Check` | `Contact Message` | `Callback Request` |
| 3 `Full Name` | full name | full name | full name | full name |
| 4 `Mobile` | mobile | mobile | mobile | mobile |
| 5 `Email` | email | email | email | *(empty)* |
| 6 `Loan Type` | loan type | loan type | *(empty)* | loan type |
| 7 `Loan Amount` | loan amount | loan amount | *(empty)* | *(empty)* |
| 8 `Employment Type` | employment type | employment type | *(empty)* | *(empty)* |
| 9 `Monthly Income` | monthly income | monthly income | *(empty)* | *(empty)* |
| 10 `City` | city | city | *(empty)* | *(empty)* |
| 11 `Subject` | *(empty)* | *(empty)* | subject | *(empty)* |
| 12 `Message` | message | *(empty)* | message | *(empty)* |
| 13 `Consent` | `true`/`false` | *(empty)* | *(empty)* | *(empty)* |
| 14 `Status` | status | status | status | status |

The exact header row the write order must match is:

```text
Timestamp | Type | Full Name | Mobile | Email | Loan Type | Loan Amount |
Employment Type | Monthly Income | City | Subject | Message | Consent | Status
```

Money columns are exported as plain decimal strings (e.g. `500000`) so `USER_ENTERED`
parses them as numbers in the sheet — never as `double`. Timestamps are formatted
`yyyy-MM-dd HH:mm:ss` so `USER_ENTERED` stores them as real date/time values.

---

## Reliability: Sheets is not part of submission

Google Sheets is **not** part of the customer submission transaction. The customer path
is unchanged:

```text
React → Spring Boot → MySQL → Success response
```

The export is a separate, scheduled concern:

```text
12:00 PM → Scheduled Job → MySQL → Google Sheets
```

If Google Sheets is unavailable, customer submissions still succeed in MySQL, and the
next scheduled export retries the `exported = false` records.

---

## Duplicate consideration

A simple boolean handles the normal flow: `false → exported successfully → true`.

Known edge case acknowledged for Version 1:

```text
Google Sheets append succeeds
        ↓
Application crashes
        ↓
Database update to true never happens
```

The same records could be appended again on the next run. For V1/local development the
boolean approach is used and this limitation is accepted — no distributed transactions
are introduced. If needed later, an export-log / idempotency table (e.g.
`google_sheet_exports` with `entity_type`, `entity_id`, `exported_at`) replaces the
boolean.

---

## Manual testing

A development-only endpoint triggers the **exact same** export service used by the
scheduler, so you can test without waiting for 12 PM:

```http
POST /api/export
```

```text
POST /api/export
       ↓
LeadExportService
       ↓
Google Sheets
```

and

```text
12 PM Scheduler
       ↓
LeadExportService
       ↓
Google Sheets
```

The export logic lives in exactly one place: `LeadExportService.exportAll()`.

Response:

```json
{ "ok": true, "message": "Google Sheets export complete. Exported: Loan Application=2, Eligibility Check=1. No new records to export." }
```

---

## Final flow

```text
                 CUSTOMER
                    ↓
                 React
                    ↓
             POST /api/form
                    ↓
              Spring Boot
                    ↓
                  MySQL
                    ↓
              exported=false
                    │
                    │
                    │ 12:00 PM
                    ↓
           Scheduled Export Job
                    ↓
        Find exported = false
                    ↓
            Google Sheets API
                    ↓
              APPEND NEW ROWS
                    ↓
             API success?
               /        \
             YES         NO
              ↓           ↓
      exported=true   exported=false
              ↓           ↓
           Done        Retry later
```

The database remains the **source of truth**; Google Sheets is only the daily reporting
destination.

---

## Configuration

| Property | Env var | Default | Purpose |
|---|---|---|---|
| `google.sheets.credentials.file` | `GOOGLE_SHEETS_CREDENTIALS` | `./credentials/service-account.json` | Service-account JSON key used to authorise the Sheets API |
| `google.sheets.spreadsheet.id` | `GOOGLE_SHEETS_SPREADSHEET_ID` | `1KwAI9b8Kvgx_t2V7Tpc4PZJjxkhnGZTZ84a0MMTEsdQ` | ID of the "BTech Loan_Wala Leads" spreadsheet |
| `google.sheets.export.tab` | `GOOGLE_SHEETS_TAB` | `LoanApplications` | Tab that receives every exported lead (single unified 14-column layout) |

The service account must be granted **Editor** access to the spreadsheet and the scope
`https://www.googleapis.com/auth/spreadsheets`.