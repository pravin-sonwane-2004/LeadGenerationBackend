package com.btechloanwala.LeadGeneration.service;

import com.btechloanwala.LeadGeneration.dto.request.CallbackRequestDTO;
import com.btechloanwala.LeadGeneration.entity.CallbackRequest;
import com.btechloanwala.LeadGeneration.enums.LoanType;
import com.btechloanwala.LeadGeneration.repository.CallbackRequestRepository;
import org.springframework.stereotype.Service;

/**
 * Services for the callback request form. Validates the loan type against {@link
 * LoanType} and persists with default status NEW.
 */
@Service
public class CallbackService {

    private final CallbackRequestRepository repository;

    public CallbackService(CallbackRequestRepository repository) {
        this.repository = repository;
    }

    public void create(CallbackRequestDTO request) {
        LoanType loanType = LoanType.fromValue(request.getLoanType())
                .orElseThrow(() -> new IllegalArgumentException("Invalid loan type."));

        CallbackRequest callback = new CallbackRequest();
        callback.setFullName(request.getFullName());
        callback.setMobile(request.getMobile());
        callback.setLoanType(loanType.getValue());

        repository.save(callback);
    }
}