package com.btechloanwala.LeadGeneration.scheduler;

import com.btechloanwala.LeadGeneration.service.export.LeadExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily trigger for the Google Sheets export.
 *
 * <p>Fires every day at exactly <b>12:00 PM Asia/Kolkata</b> and delegates to the same
 * {@link LeadExportService} used by the manual {@code POST /api/export} endpoint — the
 * export logic lives in exactly one place.</p>
 */
@Component
public class DailyExportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyExportScheduler.class);

    private final LeadExportService exportService;

    public DailyExportScheduler(LeadExportService exportService) {
        this.exportService = exportService;
    }

//    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Kolkata")
    @Scheduled(cron = "*/10 * * * * *", zone = "Asia/Kolkata")
    public void exportDaily() {

        log.info("Scheduled daily Google Sheets export started (12:00 PM Asia/Kolkata).");
        exportService.exportAll();
        log.info("Scheduled daily Google Sheets export finished.");
    }
}
