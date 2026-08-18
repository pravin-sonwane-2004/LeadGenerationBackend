package com.btechloanwala.LeadGeneration.repository;

import com.btechloanwala.LeadGeneration.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data-access layer for {@link LoanApplication}. Responsibility is limited to
 * database access — no business logic belongs here.
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    /**
     * Records that have not yet been appended to Google Sheets. The daily export job
     * selects exactly these rows ({@code WHERE exported = false}).
     */
    List<LoanApplication> findByExportedFalse();
}