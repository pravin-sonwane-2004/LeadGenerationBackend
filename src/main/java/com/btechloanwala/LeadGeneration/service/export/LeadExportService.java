package com.btechloanwala.LeadGeneration.service.export;

import com.btechloanwala.LeadGeneration.entity.CallbackRequest;
import com.btechloanwala.LeadGeneration.entity.ContactMessage;
import com.btechloanwala.LeadGeneration.entity.EligibilityCheck;
import com.btechloanwala.LeadGeneration.entity.LoanApplication;
import com.btechloanwala.LeadGeneration.repository.CallbackRequestRepository;
import com.btechloanwala.LeadGeneration.repository.ContactMessageRepository;
import com.btechloanwala.LeadGeneration.repository.EligibilityCheckRepository;
import com.btechloanwala.LeadGeneration.repository.LoanApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Orchestrates the daily Google Sheets export.
 *
 * <p>This single service is used BOTH by the {@code 12:00 PM} scheduler and the manual
 * dev endpoint {@code POST /api/export} — there is no duplicated export logic anywhere
 * else in the application.</p>
 *
 * <h3>Guaranteed ordering per table</h3>
 * <ol>
 *   <li>Query records where {@code exported = false}.</li>
 *   <li>Append them to Google Sheets (rows are only ever appended below existing data).</li>
 *   <li>Wait for a successful Google Sheets API response.</li>
 *   <li>ONLY after success: flip {@code exported = true} and save.</li>
 * </ol>
 *
 * <p>If Google Sheets fails for a table, its records keep {@code exported = false} and
 * are picked up by the next scheduled run. The customer submission path never touches
 * Google Sheets, so Google being down never blocks a lead from reaching MySQL.</p>
 *
 * <p>Known limitation (Version 1): a crash between a successful append and the
 * {@code exported = true} save can re-append the same records on the next run. A future
 * export-log/idempotency mechanism replaces the plain boolean. This is intentionally not
 * addressed with distributed transactions in V1.</p>
 */
@Service
public class LeadExportService {

    private static final Logger log = LoggerFactory.getLogger(LeadExportService.class);

    /** Labels written into the {@code Type} column so every row stays traceable. */
    private static final String TYPE_LOAN_APPLICATION = "Loan Application";
    private static final String TYPE_ELIGIBILITY_CHECK = "Eligibility Check";
    private static final String TYPE_CONTACT_MESSAGE = "Contact Message";
    private static final String TYPE_CALLBACK_REQUEST = "Callback Request";

    /**
     * Every record type is appended to ONE tab with the same 14-column layout:
     * {@code Timestamp, Type, Full Name, Mobile, Email, Loan Type, Loan Amount,
     * Employment Type, Monthly Income, City, Subject, Message, Consent, Status}.
     * A record that does not collect a column (e.g. Contact Messages have no loan
     * amount) writes an empty cell. Timestamps are formatted {@code yyyy-MM-dd HH:mm:ss}
     * so Google Sheets' {@code USER_ENTERED} parses them as real date/time values.
     */
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoanApplicationRepository loanApplicationRepository;
    private final EligibilityCheckRepository eligibilityCheckRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final CallbackRequestRepository callbackRequestRepository;
    private final GoogleSheetsClient sheetsClient;
    private final String exportTab;

