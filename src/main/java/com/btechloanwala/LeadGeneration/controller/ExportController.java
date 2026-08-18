package com.btechloanwala.LeadGeneration.controller;

import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import com.btechloanwala.LeadGeneration.service.export.ExportSummary;
import com.btechloanwala.LeadGeneration.service.export.LeadExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only manual trigger for the daily Google Sheets export.
 *
 * <p>Executes the <em>exact same</em> {@link LeadExportService} used by the 12:00 PM
 * scheduler so you can test without waiting for the scheduled run. There is no
 * duplicated export logic.</p>
 */
@RestController
@RequestMapping("/api")
public class ExportController {

    private final LeadExportService exportService;

    public ExportController(LeadExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse> exportNow() {
        ExportSummary summary = exportService.exportAll();
        return ResponseEntity.ok(new ApiResponse(true, summary.toMessage()));
    }
}
