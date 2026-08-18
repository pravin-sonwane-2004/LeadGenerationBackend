package com.btechloanwala.LeadGeneration.service.export;

import com.btechloanwala.LeadGeneration.entity.CallbackRequest;
import com.btechloanwala.LeadGeneration.entity.ContactMessage;
import com.btechloanwala.LeadGeneration.entity.EligibilityCheck;
import com.btechloanwala.LeadGeneration.entity.LoanApplication;
import com.btechloanwala.LeadGeneration.repository.CallbackRequestRepository;
import com.btechloanwala.LeadGeneration.repository.ContactMessageRepository;
import com.btechloanwala.LeadGeneration.repository.EligibilityCheckRepository;
import com.btechloanwala.LeadGeneration.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LeadExportService}. Runs without Spring or a database: the
 * repositories and the {@link GoogleSheetsClient} are mocks, which keeps the critical
 * ordering guarantee (append first, only then exported = true) directly testable.
 */
@ExtendWith(MockitoExtension.class)
class LeadExportServiceTest {

    @Mock private LoanApplicationRepository loanRepo;
    @Mock private EligibilityCheckRepository eligibilityRepo;
    @Mock private ContactMessageRepository contactRepo;
    @Mock private CallbackRequestRepository callbackRepo;
    @Mock private GoogleSheetsClient sheetsClient;

    private LeadExportService service;

    @BeforeEach
    void setUp() {
        service = new LeadExportService(loanRepo, eligibilityRepo, contactRepo, callbackRepo, sheetsClient, "LoanApplications");
    }

    @Test
    void exportAll_appendsUnexportedLoanApplication_andMarksItExported() throws Exception {
        LoanApplication app = loanApplication();

        when(loanRepo.findByExportedFalse()).thenReturn(List.of(app));
        when(eligibilityRepo.findByExportedFalse()).thenReturn(List.of());
        when(contactRepo.findByExportedFalse()).thenReturn(List.of());
        when(callbackRepo.findByExportedFalse()).thenReturn(List.of());

        ExportSummary summary = service.exportAll();

        ArgumentCaptor<List<List<Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(sheetsClient).appendRows(eq("LoanApplications"), captor.capture());

        List<List<Object>> rows = captor.getValue();
        assertEquals(1, rows.size());
        List<Object> row = rows.get(0);
        assertEquals(14, row.size(), "every row must use the unified 14-column layout");
        assertEquals("Loan Application", row.get(1), "Type column must identify the source form");
        assertEquals("Amit Sharma", row.get(2));
        assertEquals("500000", row.get(6), "amount must be exported as a plain string, never a double");
        assertEquals("90000", row.get(8));
        assertEquals("true", row.get(12), "consent must be exported");
        assertEquals("NEW", row.get(13));

        assertTrue(app.isExported(), "record must be marked exported only after a successful append");
        verify(loanRepo).saveAll(List.of(app));

        verify(sheetsClient, never()).appendRows(eq("Eligibility Checks"), any());
        verify(sheetsClient, never()).appendRows(eq("Contact Messages"), any());
        verify(sheetsClient, never()).appendRows(eq("Callback Requests"), any());

        assertEquals(1, summary.getExported().get("Loan Application"));
        assertTrue(summary.getFailed().isEmpty());
    }

    @Test
    void exportAll_appendsEveryRecordTypeToTheSingleUnifiedTab() throws Exception {
        LoanApplication app = loanApplication();

        EligibilityCheck check = new EligibilityCheck();
        check.setFullName("Riya Verma");
        check.setMobile("8989898989");
        check.setLoanType("home-bt");
        check.setEmploymentType("self-employed");
        check.setMonthlyIncome(new BigDecimal("120000"));
        check.setLoanAmount(new BigDecimal("2500000"));
        check.setCity("Mumbai");

        ContactMessage msg = new ContactMessage();
        msg.setFullName("Priya Patel");
        msg.setMobile("9876543210");
        msg.setSubject("Query about home loan");
        msg.setMessage("Please share latest rates.");

        CallbackRequest callback = new CallbackRequest();
        callback.setFullName("Kabir Khan");
        callback.setMobile("9988776655");
        callback.setLoanType("business");

        when(loanRepo.findByExportedFalse()).thenReturn(List.of(app));
        when(eligibilityRepo.findByExportedFalse()).thenReturn(List.of(check));
        when(contactRepo.findByExportedFalse()).thenReturn(List.of(msg));
        when(callbackRepo.findByExportedFalse()).thenReturn(List.of(callback));

        service.exportAll();

        ArgumentCaptor<List<List<Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(sheetsClient, times(4)).appendRows(eq("LoanApplications"), captor.capture());

        List<List<Object>> rows = captor.getAllValues().stream().flatMap(List::stream).toList();
        assertEquals(4, rows.size());
        assertEquals(
                List.of("Loan Application", "Eligibility Check", "Contact Message", "Callback Request"),
                rows.stream().map(row -> row.get(1)).toList(),
                "each row must be tagged with its source form in the Type column");
        assertTrue(rows.stream().allMatch(row -> row.size() == 14),
                "every row must use the unified 14-column layout");

        assertTrue(app.isExported());
        assertTrue(check.isExported());
        assertTrue(msg.isExported());
        assertTrue(callback.isExported());

        verify(loanRepo).saveAll(List.of(app));
        verify(eligibilityRepo).saveAll(List.of(check));
        verify(contactRepo).saveAll(List.of(msg));
        verify(callbackRepo).saveAll(List.of(callback));
    }

    @Test
    void exportAll_sheetsFailure_leavesRecordsUnexportedForRetry() throws Exception {
        LoanApplication app = loanApplication();

        when(loanRepo.findByExportedFalse()).thenReturn(List.of(app));
        when(eligibilityRepo.findByExportedFalse()).thenReturn(List.of());
        when(contactRepo.findByExportedFalse()).thenReturn(List.of());
        when(callbackRepo.findByExportedFalse()).thenReturn(List.of());
        doThrow(new IOException("Google Sheets is down"))
                .when(sheetsClient).appendRows(eq("LoanApplications"), any());

        ExportSummary summary = service.exportAll();

        assertFalse(app.isExported(), "record must remain exported=false when Google Sheets fails");
        verify(loanRepo, never()).saveAll(any());

        assertEquals(1, summary.getFailed().get("Loan Application"));
        assertTrue(summary.getExported().isEmpty());
    }

    @Test
    void exportAll_noUnexportedRecords_doesNotTouchGoogleSheets() throws Exception {
        when(loanRepo.findByExportedFalse()).thenReturn(List.of());
        when(eligibilityRepo.findByExportedFalse()).thenReturn(List.of());
        when(contactRepo.findByExportedFalse()).thenReturn(List.of());
        when(callbackRepo.findByExportedFalse()).thenReturn(List.of());

        ExportSummary summary = service.exportAll();

        verify(sheetsClient, never()).appendRows(any(), any());
        assertTrue(summary.getExported().isEmpty());
        assertTrue(summary.getFailed().isEmpty());
    }

    private static LoanApplication loanApplication() {
        LoanApplication app = new LoanApplication();
        app.setFullName("Amit Sharma");
        app.setMobile("7276063476");
        app.setLoanType("personal");
        app.setLoanAmount(new BigDecimal("500000"));
        app.setEmploymentType("salaried");
        app.setMonthlyIncome(new BigDecimal("90000"));
        app.setCity("Pune");
        app.setConsent(true);
        return app;
    }
}
