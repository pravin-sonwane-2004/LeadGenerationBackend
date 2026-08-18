package com.btechloanwala.LeadGeneration.service;

import com.btechloanwala.LeadGeneration.dto.request.LoanApplicationRequest;
import com.btechloanwala.LeadGeneration.entity.LoanApplication;
import com.btechloanwala.LeadGeneration.enums.EmploymentType;
import com.btechloanwala.LeadGeneration.enums.LoanType;
import com.btechloanwala.LeadGeneration.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Application services for the loan application form.
 *
 * <p>Responsible for business rules: mapping the DTO to the entity, converting
 * String money to {@link BigDecimal}, validating business values (loan type and
 * employment type) that Bean Validation cannot express, and saving the record.
 * The {@code status} defaults to NEW on the entity and {@code createdAt} is set by
 * the entity's {@code @PrePersist} hook.</p>
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public void create(LoanApplicationRequest request) {
        LoanType loanType = LoanType.fromValue(request.getLoanType())
                .orElseThrow(() -> new IllegalArgumentException("Invalid loan type."));

        EmploymentType employmentType = EmploymentType.fromValue(request.getEmployment())
                .orElseThrow(() -> new IllegalArgumentException("Invalid employment type."));

        LoanApplication application = new LoanApplication();
        application.setFullName(request.getFullName());
        application.setMobile(request.getMobile());
        application.setEmail(request.getEmail());
        application.setLoanType(loanType.getValue());
        application.setLoanAmount(toMoney(request.getAmount(), "Invalid loan amount."));
        application.setEmploymentType(employmentType.getValue());
        application.setMonthlyIncome(toMoney(request.getIncome(), "Invalid monthly income."));
        application.setCity(request.getCity());
        application.setMessage(request.getMessage());
        application.setConsent(request.getConsent());

        repository.save(application);
    }

    /**
     * Converts a String value such as {@code "500000"} to a non-negative {@link BigDecimal}.
     * Throws {@link IllegalArgumentException} (mapped to HTTP 422) for non-numeric or
     * negative input so bad values never reach the database.
     */
    private BigDecimal toMoney(String value, String errorMessage) {
        try {
            BigDecimal money = new BigDecimal(value);
            if (money.signum() < 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return money;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}