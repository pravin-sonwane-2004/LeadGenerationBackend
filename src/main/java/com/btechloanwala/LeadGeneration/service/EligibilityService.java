package com.btechloanwala.LeadGeneration.service;

import com.btechloanwala.LeadGeneration.dto.request.EligibilityRequest;
import com.btechloanwala.LeadGeneration.entity.EligibilityCheck;
import com.btechloanwala.LeadGeneration.enums.EmploymentType;
import com.btechloanwala.LeadGeneration.enums.LoanType;
import com.btechloanwala.LeadGeneration.repository.EligibilityCheckRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Services for the eligibility-check form. Mirrors {@link LoanApplicationService}:
 * maps DTO to entity, validates business values, converts String money to
 * {@link BigDecimal}, and saves with default status NEW.
 */
@Service
public class EligibilityService {

    private final EligibilityCheckRepository repository;

    public EligibilityService(EligibilityCheckRepository repository) {
        this.repository = repository;
    }

    public void create(EligibilityRequest request) {
        LoanType loanType = LoanType.fromValue(request.getLoanType())
                .orElseThrow(() -> new IllegalArgumentException("Invalid loan type."));

        EmploymentType employmentType = EmploymentType.fromValue(request.getEmployment())
                .orElseThrow(() -> new IllegalArgumentException("Invalid employment type."));

        EligibilityCheck check = new EligibilityCheck();
        check.setFullName(request.getFullName());
        check.setMobile(request.getMobile());
        check.setEmail(request.getEmail());
        check.setLoanType(loanType.getValue());
        check.setEmploymentType(employmentType.getValue());
        check.setMonthlyIncome(toMoney(request.getIncome(), "Invalid monthly income."));
        check.setLoanAmount(toMoney(request.getAmount(), "Invalid loan amount."));
        check.setCity(request.getCity());

        repository.save(check);
    }

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