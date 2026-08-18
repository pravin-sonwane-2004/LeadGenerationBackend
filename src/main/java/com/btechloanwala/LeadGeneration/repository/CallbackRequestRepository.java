package com.btechloanwala.LeadGeneration.repository;

import com.btechloanwala.LeadGeneration.entity.CallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data-access layer for {@link CallbackRequest}. Database access only.
 */
public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {

    /**
     * Records that have not yet been appended to Google Sheets. The daily export job
     * selects exactly these rows ({@code WHERE exported = false}).
     */
    List<CallbackRequest> findByExportedFalse();
}