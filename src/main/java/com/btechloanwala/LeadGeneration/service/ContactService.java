package com.btechloanwala.LeadGeneration.service;

import com.btechloanwala.LeadGeneration.dto.request.ContactRequest;
import com.btechloanwala.LeadGeneration.entity.ContactMessage;
import com.btechloanwala.LeadGeneration.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;

/**
 * Services for the contact form. Maps the DTO to the entity and saves; status
 * defaults to NEW and createdAt is stamped by the entity's {@code @PrePersist}.
 */
@Service
public class ContactService {

    private final ContactMessageRepository repository;

    public ContactService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    public void create(ContactRequest request) {
        ContactMessage message = new ContactMessage();
        message.setFullName(request.getFullName());
        message.setMobile(request.getMobile());
        message.setEmail(request.getEmail());
        message.setSubject(request.getSubject());
        message.setMessage(request.getMessage());

        repository.save(message);
    }
}