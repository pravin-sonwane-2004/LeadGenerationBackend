package com.btechloanwala.LeadGeneration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's task scheduling so the {@link com.btechloanwala.LeadGeneration.scheduler.DailyExportScheduler}
 * runs its {@code @Scheduled} daily Google Sheets export.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
