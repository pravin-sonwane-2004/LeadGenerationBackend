package com.btechloanwala.LeadGeneration.repository;

import com.btechloanwala.LeadGeneration.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data-access layer for {@link ContactMessage}. Database access only.
 */
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    /**
     * Records that have not yet been appended to Google Sheets. The daily export job
     * selects exactly these rows ({@code WHERE exported = false}).
     */
    List<ContactMessage> findByExportedFalse();
}