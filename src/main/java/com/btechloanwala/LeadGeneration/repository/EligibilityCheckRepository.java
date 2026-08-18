package com.btechloanwala.LeadGeneration.repository;

import com.btechloanwala.LeadGeneration.entity.EligibilityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data-access layer for {@link EligibilityCheck}. Database access only.
 */
public interface EligibilityCheckRepository extends JpaRepository<EligibilityCheck, Long> {

    /**
     * Records that have not yet been appended to Google Sheets. The daily export job
     * selects exactly these rows ({@code WHERE exported = false}).
     */
    List<EligibilityCheck> findByExportedFalse();
}