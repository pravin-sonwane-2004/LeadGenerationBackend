package com.btechloanwala.LeadGeneration.service.export;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of one {@code exportAll()} run: how many records were appended per lead type
 * and how many appends failed (and are therefore left {@code exported=false} for retry).
 */
public class ExportSummary {

    private final Map<String, Integer> exported = new LinkedHashMap<>();
    private final Map<String, Integer> failed = new LinkedHashMap<>();

    void recordExported(String tabName, int count) {
        exported.put(tabName, count);
    }

    void recordFailed(String tabName, int count) {
        failed.put(tabName, count);
    }

    public Map<String, Integer> getExported() {
        return Map.copyOf(exported);
    }

    public Map<String, Integer> getFailed() {
        return Map.copyOf(failed);
    }

    public boolean hasFailures() {
        return !failed.isEmpty();
    }

    /** Human-readable one-liner used in logs and the manual {@code POST /api/export} response. */
    public String toMessage() {
        StringBuilder sb = new StringBuilder("Google Sheets export complete.");
        if (!exported.isEmpty()) {
            sb.append(" Exported: ");
            exported.forEach((tab, count) -> sb.append(tab).append('=').append(count).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append('.');
        } else {
            sb.append(" No new records to export.");
        }
        if (!failed.isEmpty()) {
            sb.append(" Failed (will retry): ");
            failed.forEach((tab, count) -> sb.append(tab).append('=').append(count).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append('.');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toMessage();
    }
}