    public LeadExportService(
            LoanApplicationRepository loanApplicationRepository,
            EligibilityCheckRepository eligibilityCheckRepository,
            ContactMessageRepository contactMessageRepository,
            CallbackRequestRepository callbackRequestRepository,
            GoogleSheetsClient sheetsClient,
            @Value("${google.sheets.export.tab}") String exportTab) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.eligibilityCheckRepository = eligibilityCheckRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.callbackRequestRepository = callbackRequestRepository;
        this.sheetsClient = sheetsClient;
        this.exportTab = exportTab;
    }

    /**
     * Exports every unexported record from all four tables into the single unified
     * tab, tagging each row with its source type in the {@code Type} column.
     *
     * @return a summary of how many records were exported (and failed) per lead type
     */
    public ExportSummary exportAll() {
        ExportSummary summary = new ExportSummary();

        exportLoanApplications(summary);
        exportEligibilityChecks(summary);
        exportContactMessages(summary);
        exportCallbackRequests(summary);

        log.info("{}", summary);
        return summary;
    }

    // ------------------------------------------------------------------
    // One method per table. All four follow the same shape:
    //   1. query unexported
    //   2. append to Sheets
    //   3. only on success: exported = true + save
    // ------------------------------------------------------------------

    private void exportLoanApplications(ExportSummary summary) {
        List<LoanApplication> records = loanApplicationRepository.findByExportedFalse();
        if (records.isEmpty()) {
            log.info("No new records to export for '{}'.", TYPE_LOAN_APPLICATION);
            return;
        }
        List<List<Object>> rows = records.stream().map(this::loanApplicationToRow).toList();
        if (appendRows(exportTab, TYPE_LOAN_APPLICATION, rows, records.size(), summary)) {
            records.forEach(record -> record.setExported(true));
            loanApplicationRepository.saveAll(records);
            summary.recordExported(TYPE_LOAN_APPLICATION, records.size());
        }
    }

    private void exportEligibilityChecks(ExportSummary summary) {
        List<EligibilityCheck> records = eligibilityCheckRepository.findByExportedFalse();
        if (records.isEmpty()) {
            log.info("No new records to export for '{}'.", TYPE_ELIGIBILITY_CHECK);
            return;
        }
        List<List<Object>> rows = records.stream().map(this::eligibilityCheckToRow).toList();
        if (appendRows(exportTab, TYPE_ELIGIBILITY_CHECK, rows, records.size(), summary)) {
            records.forEach(record -> record.setExported(true));
            eligibilityCheckRepository.saveAll(records);
            summary.recordExported(TYPE_ELIGIBILITY_CHECK, records.size());
        }
    }

    private void exportContactMessages(ExportSummary summary) {
        List<ContactMessage> records = contactMessageRepository.findByExportedFalse();
        if (records.isEmpty()) {
            log.info("No new records to export for '{}'.", TYPE_CONTACT_MESSAGE);
            return;
        }
        List<List<Object>> rows = records.stream().map(this::contactMessageToRow).toList();
        if (appendRows(exportTab, TYPE_CONTACT_MESSAGE, rows, records.size(), summary)) {
            records.forEach(record -> record.setExported(true));
            contactMessageRepository.saveAll(records);
            summary.recordExported(TYPE_CONTACT_MESSAGE, records.size());
        }
    }

    private void exportCallbackRequests(ExportSummary summary) {
        List<CallbackRequest> records = callbackRequestRepository.findByExportedFalse();
        if (records.isEmpty()) {
            log.info("No new records to export for '{}'.", TYPE_CALLBACK_REQUEST);
            return;
        }
        List<List<Object>> rows = records.stream().map(this::callbackRequestToRow).toList();
        if (appendRows(exportTab, TYPE_CALLBACK_REQUEST, rows, records.size(), summary)) {
            records.forEach(record -> record.setExported(true));
            callbackRequestRepository.saveAll(records);
            summary.recordExported(TYPE_CALLBACK_REQUEST, records.size());
        }
    }

    /**
     * Single point of contact with Google Sheets. Returns {@code true} only after the
     * append call succeeded; on failure the error is logged, the record type is recorded
     * as failed in the summary, and the records stay {@code exported = false} for retry.
     *
     * @param tabName    actual spreadsheet tab the rows are appended to
     * @param summaryKey label used in logs and the summary (the {@code Type} column value)
     */
    private boolean appendRows(String tabName, String summaryKey, List<List<Object>> rows, int count, ExportSummary summary) {
        try {
            sheetsClient.appendRows(tabName, rows);
            return true;
        } catch (Exception ex) {
            log.error("Google Sheets append FAILED for '{}' ({} records). "
                    + "Records remain exported=false and will be retried on the next run.", summaryKey, count, ex);
            summary.recordFailed(summaryKey, count);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Row mapping. Column order must match the unified tab's header row, which is
    // the exact 14-column layout captured in the class javadoc:
    //   Timestamp | Type | Full Name | Mobile | Email | Loan Type | Loan Amount |
    //   Employment Type | Monthly Income | City | Subject | Message | Consent | Status
    // ------------------------------------------------------------------

    private List<Object> loanApplicationToRow(LoanApplication r) {
        return Arrays.asList(
                timestamp(r.getCreatedAt()),
                TYPE_LOAN_APPLICATION,
                r.getFullName(),
                r.getMobile(),
                r.getEmail(),
                r.getLoanType(),
                money(r.getLoanAmount()),
                r.getEmploymentType(),
                money(r.getMonthlyIncome()),
                r.getCity(),
                null,             // Subject
                r.getMessage(),
                consent(r.getConsent()),
                r.getStatus().name());
    }

    private List<Object> eligibilityCheckToRow(EligibilityCheck r) {
        return Arrays.asList(
                timestamp(r.getCreatedAt()),
                TYPE_ELIGIBILITY_CHECK,
                r.getFullName(),
                r.getMobile(),
                r.getEmail(),
                r.getLoanType(),
                money(r.getLoanAmount()),
                r.getEmploymentType(),
                money(r.getMonthlyIncome()),
                r.getCity(),
                null,             // Subject
                null,             // Message
                null,             // Consent
                r.getStatus().name());
    }

    private List<Object> contactMessageToRow(ContactMessage r) {
        return Arrays.asList(
                timestamp(r.getCreatedAt()),
                TYPE_CONTACT_MESSAGE,
                r.getFullName(),
                r.getMobile(),
                r.getEmail(),
                null,             // Loan Type
                null,             // Loan Amount
                null,             // Employment Type
                null,             // Monthly Income
                null,             // City
                r.getSubject(),
                r.getMessage(),
                null,             // Consent
                r.getStatus().name());
    }

    private List<Object> callbackRequestToRow(CallbackRequest r) {
        return Arrays.asList(
                timestamp(r.getCreatedAt()),
                TYPE_CALLBACK_REQUEST,
                r.getFullName(),
                r.getMobile(),
                null,             // Email
                r.getLoanType(),
                null,             // Loan Amount
                null,             // Employment Type
                null,             // Monthly Income
                null,             // City
                null,             // Subject
                null,             // Message
                null,             // Consent
                r.getStatus().name());
    }

    private static String money(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static String timestamp(LocalDateTime value) {
        return value == null ? null : value.format(EXPORT_TIMESTAMP_FORMAT);
    }

    private static String consent(Boolean value) {
        return value == null ? null : String.valueOf(value);
    }
}

